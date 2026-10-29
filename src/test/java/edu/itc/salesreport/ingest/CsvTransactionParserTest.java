package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionParserTest {

    private final TransactionParser parser = new CsvTransactionParser();

    @Test
    void parsesAValidRow() throws InvalidRowException {
        var t = parser.parse("PNH,2026-09-01,PNH-000123,"
                + "SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR");
        assertEquals("PNH", t.branch());
        assertEquals(LocalDate.of(2026, 9, 1), t.date());
        assertEquals("PNH-000123", t.receiptNo());
        assertEquals("SKU-1001", t.sku());
        assertEquals("Jasmine Rice 5kg", t.productName());
        assertEquals("Grocery", t.category());
        assertEquals(2, t.quantity());
        assertEquals(new BigDecimal("6.50"), t.unitPrice());
        assertEquals(new BigDecimal("0.00"), t.discount());
        assertEquals(PaymentMethod.KHQR, t.paymentMethod());
        assertEquals(new BigDecimal("13.00"), t.revenue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "XXX,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,not-a-date,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Unknown,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,zero,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,cost,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,TRANSFER",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00",
        "PNH,2026-09-31,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,0,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,-1.00,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,13.01,CASH"
    })
    void rejectsInvalidRows(String line) {
        assertThrows(InvalidRowException.class, () -> parser.parse(line));
    }

    @Test
    void trimsFieldsBeforeParsing() throws InvalidRowException {
        var t = parser.parse(" PNH , 2026-09-01 , PNH-1 , SKU-1 , Rice , "
                + "Grocery , 2 , 6.50 , 0.00 , CASH ");

        assertEquals("PNH", t.branch());
        assertEquals(LocalDate.of(2026, 9, 1), t.date());
        assertEquals("PNH-1", t.receiptNo());
        assertEquals(PaymentMethod.CASH, t.paymentMethod());
    }
}