package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ingestion orchestrator responsible for scanning monthly CSV files,
 * parsing records, skipping faulty rows, broadcasting events to listeners,
 * and aggregating metrics into a MonthlyReport.
 */
public class MonthLoader {

    private final TransactionParser parser;
    private final List<LoadListener> listeners = new CopyOnWriteArrayList<>();

    public MonthLoader() {
        this(new CsvTransactionParser());
    }

    public MonthLoader(TransactionParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    public void addListener(LoadListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(LoadListener listener) {
        listeners.remove(listener);
    }

    protected void fireEvent(LoadEvent event) {
        for (LoadListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    /**
     * Loads transactions from the directory 'data/<month>' and produces an aggregated MonthlyReport.
     */
    public MonthlyReport loadMonth(YearMonth month) throws IOException {
        return loadMonth(month, Path.of("data", month.toString()));
    }

    /**
     * Loads transactions from a specified directory and produces an aggregated MonthlyReport.
     */
    public MonthlyReport loadMonth(YearMonth month, Path dir) throws IOException {
        if (!Files.exists(dir)) {
            throw new IOException("Directory does not exist: " + dir);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }

        List<SaleTransaction> transactions = new ArrayList<>();
        int totalInvalidRows = 0;

        for (Path file : files) {
            fireEvent(LoadEvent.fileStarted(file));
            int fileValid = 0;
            int fileInvalid = 0;

            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber == 1 && line.toLowerCase().startsWith("branch,")) {
                        // Skip header row
                        continue;
                    }
                    if (line.isBlank()) {
                        continue;
                    }

                    try {
                        SaleTransaction tx = parser.parse(file, lineNumber, line);
                        transactions.add(tx);
                        fileValid++;
                        fireEvent(LoadEvent.rowParsed(file, lineNumber, tx));
                    } catch (InvalidRowException ex) {
                        fileInvalid++;
                        totalInvalidRows++;
                        fireEvent(LoadEvent.rowInvalid(file, lineNumber, ex));
                    }
                }
            }

            fireEvent(LoadEvent.fileCompleted(file, fileValid, fileInvalid));
        }

        fireEvent(LoadEvent.loadCompleted(files.size(), transactions.size(), totalInvalidRows));

        return buildReport(month, files.size(), totalInvalidRows, transactions);
    }

    /**
     * Aggregates raw transactions into an immutable MonthlyReport.
     */
    public MonthlyReport buildReport(
            YearMonth month,
            int fileCount,
            int invalidRowCount,
            List<SaleTransaction> transactions
    ) {
        int totalUnitsSold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<PaymentMethod, BigDecimal> totalRevenueByPayment = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod pm : PaymentMethod.values()) {
            totalRevenueByPayment.put(pm, BigDecimal.ZERO);
        }

        Map<String, List<SaleTransaction>> byBranch = new TreeMap<>();
        Map<String, List<SaleTransaction>> bySku = new LinkedHashMap<>();

        for (SaleTransaction tx : transactions) {
            totalUnitsSold += tx.quantity();
            BigDecimal lineTotal = tx.getLineTotal();
            totalRevenue = totalRevenue.add(lineTotal);

            totalRevenueByPayment.put(
                    tx.paymentMethod(),
                    totalRevenueByPayment.get(tx.paymentMethod()).add(lineTotal)
            );

            byBranch.computeIfAbsent(tx.branch(), k -> new ArrayList<>()).add(tx);
            bySku.computeIfAbsent(tx.sku(), k -> new ArrayList<>()).add(tx);
        }

        // Build branch summaries
        List<BranchSummary> branchSummaries = new ArrayList<>();
        for (Map.Entry<String, List<SaleTransaction>> entry : byBranch.entrySet()) {
            String branchName = entry.getKey();
            List<SaleTransaction> branchTxs = entry.getValue();

            int bCount = branchTxs.size();
            int bUnits = 0;
            BigDecimal bRev = BigDecimal.ZERO;
            Map<PaymentMethod, BigDecimal> bPayment = new EnumMap<>(PaymentMethod.class);
            for (PaymentMethod pm : PaymentMethod.values()) {
                bPayment.put(pm, BigDecimal.ZERO);
            }

            for (SaleTransaction tx : branchTxs) {
                bUnits += tx.quantity();
                BigDecimal lt = tx.getLineTotal();
                bRev = bRev.add(lt);
                bPayment.put(tx.paymentMethod(), bPayment.get(tx.paymentMethod()).add(lt));
            }

            branchSummaries.add(new BranchSummary(branchName, bCount, bUnits, bRev, bPayment));
        }

        // Build product totals sorted by totalRevenue descending
        List<ProductTotal> productTotals = new ArrayList<>();
        for (Map.Entry<String, List<SaleTransaction>> entry : bySku.entrySet()) {
            List<SaleTransaction> pTxs = entry.getValue();
            SaleTransaction first = pTxs.get(0);

            int pUnits = 0;
            BigDecimal pRev = BigDecimal.ZERO;
            for (SaleTransaction tx : pTxs) {
                pUnits += tx.quantity();
                pRev = pRev.add(tx.getLineTotal());
            }

            productTotals.add(new ProductTotal(
                    first.sku(),
                    first.productName(),
                    first.category(),
                    pUnits,
                    pRev
            ));
        }

        productTotals.sort(Comparator.comparing(ProductTotal::totalRevenue).reversed());

        return new MonthlyReport(
                month,
                fileCount,
                transactions.size(),
                invalidRowCount,
                totalUnitsSold,
                totalRevenue,
                branchSummaries,
                productTotals,
                totalRevenueByPayment
        );
    }
}
