package edu.itc.salesreport.ui.mvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportPresenterTest {
    private final RecordingScreen screen = new RecordingScreen();
    private ReportPresenter presenter;

    @BeforeEach
    void setUp() throws Exception {
        Path data = Files.createTempDirectory("mvp");
        TestData.writeSample(data);
        var model = new edu.itc.salesreport.ui.SalesModel(
                new CsvMonthReader(new CsvTransactionParser()), new DefaultSalesAggregator());
        presenter = new ReportPresenter(model, screen, data,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void formatsStatusAndRowsWithoutJavaFx() {
        presenter.monthEntered("2026-09");
        presenter.branchChosen("PNH");
        assertEquals("PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00",
                screen.status);
        assertEquals(new RowItem("2026-09-01", "PNH-000001", "Power Bank 10000mAh",
                "Electronics", "1", "18.00"), screen.rows.get(1));
        assertEquals("1,234.50", ReportPresenter.money(new BigDecimal("1234.5")));
    }

    private static final class RecordingScreen implements ReportScreen {
        private List<RowItem> rows = List.of();
        private String status;
        @Override public void showRows(List<RowItem> rows) { this.rows = rows; }
        @Override public void showCategoryRevenue(Map<String, BigDecimal> revenue) {}
        @Override public void showStatus(String text) { status = text; }
        @Override public void showError(String message) {}
    }
}
