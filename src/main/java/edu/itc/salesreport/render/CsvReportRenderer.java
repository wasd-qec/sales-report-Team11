package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * CSV report renderer that serializes aggregated metrics for spreadsheet ingestion.
 */
public class CsvReportRenderer implements ReportRenderer {

    @Override
    public void render(MonthlyReport report, Writer writer) throws IOException {
        PrintWriter out = new PrintWriter(writer);

        // Section 1: Overview Metadata
        out.println("section,identifier,attribute_1,attribute_2,numeric_1,numeric_2");
        out.printf("OVERVIEW,%s,FilesLoaded,%d,Transactions,%d%n",
                report.month(), report.totalFilesLoaded(), report.totalTransactions());
        out.printf("OVERVIEW,%s,InvalidRowsSkipped,%d,TotalRevenue,%.2f%n",
                report.month(), report.invalidRowCount(), report.totalRevenue());

        // Section 2: Branch Summaries
        for (BranchSummary bs : report.branchSummaries()) {
            out.printf("BRANCH,%s,Transactions,%d,UnitsSold,%d,Revenue,%.2f%n",
                    bs.branch(), bs.transactionCount(), bs.totalUnitsSold(), bs.totalRevenue());
        }

        // Section 3: Payment Breakdown
        for (PaymentMethod pm : PaymentMethod.values()) {
            BigDecimal rev = report.totalRevenueByPaymentMethod().getOrDefault(pm, BigDecimal.ZERO);
            out.printf("PAYMENT_METHOD,%s,None,0,Revenue,%.2f%n", pm.name(), rev);
        }

        // Section 4: Top Products
        for (ProductTotal pt : report.topProducts()) {
            out.printf("PRODUCT,%s,%s,%s,%d,%.2f%n",
                    pt.sku(),
                    escapeCsv(pt.productName()),
                    escapeCsv(pt.category()),
                    pt.totalQuantitySold(),
                    pt.totalRevenue());
        }

        out.flush();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
