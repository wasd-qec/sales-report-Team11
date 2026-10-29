package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import java.nio.charset.StandardCharsets;

public final class CsvReportRenderer implements ReportRenderer {
    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        var csv = new StringBuilder(
                "month,branch,revenue,discounts,receipts,average_basket\n");
        report.branches().forEach(branch -> line(csv, report, branch));
        line(csv, report, report.chain());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder csv, MonthlyReport report,
            BranchSummary branch) {
        csv.append(report.month()).append(',').append(branch.branch()).append(',')
                .append(branch.revenue()).append(',').append(branch.discounts())
                .append(',').append(branch.receipts()).append(',')
                .append(branch.averageBasket()).append('\n');
    }
}
