package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.MonthData;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.SaleTransaction;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SalesModel {
    public static final String ALL = "ALL";
    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final List<SalesModelListener> listeners = new CopyOnWriteArrayList<>();
    private MonthData data;
    private String branch = ALL;

    public SalesModel(CsvMonthReader reader, SalesAggregator aggregator) {
        this.reader = reader;
        this.aggregator = aggregator;
    }

    public void addListener(SalesModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SalesModelListener listener) {
        listeners.remove(listener);
    }

    public void load(Path dataDir, YearMonth month) throws IOException {
        MonthData loaded = reader.read(dataDir, month);
        data = loaded;
        fire(new SalesEvent.MonthLoaded(month, loaded.rows().size(), loaded.rejected().size()));
    }

    public void selectBranch(String newBranch) {
        if (branch.equals(newBranch)) {
            return;
        }
        branch = newBranch;
        fire(new SalesEvent.BranchSelected(branch));
    }

    public String branch() {
        return branch;
    }

    public Optional<YearMonth> month() {
        return Optional.ofNullable(data).map(MonthData::month);
    }

    public List<SaleTransaction> visibleTransactions() {
        if (data == null) {
            return List.of();
        }
        if (ALL.equals(branch)) {
            return data.rows();
        }
        return data.rows().stream().filter(row -> row.branch().equals(branch)).toList();
    }

    public BranchSummary summary() {
        return aggregator.summarize(branch, visibleTransactions());
    }

    public List<String> rejectedRows() {
        return data == null ? List.of() : data.rejected();
    }

    private void fire(SalesEvent event) {
        listeners.forEach(listener -> listener.modelChanged(event));
    }
}
