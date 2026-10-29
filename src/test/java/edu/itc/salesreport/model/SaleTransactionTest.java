package edu.itc.salesreport.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SaleTransactionTest {

    @Test
    void roundsRevenueHalfUp() {
        var transaction = new SaleTransaction(
                "PNH",
                LocalDate.of(2026, 9, 1),
                "PNH-1",
                "SKU-1",
                "Rice",
                "Grocery",
                3,
                new BigDecimal("0.335"),
                BigDecimal.ZERO,
                PaymentMethod.CASH);

        assertEquals(new BigDecimal("1.01"), transaction.revenue());
    }

    @Test
    void branchSummaryTopProductsIsUnmodifiable() {
        var products = new ArrayList<>(List.of(
                new ProductTotal("SKU-1", "Rice", new BigDecimal("10.00"), 2)));
        var summary = new BranchSummary(
                "PNH",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                1,
                Map.of("Grocery", new BigDecimal("10.00")),
                products,
                Map.of(PaymentMethod.CASH, new BigDecimal("10.00")));

        products.add(new ProductTotal(
                "SKU-2", "Tea", new BigDecimal("2.00"), 1));

        assertEquals(1, summary.topProducts().size());
        assertThrows(UnsupportedOperationException.class, () -> summary
                .topProducts().add(new ProductTotal(
                        "SKU-3", "Coffee", new BigDecimal("1.00"), 1)));
    }
}
