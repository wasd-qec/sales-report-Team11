package edu.itc.salesreport.ui.console;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;

public final class ConsoleReportView {
    private final SalesModel model;
    private final PrintStream out;

    public ConsoleReportView(SalesModel model, PrintStream out) {
        this.model = model;
        this.out = out;
        model.addListener(this::modelChanged);
    }

    private void modelChanged(SalesEvent event) {
        out.println(switch (event) {
            case SalesEvent.MonthLoaded(var month, var rows, var rejected) ->
                    "Loaded %s: %d rows, %d rejected".formatted(month, rows, rejected);
            case SalesEvent.BranchSelected(var branch) -> "Branch " + branch;
        });
        var summary = model.summary();
        out.printf("  %-4s revenue %9s  receipts %4d  basket %5s%n",
                summary.branch(), summary.revenue(), summary.receipts(), summary.averageBasket());
        summary.revenueByCategory().entrySet().stream()
                .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                .forEach(entry -> out.printf("    %-14s %9s%n",
                        entry.getKey(), entry.getValue()));
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        new ConsoleReportView(model, System.out);
        model.load(dataDir, YearMonth.parse("2026-09"));
        model.selectBranch("PNH");
    }
}
