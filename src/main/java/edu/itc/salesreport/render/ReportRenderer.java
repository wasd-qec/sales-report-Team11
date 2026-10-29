package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;

/** Strategy: one way of turning a report into a file. */
public sealed interface ReportRenderer
        permits TextReportRenderer, HtmlReportRenderer, CsvReportRenderer {

    String extension();

    byte[] render(MonthlyReport report);

    static ReportRenderer forExtension(String ext) {
        return switch (ext) {
            case "txt" -> new TextReportRenderer();
            case "html" -> new HtmlReportRenderer();
            case "csv" -> new CsvReportRenderer();
            default -> throw new IllegalArgumentException("no renderer for ." + ext);
        };
    }
}
