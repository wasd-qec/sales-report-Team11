package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportControllerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));

    @Test
    void validMonthLoadsAndClearsError() throws Exception {
        Path dir = Files.createTempDirectory("report-controller");
        TestData.writeSample(dir);
        var model = model();
        var errors = new FakeErrors();
        new ReportController(model, dir, CLOCK, errors).monthEntered("2026-09");

        assertEquals(3, model.visibleTransactions().size());
        assertEquals(List.of(), errors.messages);
        assertEquals(1, errors.clearCount);
    }

    @Test
    void invalidMonthNeverReachesModel() throws Exception {
        var model = model();
        var errors = new FakeErrors();
        new ReportController(model, Files.createTempDirectory("report-controller"),
                CLOCK, errors).monthEntered("09/2026");

        assertTrue(model.month().isEmpty());
        assertEquals(List.of("'09/2026' is not a month in the form yyyy-MM"),
                errors.messages);
        assertEquals(0, errors.clearCount);
    }

    @Test
    void missingMonthMapsToUserFacingMessage() throws Exception {
        Path dir = Files.createTempDirectory("report-controller");
        var errors = new FakeErrors();
        new ReportController(model(), dir, CLOCK, errors).monthEntered("2026-07");

        assertEquals(List.of("No sales data found in " + dir.resolve("2026-07")),
                errors.messages);
    }

    @Test
    void branchIsNormalisedAndApplied() throws Exception {
        Path dir = Files.createTempDirectory("report-controller");
        TestData.writeSample(dir);
        var model = model();
        var errors = new FakeErrors();
        var controller = new ReportController(model, dir, CLOCK, errors);
        controller.monthEntered("2026-09");
        controller.branchChosen("rep");

        assertEquals("REP", model.branch());
        assertEquals(1, model.visibleTransactions().size());
        assertEquals(List.of(), errors.messages);
        assertEquals(2, errors.clearCount);
    }

    private static SalesModel model() {
        return new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
    }

    private static final class FakeErrors implements ErrorDisplay {
        private final List<String> messages = new ArrayList<>();
        private int clearCount;

        @Override
        public void showError(String message) {
            messages.add(message);
        }

        @Override
        public void clearError() {
            clearCount++;
        }
    }
}
