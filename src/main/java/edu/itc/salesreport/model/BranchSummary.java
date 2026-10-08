package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated sales statistics for a specific branch.
 */
public record BranchSummary(
        String branch,
        int transactionCount,
        int totalUnitsSold,
        BigDecimal totalRevenue,
        Map<PaymentMethod, BigDecimal> revenueByPaymentMethod
) {
    public BranchSummary {
        Objects.requireNonNull(branch, "branch must not be null");
        Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
        Objects.requireNonNull(revenueByPaymentMethod, "revenueByPaymentMethod must not be null");

        if (branch.isBlank()) {
            throw new IllegalArgumentException("branch must not be blank");
        }
        if (transactionCount < 0) {
            throw new IllegalArgumentException("transactionCount cannot be negative: " + transactionCount);
        }
        if (totalUnitsSold < 0) {
            throw new IllegalArgumentException("totalUnitsSold cannot be negative: " + totalUnitsSold);
        }
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalRevenue cannot be negative: " + totalRevenue);
        }

        revenueByPaymentMethod = Collections.unmodifiableMap(revenueByPaymentMethod);
    }
}
