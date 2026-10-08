package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level immutable report aggregating metrics across all branches and products for a month.
 */
public record MonthlyReport(
        YearMonth month,
        int totalFilesLoaded,
        int totalTransactions,
        int invalidRowCount,
        int totalUnitsSold,
        BigDecimal totalRevenue,
        List<BranchSummary> branchSummaries,
        List<ProductTotal> topProducts,
        Map<PaymentMethod, BigDecimal> totalRevenueByPaymentMethod
) {
    public MonthlyReport {
        Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
        Objects.requireNonNull(branchSummaries, "branchSummaries must not be null");
        Objects.requireNonNull(topProducts, "topProducts must not be null");
        Objects.requireNonNull(totalRevenueByPaymentMethod, "totalRevenueByPaymentMethod must not be null");

        if (totalFilesLoaded < 0) {
            throw new IllegalArgumentException("totalFilesLoaded cannot be negative");
        }
        if (totalTransactions < 0) {
            throw new IllegalArgumentException("totalTransactions cannot be negative");
        }
        if (invalidRowCount < 0) {
            throw new IllegalArgumentException("invalidRowCount cannot be negative");
        }
        if (totalUnitsSold < 0) {
            throw new IllegalArgumentException("totalUnitsSold cannot be negative");
        }
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalRevenue cannot be negative");
        }

        branchSummaries = List.copyOf(branchSummaries);
        topProducts = List.copyOf(topProducts);
        totalRevenueByPaymentMethod = Collections.unmodifiableMap(totalRevenueByPaymentMethod);
    }
}
