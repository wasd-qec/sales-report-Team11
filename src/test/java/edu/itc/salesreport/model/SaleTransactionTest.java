package edu.itc.salesreport.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionTest {

    @Test
    @DisplayName("Should correctly calculate gross, discount, and net line total")
    void calculationsAreAccurate() {
        SaleTransaction tx = new SaleTransaction(
                "PNH",
                LocalDate.of(2026, 9, 1),
                "PNH-000001",
                "SKU-1001",
                "Jasmine Rice 5kg",
                "Grocery",
                3,
                new BigDecimal("6.50"),
                new BigDecimal("0.10"),
                PaymentMethod.KHQR
        );

        // 3 * 6.50 = 19.50
        assertEquals(new BigDecimal("19.50"), tx.getGrossAmount());
        // 19.50 * 0.10 = 1.95
        assertEquals(new BigDecimal("1.95"), tx.getDiscountAmount());
        // 19.50 - 1.95 = 17.55
        assertEquals(new BigDecimal("17.55"), tx.getLineTotal());
    }

    @Test
    @DisplayName("Should correctly calculate with zero discount")
    void zeroDiscountCalculation() {
        SaleTransaction tx = new SaleTransaction(
                "BTB",
                LocalDate.of(2026, 9, 2),
                "BTB-000042",
                "SKU-2001",
                "Mineral Water 1.5L",
                "Beverages",
                4,
                new BigDecimal("0.45"),
                BigDecimal.ZERO,
                PaymentMethod.CASH
        );

        // 4 * 0.45 = 1.80
        assertEquals(new BigDecimal("1.80"), tx.getGrossAmount());
        assertEquals(new BigDecimal("0.00"), tx.getDiscountAmount());
        assertEquals(new BigDecimal("1.80"), tx.getLineTotal());
    }

    @Test
    @DisplayName("Should reject invalid or out-of-range arguments")
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "BTB", LocalDate.now(), "R-1", "SKU-1", "Name", "Cat",
                0, new BigDecimal("1.00"), BigDecimal.ZERO, PaymentMethod.CASH
        ), "Quantity must be > 0");

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "BTB", LocalDate.now(), "R-1", "SKU-1", "Name", "Cat",
                -2, new BigDecimal("1.00"), BigDecimal.ZERO, PaymentMethod.CASH
        ), "Quantity must not be negative");

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "BTB", LocalDate.now(), "R-1", "SKU-1", "Name", "Cat",
                1, new BigDecimal("-5.00"), BigDecimal.ZERO, PaymentMethod.CASH
        ), "Unit price must not be negative");

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "BTB", LocalDate.now(), "R-1", "SKU-1", "Name", "Cat",
                1, new BigDecimal("5.00"), new BigDecimal("1.50"), PaymentMethod.CASH
        ), "Discount must be <= 1.00");

        assertThrows(NullPointerException.class, () -> new SaleTransaction(
                null, LocalDate.now(), "R-1", "SKU-1", "Name", "Cat",
                1, new BigDecimal("5.00"), BigDecimal.ZERO, PaymentMethod.CASH
        ), "Branch cannot be null");
    }

    @Test
    @DisplayName("PaymentMethod parsing handles case insensitivity and rejects unknown values")
    void paymentMethodParsing() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("cash"));
        assertEquals(PaymentMethod.CARD, PaymentMethod.fromString("CARD"));
        assertEquals(PaymentMethod.KHQR, PaymentMethod.fromString("khqr"));

        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromString("PAYPAL"));
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromString(null));
    }
}
