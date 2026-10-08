package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Strategy interface for rendering a MonthlyReport into various output representations.
 */
public interface ReportRenderer {

    /**
     * Renders the given MonthlyReport to the provided character stream Writer.
     *
     * @param report the aggregated report to render
     * @param writer the destination writer
     * @throws IOException if writing encounters an I/O error
     */
    void render(MonthlyReport report, Writer writer) throws IOException;

    /**
     * Helper method to render the report directly into a String.
     *
     * @param report the aggregated report to render
     * @return formatted report string
     */
    default String renderToString(MonthlyReport report) {
        StringWriter sw = new StringWriter();
        try {
            render(report, sw);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected in-memory rendering failure", e);
        }
        return sw.toString();
    }
}
