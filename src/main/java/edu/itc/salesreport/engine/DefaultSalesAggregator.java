package edu.itc.salesreport.engine;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.SaleTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultSalesAggregator implements SalesAggregator {
    @Override
    public BranchSummary summarize(String branch, List<SaleTransaction> rows) {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        Set<String> receipts = new TreeSet<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        Map<PaymentMethod, BigDecimal> byPayment = new EnumMap<>(PaymentMethod.class);
        Map<String, ProductAccumulator> products = new HashMap<>();
        for (SaleTransaction row : rows) {
            revenue = revenue.add(row.revenue());
            discounts = discounts.add(row.discount());
            receipts.add(row.receiptNo());
            byCategory.merge(row.category(), row.revenue(), BigDecimal::add);
            byPayment.merge(row.paymentMethod(), row.revenue(), BigDecimal::add);
            products.computeIfAbsent(row.sku(), ignored ->
                    new ProductAccumulator(row.productName())).add(row);
        }
        List<ProductTotal> topProducts = products.entrySet().stream()
                .map(entry -> entry.getValue().toProduct(entry.getKey()))
                .sorted(Comparator.comparing(ProductTotal::revenue).reversed()
                        .thenComparing(ProductTotal::sku))
                .limit(10)
                .toList();
        return new BranchSummary(branch, money(revenue), money(discounts), receipts.size(),
                byCategory, topProducts, byPayment);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class ProductAccumulator {
        private final String name;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long quantity;

        private ProductAccumulator(String name) {
            this.name = name;
        }

        private void add(SaleTransaction row) {
            revenue = revenue.add(row.revenue());
            quantity += row.quantity();
        }

        private ProductTotal toProduct(String sku) {
            return new ProductTotal(sku, name, money(revenue), quantity);
        }
    }
}
