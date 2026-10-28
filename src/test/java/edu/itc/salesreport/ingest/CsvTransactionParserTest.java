package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionParserTest {

    private final TransactionParser parser = new CsvTransactionParser();

    @Test
    void parsesAValidRow() throws InvalidRowException {
        var t = parser.parse("PNH,2026-09-01,PNH-000123,"
                + "SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR");
        assertEquals("PNH", t.branch());
        // TODO: date, productName, quantity, unitPrice, paymentMethod
        assertEquals(new BigDecimal("13.00"), t.revenue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00",
        "PNH,2026-09-31,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH"
        // TODO: at least 6 more invalid rows, one rule each
    })
    void rejectsInvalidRows(String line) {
        assertThrows(InvalidRowException.class, () -> parser.parse(line));
    }
}