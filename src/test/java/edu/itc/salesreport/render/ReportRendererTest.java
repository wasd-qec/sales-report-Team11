package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    private MonthlyReport sampleReport;

    @BeforeEach
    void setUp() {
        sampleReport = TestReports.createSampleReport();
    }

    @Test
    @DisplayName("TextReportRenderer should produce non-empty ASCII table containing key metrics")
    void testTextReportRenderer() {
        ReportRenderer renderer = new TextReportRenderer();
        String output = renderer.renderToString(sampleReport);

        assertNotNull(output);
        assertTrue(output.contains("MONTHLY SALES REPORT - 2026-09"));
        assertTrue(output.contains("PNH"));
        assertTrue(output.contains("REP"));
        assertTrue(output.contains("SKU-1001"));
        assertTrue(output.contains("Jasmine Rice 5kg"));
        assertTrue(output.contains("KHQR"));
        assertTrue(output.contains("$2,200.00"));
    }

    @Test
    @DisplayName("HtmlReportRenderer should generate full HTML document with tables and styles")
    void testHtmlReportRenderer() {
        ReportRenderer renderer = new HtmlReportRenderer();
        String html = renderer.renderToString(sampleReport);

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<title>Sales Report - 2026-09</title>"));
        assertTrue(html.contains("PNH"));
        assertTrue(html.contains("REP"));
        assertTrue(html.contains("SKU-1001"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    @DisplayName("CsvReportRenderer should generate valid CSV structure with headers and sections")
    void testCsvReportRenderer() {
        ReportRenderer renderer = new CsvReportRenderer();
        String csv = renderer.renderToString(sampleReport);

        assertNotNull(csv);
        assertTrue(csv.contains("section,identifier,attribute_1,attribute_2,numeric_1,numeric_2"));
        assertTrue(csv.contains("OVERVIEW,2026-09"));
        assertTrue(csv.contains("BRANCH,PNH"));
        assertTrue(csv.contains("BRANCH,REP"));
        assertTrue(csv.contains("PRODUCT,SKU-1001"));
    }
}
