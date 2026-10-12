package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Standard CSV implementation of TransactionParser for comma-separated sale records.
 * Expects exactly 10 fields:
 * branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
 */
public class CsvTransactionParser implements TransactionParser {

    private static final int EXPECTED_COLUMNS = 10;

    @Override
    public SaleTransaction parse(Path file, int lineNumber, String line) throws InvalidRowException {
        if (line == null || line.isBlank()) {
            throw new InvalidRowException(file, lineNumber, line, "Line is empty or whitespace");
        }

        String[] parts = line.split(",", -1);
        if (parts.length != EXPECTED_COLUMNS) {
            throw new InvalidRowException(file, lineNumber, line,
                    String.format("Expected %d columns, but got %d", EXPECTED_COLUMNS, parts.length));
        }

        String branch = parts[0].trim();
        String dateStr = parts[1].trim();
        String receiptNo = parts[2].trim();
        String sku = parts[3].trim();
        String productName = parts[4].trim();
        String category = parts[5].trim();
        String quantityStr = parts[6].trim();
        String unitPriceStr = parts[7].trim();
        String discountStr = parts[8].trim();
        String paymentStr = parts[9].trim();

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRowException(file, lineNumber, line, "Invalid date format: " + dateStr, e);
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            throw new InvalidRowException(file, lineNumber, line, "Invalid quantity integer: " + quantityStr, e);
        }

        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(unitPriceStr);
        } catch (NumberFormatException e) {
            throw new InvalidRowException(file, lineNumber, line, "Invalid unit price number: " + unitPriceStr, e);
        }

        BigDecimal discount;
        try {
            discount = new BigDecimal(discountStr);
        } catch (NumberFormatException e) {
            throw new InvalidRowException(file, lineNumber, line, "Invalid discount number: " + discountStr, e);
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.fromString(paymentStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException(file, lineNumber, line, e.getMessage(), e);
        }

        try {
            return new SaleTransaction(
                    branch,
                    date,
                    receiptNo,
                    sku,
                    productName,
                    category,
                    quantity,
                    unitPrice,
                    discount,
                    paymentMethod
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException(file, lineNumber, line, "Domain validation error: " + e.getMessage(), e);
        }
    }
}
