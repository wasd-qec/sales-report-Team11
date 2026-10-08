package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable domain record representing an individual line-item sale transaction.
 */
public record SaleTransaction(
        String branch,
        LocalDate date,
        String receiptNo,
        String sku,
        String productName,
        String category,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        PaymentMethod paymentMethod
) {
    public SaleTransaction {
        Objects.requireNonNull(branch, "branch must not be null");
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(receiptNo, "receiptNo must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(discount, "discount must not be null");
        Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");

        if (branch.isBlank()) {
            throw new IllegalArgumentException("branch must not be blank");
        }
        if (receiptNo.isBlank()) {
            throw new IllegalArgumentException("receiptNo must not be blank");
        }
        if (sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive, got: " + quantity);
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice cannot be negative, got: " + unitPrice);
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discount must be between 0.00 and 1.00, got: " + discount);
        }
    }

    /**
     * Calculates the gross sale amount before discount: quantity * unitPrice.
     */
    public BigDecimal getGrossAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the discount amount: grossAmount * discount.
     */
    public BigDecimal getDiscountAmount() {
        return getGrossAmount().multiply(discount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the final net line total after discount: grossAmount - discountAmount.
     */
    public BigDecimal getLineTotal() {
        return getGrossAmount().subtract(getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
    }
}
