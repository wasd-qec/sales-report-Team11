package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/** ViewModel for the sales report viewer. Uses only javafx.base. */
public final class ReportViewModel {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private final SummarySource source;
    private final BackgroundJobs jobs;
    private final Clock clock;

    // ---- input state: two-way bound by the view
    private final ObjectProperty<YearMonth> month =
            new SimpleObjectProperty<>(this, "month");
    private final Map<String, BooleanProperty> branchSelected =
            new LinkedHashMap<>();
    private final StringProperty productFilter =
            new SimpleStringProperty(this, "productFilter", "");

    // ---- output state: one-way bound by the view
    private final ObservableList<YearMonth> monthChoices =
            FXCollections.observableArrayList();
    private final ObservableList<BranchSummary> summaries =
            FXCollections.observableArrayList();
    private final ObservableList<ProductTotal> products =
            FXCollections.observableArrayList();
    private final FilteredList<ProductTotal> visibleProducts =
            new FilteredList<>(products);
    private final ReadOnlyBooleanWrapper loading =
            new ReadOnlyBooleanWrapper(this, "loading", false);
    private final ReadOnlyDoubleWrapper progress =
            new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper(this, "status", "Choose a month.");
    private final ReadOnlyStringWrapper loadError =
            new ReadOnlyStringWrapper(this, "loadError", "");

    // ---- computed state
    private final StringBinding monthError;
    private final StringBinding branchError;
    private final StringBinding validationMessage;
    private final BooleanBinding valid;
    private final BooleanBinding canGenerate;
    private final ObjectBinding<BigDecimal> totalRevenue;
    private final StringBinding totalRevenueText;

    private final Command generateCommand;
    private final Command cancelCommand;
    private BackgroundJobs.JobHandle running;

    private record LoadResult(List<BranchSummary> summaries, List<ProductTotal> products) { }

    public ReportViewModel(SummarySource source, BackgroundJobs jobs,
                           Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.jobs = Objects.requireNonNull(jobs);
        this.clock = Objects.requireNonNull(clock);
        YearMonth now = YearMonth.now(clock);
        for (int i = 0; i < 12; i++) {
            monthChoices.add(now.minusMonths(i));
        }
        for (String b : BRANCHES) {
            branchSelected.put(b, new SimpleBooleanProperty(this, b, true));
        }

        monthError = Bindings.createStringBinding(
                () -> checkMonth(month.get()), month);

        Observable[] flags = branchSelected.values().toArray(Observable[]::new);
        branchError = Bindings.createStringBinding(
                () -> selectedBranches().isEmpty() ? "Select at least one branch." : "",
                flags);

        validationMessage = Bindings.createStringBinding(
                () -> Stream.of(monthError.get(), branchError.get())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(" ")),
                monthError, branchError);

        valid = monthError.isEmpty().and(branchError.isEmpty());
        canGenerate = valid.and(loading.not());

        totalRevenue = Bindings.createObjectBinding(
                () -> summaries.stream()
                        .map(BranchSummary::revenue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                summaries);

        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        totalRevenueText = Bindings.createStringBinding(
                () -> "USD " + currencyFormat.format(totalRevenue.get()),
                totalRevenue);

        productFilter.addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);
            if (filter.isEmpty()) {
                visibleProducts.setPredicate(p -> true);
            } else {
                visibleProducts.setPredicate(p ->
                        p.productName().toLowerCase(Locale.ROOT).contains(filter)
                        || p.sku().toLowerCase(Locale.ROOT).contains(filter));
            }
        });

        generateCommand = new DelegateCommand(this::generate, canGenerate);
        cancelCommand = new DelegateCommand(this::cancel,
                loading.getReadOnlyProperty());
    }

    private String checkMonth(YearMonth m) {
        if (m == null) {
            return "Choose a month.";
        }
        YearMonth today = YearMonth.now(clock);
        if (m.isAfter(today)) {
            return "Month " + m + " is in the future.";
        }
        return "";
    }

    public List<String> selectedBranches() {
        return branchSelected.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void generate() {
        YearMonth m = month.get();
        List<String> branches = List.copyOf(selectedBranches());
        if (m == null || branches.isEmpty()) {
            return;
        }

        loading.set(true);
        progress.set(0);
        loadError.set("");
        status.set("Loading " + branches.size() + " branch(es)...");

        running = jobs.submit(sink -> loadAll(m, branches, sink),
                new BackgroundJobs.JobListener<LoadResult>() {
                    @Override
                    public void progress(double fraction) {
                        progress.set(fraction);
                    }

                    @Override
                    public void succeeded(LoadResult result) {
                        summaries.setAll(result.summaries());
                        products.setAll(result.products());
                        status.set(String.format(Locale.ROOT,
                                "Loaded %d branch(es) for %s.",
                                result.summaries().size(), m));
                        loadError.set("");
                        progress.set(1.0);
                        loading.set(false);
                        running = null;
                    }

                    @Override
                    public void failed(Throwable error) {
                        loadError.set(error.getMessage() != null
                                ? error.getMessage() : error.toString());
                        status.set("Load failed, previous data kept.");
                        loading.set(false);
                        running = null;
                    }

                    @Override
                    public void cancelled() {
                        status.set("Cancelled, previous data kept.");
                        loading.set(false);
                        running = null;
                    }
                });
    }

    private void cancel() {
        if (running != null) {
            running.cancel();
            running = null;
        }
    }

    private LoadResult loadAll(YearMonth m, List<String> branches,
                               BackgroundJobs.ProgressSink sink) throws Exception {
        if (sink.isCancelled()) {
            throw new CancellationException("Cancelled");
        }
        List<BranchSummary> branchList = new ArrayList<>();
        Map<String, ProductTotal> mergedProducts = new LinkedHashMap<>();

        for (int i = 0; i < branches.size(); i++) {
            if (sink.isCancelled()) {
                throw new CancellationException("Cancelled");
            }
            String b = branches.get(i);
            BranchSummary summary = source.load(m, b);
            branchList.add(summary);

            for (ProductTotal pt : summary.topProducts()) {
                mergedProducts.merge(pt.sku(), pt,
                        (p1, p2) -> new ProductTotal(
                                p1.sku(), p1.productName(),
                                p1.revenue().add(p2.revenue()),
                                p1.quantity() + p2.quantity()));
            }
            sink.update(i + 1, branches.size());
        }

        List<ProductTotal> sortedProducts = mergedProducts.values().stream()
                .sorted(Comparator.comparing(ProductTotal::revenue).reversed())
                .toList();

        return new LoadResult(branchList, sortedProducts);
    }

    // ---- accessors: read-only types for everything the view must not set
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public BooleanProperty branchSelectedProperty(String branch) {
        return Objects.requireNonNull(branchSelected.get(branch), branch);
    }
    public StringProperty productFilterProperty() { return productFilter; }
    public ObservableList<YearMonth> monthChoices() {
        return FXCollections.unmodifiableObservableList(monthChoices);
    }
    public ObservableList<BranchSummary> summaries() {
        return FXCollections.unmodifiableObservableList(summaries);
    }
    public ObservableList<ProductTotal> visibleProducts() {
        return visibleProducts;
    }
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty loadErrorProperty() {
        return loadError.getReadOnlyProperty();
    }
    public StringExpression monthErrorProperty() { return monthError; }
    public StringExpression branchErrorProperty() { return branchError; }
    public StringExpression validationMessageProperty() {
        return validationMessage;
    }
    public ObservableBooleanValue canGenerateProperty() { return canGenerate; }
    public ObservableObjectValue<BigDecimal> totalRevenueProperty() {
        return totalRevenue;
    }
    public StringExpression totalRevenueTextProperty() {
        return totalRevenueText;
    }
    public Command generateCommand() { return generateCommand; }
    public Command cancelCommand() { return cancelCommand; }
}
