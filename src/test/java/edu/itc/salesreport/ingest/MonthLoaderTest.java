package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthLoaderTest {

    @Test
    @DisplayName("MonthLoader should process files, notify listeners, skip invalid rows, and aggregate report")
    void testLoadingAndEventDispatching(@TempDir Path tempDir) throws IOException {
        // Create 2 test CSV files in tempDir
        String header = "branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method\n";
        String row1 = "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,CASH\n";
        String row2 = "PNH,2026-09-01,PNH-000002,SKU-2001,Mineral Water 1.5L,Beverages,3,0.45,0.00,KHQR\n";
        String badRow1 = "PNH,2026-09-01,PNH-000003,SKU-2001,Mineral Water 1.5L,Beverages,bad_qty,0.45,0.00,KHQR\n";

        Files.writeString(tempDir.resolve("PNH-2026-09-01.csv"), header + row1 + row2 + badRow1);

        String row3 = "BTB,2026-09-02,BTB-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.10,CARD\n";
        String badRow2 = "BTB,2026-09-02,BTB-000002,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,0.00,CRYPTO\n";

        Files.writeString(tempDir.resolve("BTB-2026-09-02.csv"), header + row3 + badRow2);

        List<LoadEvent> recordedEvents = new ArrayList<>();
        MonthLoader loader = new MonthLoader();
        loader.addListener(recordedEvents::add);

        YearMonth month = YearMonth.of(2026, 9);
        MonthlyReport report = loader.loadMonth(month, tempDir);

        assertNotNull(report);
        assertEquals(2, report.totalFilesLoaded());
        assertEquals(3, report.totalTransactions(), "Should have 3 valid transactions");
        assertEquals(2, report.invalidRowCount(), "Should have skipped 2 invalid rows");

        // Units: 2 (row1) + 3 (row2) + 1 (row3) = 6 units
        assertEquals(6, report.totalUnitsSold());

        // Revenue:
        // row1: 2 * 6.50 = 13.00
        // row2: 3 * 0.45 = 1.35
        // row3: 1 * 6.50 * 0.90 = 5.85
        // Total = 13.00 + 1.35 + 5.85 = 20.20
        assertEquals(new BigDecimal("20.20"), report.totalRevenue());

        // Verify event dispatching
        long fileStartEvents = recordedEvents.stream().filter(e -> e.getType() == LoadEvent.Type.FILE_STARTED).count();
        long rowInvalidEvents = recordedEvents.stream().filter(e -> e.getType() == LoadEvent.Type.ROW_INVALID).count();
        long rowParsedEvents = recordedEvents.stream().filter(e -> e.getType() == LoadEvent.Type.ROW_PARSED).count();
        long completedEvents = recordedEvents.stream().filter(e -> e.getType() == LoadEvent.Type.LOAD_COMPLETED).count();

        assertEquals(2, fileStartEvents);
        assertEquals(2, rowInvalidEvents);
        assertEquals(3, rowParsedEvents);
        assertEquals(1, completedEvents);

        // Branch summary checks
        assertEquals(2, report.branchSummaries().size());
        assertTrue(report.branchSummaries().stream().anyMatch(b -> b.branch().equals("PNH")));
        assertTrue(report.branchSummaries().stream().anyMatch(b -> b.branch().equals("BTB")));

        // Payment breakdown checks
        assertEquals(new BigDecimal("13.00"), report.totalRevenueByPaymentMethod().get(PaymentMethod.CASH));
        assertEquals(new BigDecimal("1.35"), report.totalRevenueByPaymentMethod().get(PaymentMethod.KHQR));
        assertEquals(new BigDecimal("5.85"), report.totalRevenueByPaymentMethod().get(PaymentMethod.CARD));
    }
}
