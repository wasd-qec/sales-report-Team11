package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain JUnit 6 tests: no JavaFX toolkit, no window, no robot, no sleep. */
class ReportViewModelTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);
    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final YearMonth OCT = YearMonth.of(2026, 10);

    ManualJobs jobs = new ManualJobs();
    List<String> loaded = new ArrayList<>();
    boolean failNext;
    ReportViewModel vm;

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> {
            if (failNext) {
                throw new IOException(branch + "-2026-08-03.csv: unreadable");
            }
            loaded.add(branch);
            return summary(branch, "200.00");
        };
        vm = new ReportViewModel(source, jobs, SEP_27);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    @Test
    void futureMonthIsRejected() {
        vm.monthProperty().set(OCT);
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void cannotGenerateBeforeAMonthIsChosen() {
        vm.monthProperty().set(null);
        assertEquals("Choose a month.", vm.monthErrorProperty().get());
        assertEquals("Choose a month.", vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void emptyBranchSelectionIsRejected() {
        vm.monthProperty().set(AUG);
        for (String b : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(b).set(false);
        }
        assertEquals("Select at least one branch.", vm.branchErrorProperty().get());
        assertEquals("Select at least one branch.", vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void multipleValidationErrorsAreJoined() {
        vm.monthProperty().set(null);
        for (String b : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(b).set(false);
        }
        assertEquals("Choose a month. Select at least one branch.",
                vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void generateStartsLoadingAndDisablesItself() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        assertTrue(vm.cancelCommand().isExecutable());
        assertEquals(1, jobs.pendingCount());
    }

    @Test
    void generateIsIgnoredWhileInvalid() {
        vm.monthProperty().set(null);
        vm.generateCommand().execute();
        assertFalse(vm.loadingProperty().get());
        assertEquals(0, jobs.pendingCount());
    }

    @Test
    void successfulLoadFillsSummariesAndTotal() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();

        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
        assertEquals(3, vm.summaries().size());
        assertEquals(new BigDecimal("600.00"), vm.totalRevenueProperty().get());
        assertEquals("USD 600.00", vm.totalRevenueTextProperty().get());
        assertEquals("Loaded 3 branch(es) for 2026-08.", vm.statusProperty().get());
        assertEquals("", vm.loadErrorProperty().get());
    }

    @Test
    void progressFollowsTheBranches() {
        List<Double> observed = new ArrayList<>();
        vm.progressProperty().addListener((obs, oldV, newV) -> observed.add(newV.doubleValue()));
        vm.monthProperty().set(AUG);
        vm.branchSelectedProperty("BTB").set(false); // 2 branches: PNH, REP

        vm.generateCommand().execute();
        jobs.runNext();

        assertTrue(observed.contains(0.5));
        assertTrue(observed.contains(1.0));
    }

    @Test
    void failureKeepsPreviousDataAndShowsTheError() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
        assertEquals(3, vm.summaries().size());

        failNext = true;
        vm.generateCommand().execute();
        jobs.runNext();

        assertEquals(3, vm.summaries().size()); // previous data kept
        assertEquals("PNH-2026-08-03.csv: unreadable", vm.loadErrorProperty().get());
        assertEquals("Load failed, previous data kept.", vm.statusProperty().get());
        assertFalse(vm.loadingProperty().get());
    }

    @Test
    void cancelReenablesGenerateAndKeepsOldData() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
        assertEquals(3, vm.summaries().size());

        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        vm.cancelCommand().execute();

        assertFalse(vm.loadingProperty().get());
        assertEquals("Cancelled, previous data kept.", vm.statusProperty().get());
        assertEquals(3, vm.summaries().size());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void productFilterIsCaseInsensitive() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();

        assertEquals(1, vm.visibleProducts().size());

        vm.productFilterProperty().set("rice");
        assertEquals(1, vm.visibleProducts().size());

        vm.productFilterProperty().set("SKU-1001");
        assertEquals(1, vm.visibleProducts().size());

        vm.productFilterProperty().set("nonexistent");
        assertEquals(0, vm.visibleProducts().size());

        vm.productFilterProperty().set("");
        assertEquals(1, vm.visibleProducts().size());
    }

    @Test
    void workerUsesASnapshotOfTheInputs() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();

        // Mutate properties while job is pending
        vm.monthProperty().set(SEP);
        vm.branchSelectedProperty("PNH").set(false);

        jobs.runNext();
        assertEquals(List.of("PNH", "REP", "BTB"), loaded);
        assertEquals("Loaded 3 branch(es) for 2026-08.", vm.statusProperty().get());
    }

    @Test
    void twoViewsShareViewModel() {
        List<String> view1Totals = new ArrayList<>();
        List<String> view2Totals = new ArrayList<>();

        vm.totalRevenueTextProperty().addListener((obs, oldVal, newVal) -> view1Totals.add(newVal));
        vm.totalRevenueTextProperty().addListener((obs, oldVal, newVal) -> view2Totals.add(newVal));

        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();

        assertEquals(List.of("USD 600.00"), view1Totals);
        assertEquals(List.of("USD 600.00"), view2Totals);
    }
}

