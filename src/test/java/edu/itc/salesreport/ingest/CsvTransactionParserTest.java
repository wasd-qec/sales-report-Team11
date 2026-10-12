package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionParserTest {

    private CsvTransactionParser parser;
    private final Path testFile = Path.of("data/2026-09/BTB-2026-09-01.csv");

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    @Test
    @DisplayName("Should successfully parse a well-formed CSV transaction row")
    void parseValidRow() throws InvalidRowException {
        String row = "BTB,2026-09-01,BTB-000001,SKU-2001,Mineral Water 1.5L,Beverages,2,0.45,0.00,KHQR";
        SaleTransaction tx = parser.parse(testFile, 2, row);

        assertNotNull(tx);
        assertEquals("BTB", tx.branch());
        assertEquals(LocalDate.of(2026, 9, 1), tx.date());
        assertEquals("BTB-000001", tx.receiptNo());
        assertEquals("SKU-2001", tx.sku());
        assertEquals("Mineral Water 1.5L", tx.productName());
        assertEquals("Beverages", tx.category());
        assertEquals(2, tx.quantity());
        assertEquals(new BigDecimal("0.45"), tx.unitPrice());
        assertEquals(new BigDecimal("0.00"), tx.discount());
        assertEquals(PaymentMethod.KHQR, tx.paymentMethod());
    }

    @Test
    @DisplayName("Should throw InvalidRowException when quantity is non-numeric like 'two'")
    void parseInvalidQuantity() {
        String badRow = "REP,2026-09-02,REP-000080,SKU-5001,USB-C Cable 1m,Electronics,two,4.50,0.10,CARD";
        InvalidRowException ex = assertThrows(InvalidRowException.class,
                () -> parser.parse(testFile, 11, badRow));

        assertEquals(11, ex.getLineNumber());
        assertEquals(badRow, ex.getRawLine());
        assertTrue(ex.getMessage().contains("quantity"), "Message should mention quantity error: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidRowException when payment method is unsupported like 'PAYPAL'")
    void parseInvalidPaymentMethod() {
        String badRow = "BTB,2026-09-04,BTB-000020,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00,PAYPAL";
        InvalidRowException ex = assertThrows(InvalidRowException.class,
                () -> parser.parse(testFile, 21, badRow));

        assertEquals(21, ex.getLineNumber());
        assertTrue(ex.getMessage().contains("PAYPAL"), "Message should mention PAYPAL: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidRowException when row has missing columns")
    void parseMissingColumns() {
        String badRow = "BTB,2026-09-01,BTB-000001,SKU-2001,Mineral Water";
        assertThrows(InvalidRowException.class, () -> parser.parse(testFile, 5, badRow));
    }

    @Test
    @DisplayName("Should throw InvalidRowException when row has invalid date format")
    void parseInvalidDate() {
        String badRow = "BTB,01-09-2026,BTB-000001,SKU-2001,Water,Beverages,1,1.00,0.00,CASH";
        assertThrows(InvalidRowException.class, () -> parser.parse(testFile, 5, badRow));
    }

    @Test
    @DisplayName("Should throw InvalidRowException for empty or blank lines")
    void parseEmptyLine() {
        assertThrows(InvalidRowException.class, () -> parser.parse(testFile, 1, ""));
        assertThrows(InvalidRowException.class, () -> parser.parse(testFile, 1, "   "));
    }
}
