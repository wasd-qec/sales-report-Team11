package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregated sales statistics for a specific product.
 */
public record ProductTotal(
        String sku,
        String productName,
        String category,
        int totalQuantitySold,
        BigDecimal totalRevenue
) {
    public ProductTotal {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");

        if (totalQuantitySold < 0) {
            throw new IllegalArgumentException("totalQuantitySold cannot be negative: " + totalQuantitySold);
        }
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalRevenue cannot be negative: " + totalRevenue);
        }
    }
}
