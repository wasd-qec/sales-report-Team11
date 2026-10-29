package edu.itc.salesreport.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ReportRendererTest {
    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void rendersEveryBranchAndChain(String extension) {
        var output = new String(ReportRenderer.forExtension(extension)
                .render(TestReports.september()), StandardCharsets.UTF_8);
        assertTrue(output.contains("PNH"));
        assertTrue(output.contains("REP"));
        assertTrue(output.contains("BTB"));
        assertTrue(output.contains("2300.50"));
    }

    @Test
    void csvHasStableHeaderAndRows() {
        var output = new String(new CsvReportRenderer().render(TestReports.september()),
                StandardCharsets.UTF_8);
        assertTrue(output.contains("month,branch,revenue,discounts,receipts,average_basket"));
        assertTrue(output.contains("2026-09,REP,800.50,0.00,50,16.01"));
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportRenderer.forExtension("pdf"));
    }

    @Test
    void rendererSwitchIsExhaustive() {
        for (var renderer : Stream.of(new TextReportRenderer(),
                new HtmlReportRenderer(), new CsvReportRenderer()).toList()) {
            var name = switch (renderer) {
                case TextReportRenderer ignored -> "txt";
                case HtmlReportRenderer ignored -> "html";
                case CsvReportRenderer ignored -> "csv";
            };
            assertEquals(renderer.extension(), name);
        }
    }

    @Test
    void htmlEscapesMarkup() {
        var report = new edu.itc.salesreport.model.MonthlyReport(
                java.time.YearMonth.of(2026, 9),
                java.util.List.of(TestReports.branch("A<&", "1.00", 1)),
                TestReports.branch("ALL", "1.00", 1), java.util.List.of());
        var output = new String(new HtmlReportRenderer().render(report),
                StandardCharsets.UTF_8);
        assertTrue(output.contains("A&lt;&amp;"));
    }
}
