package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Text/ASCII table renderer for terminal output and log files.
 */
public class TextReportRenderer implements ReportRenderer {

    @Override
    public void render(MonthlyReport report, Writer writer) throws IOException {
        PrintWriter out = new PrintWriter(writer);

        out.println("===============================================================================");
        out.printf("                    MONTHLY SALES REPORT - %s%n", report.month());
        out.println("===============================================================================");
        out.printf(" Files Processed   : %d%n", report.totalFilesLoaded());
        out.printf(" Valid Transactions: %d%n", report.totalTransactions());
        out.printf(" Invalid Rows      : %d (skipped)%n", report.invalidRowCount());
        out.printf(" Total Units Sold  : %d%n", report.totalUnitsSold());
        out.printf(" Total Revenue     : $%,.2f%n", report.totalRevenue());
        out.println("-------------------------------------------------------------------------------");
        out.println();

        // Branch Breakdown
        out.println("--- BRANCH PERFORMANCE ---");
        out.println("+--------+--------------+------------+----------------+-----------------------+");
        out.println("| Branch | Transactions | Units Sold |  Total Revenue | Top Payment Method    |");
        out.println("+--------+--------------+------------+----------------+-----------------------+");
        for (BranchSummary bs : report.branchSummaries()) {
            PaymentMethod topMethod = PaymentMethod.CASH;
            BigDecimal maxPaymentRev = BigDecimal.ZERO;
            for (var entry : bs.revenueByPaymentMethod().entrySet()) {
                if (entry.getValue().compareTo(maxPaymentRev) > 0) {
                    maxPaymentRev = entry.getValue();
                    topMethod = entry.getKey();
                }
            }

            out.printf("| %-6s | %12d | %10d | $%13.2f | %-21s |%n",
                    bs.branch(),
                    bs.transactionCount(),
                    bs.totalUnitsSold(),
                    bs.totalRevenue(),
                    topMethod + " ($" + String.format("%.2f", maxPaymentRev) + ")");
        }
        out.println("+--------+--------------+------------+----------------+-----------------------+");
        out.println();

        // Payment Method Breakdown
        out.println("--- REVENUE BY PAYMENT METHOD ---");
        out.println("+----------------+----------------+------------+");
        out.println("| Payment Method |        Revenue | Share (%)  |");
        out.println("+----------------+----------------+------------+");
        for (PaymentMethod pm : PaymentMethod.values()) {
            BigDecimal rev = report.totalRevenueByPaymentMethod().getOrDefault(pm, BigDecimal.ZERO);
            BigDecimal share = report.totalRevenue().compareTo(BigDecimal.ZERO) > 0
                    ? rev.multiply(BigDecimal.valueOf(100)).divide(report.totalRevenue(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            out.printf("| %-14s | $%13.2f | %9.1f%% |%n", pm, rev, share);
        }
        out.println("+----------------+----------------+------------+");
        out.println();

        // Top Products
        out.println("--- TOP PRODUCTS BY REVENUE ---");
        out.println("+----------+----------------------+---------------+------------+----------------+");
        out.println("| SKU      | Product Name         | Category      | Units Sold |  Total Revenue |");
        out.println("+----------+----------------------+---------------+------------+----------------+");
        int count = 0;
        for (ProductTotal pt : report.topProducts()) {
            if (++count > 10) break;
            out.printf("| %-8s | %-20s | %-13s | %10d | $%13.2f |%n",
                    pt.sku(),
                    pt.productName().length() > 20 ? pt.productName().substring(0, 17) + "..." : pt.productName(),
                    pt.category(),
                    pt.totalQuantitySold(),
                    pt.totalRevenue());
        }
        out.println("+----------+----------------------+---------------+------------+----------------+");
        out.println();
        out.println("===============================================================================");
        out.flush();
    }
}
