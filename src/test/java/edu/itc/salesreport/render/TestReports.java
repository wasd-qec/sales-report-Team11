package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Test fixture utility providing standard MonthlyReport instances for renderer tests.
 */
public final class TestReports {

    private TestReports() {}

    public static MonthlyReport createSampleReport() {
        YearMonth month = YearMonth.of(2026, 9);

        BranchSummary pnh = new BranchSummary(
                "PNH",
                100,
                250,
                new BigDecimal("1250.00"),
                Map.of(
                        PaymentMethod.CASH, new BigDecimal("500.00"),
                        PaymentMethod.CARD, new BigDecimal("350.00"),
                        PaymentMethod.KHQR, new BigDecimal("400.00")
                )
        );

        BranchSummary rep = new BranchSummary(
                "REP",
                80,
                190,
                new BigDecimal("950.00"),
                Map.of(
                        PaymentMethod.CASH, new BigDecimal("300.00"),
                        PaymentMethod.CARD, new BigDecimal("250.00"),
                        PaymentMethod.KHQR, new BigDecimal("400.00")
                )
        );

        ProductTotal rice = new ProductTotal(
                "SKU-1001",
                "Jasmine Rice 5kg",
                "Grocery",
                150,
                new BigDecimal("975.00")
        );

        ProductTotal water = new ProductTotal(
                "SKU-2001",
                "Mineral Water 1.5L",
                "Beverages",
                290,
                new BigDecimal("130.50")
        );

        Map<PaymentMethod, BigDecimal> totalPayments = Map.of(
                PaymentMethod.CASH, new BigDecimal("800.00"),
                PaymentMethod.CARD, new BigDecimal("600.00"),
                PaymentMethod.KHQR, new BigDecimal("800.00")
        );

        return new MonthlyReport(
                month,
                15,
                180,
                2,
                440,
                new BigDecimal("2200.00"),
                List.of(pnh, rep),
                List.of(rice, water),
                totalPayments
        );
    }
}
