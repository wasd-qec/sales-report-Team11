package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import java.nio.charset.StandardCharsets;

public final class HtmlReportRenderer implements ReportRenderer {
    @Override
    public String extension() {
        return "html";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        var html = new StringBuilder("""
                <!doctype html>
                <html><body><table>
                <thead><tr><th>Branch</th><th>Revenue</th><th>Receipts</th><th>Average basket</th></tr></thead>
                <tbody>
                """);
        report.branches().forEach(branch -> row(html, branch));
        row(html, report.chain());
        html.append("</tbody></table></body></html>\n");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder html, BranchSummary branch) {
        html.append("<tr><td>").append(escape(branch.branch()))
                .append("</td><td>").append(escape(branch.revenue().toString()))
                .append("</td><td>").append(branch.receipts())
                .append("</td><td>").append(escape(branch.averageBasket().toString()))
                .append("</td></tr>\n");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
