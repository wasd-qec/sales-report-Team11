package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonthLoaderTest {
    @Test
    void loadsSortedFilesAndPublishesProgress(@TempDir Path temp) throws Exception {
        var month = temp.resolve("2026-09");
        Files.createDirectories(month);
        var header = CsvTransactionParser.HEADER;
        var valid = "PNH,2026-09-01,R-1,SKU-1,Rice,Grocery,1,2.00,0.00,KHQR";
        var invalid = "PNH,2026-09-01,R-2,SKU-2,Rice,Grocery,two,2.00,0.00,KHQR";
        Files.writeString(month.resolve("b.csv"), header + "\n" + valid + "\n");
        Files.writeString(month.resolve("a.csv"), header + "\n" + valid + "\n" + invalid + "\n");

        var events = new ArrayList<LoadEvent>();
        var loader = new MonthLoader(new CsvTransactionParser());
        loader.subscribe(events::add);
        var transactions = loader.load(YearMonth.of(2026, 9), temp);

        assertEquals(2, transactions.size());
        assertEquals(3, events.size());
        assertEquals("a.csv", ((LoadEvent.FileLoaded) events.get(0)).file().getFileName().toString());
        assertEquals(1, ((LoadEvent.FileLoaded) events.get(0)).accepted());
        assertEquals(1, ((LoadEvent.FileLoaded) events.get(0)).rejected());
        var finished = (LoadEvent.LoadFinished) events.get(2);
        assertEquals(2, finished.accepted());
        assertEquals(1, finished.rejected());
        assertTrue(loader.rejectedRows().get(0).startsWith("a.csv:3 "));
    }

    @Test
    void unsubscribeStopsEvents(@TempDir Path temp) throws Exception {
        var month = temp.resolve("2026-09");
        Files.createDirectories(month);
        Files.writeString(month.resolve("a.csv"), CsvTransactionParser.HEADER + "\n");
        var events = new ArrayList<LoadEvent>();
        var loader = new MonthLoader(new CsvTransactionParser());
        var unsubscribe = loader.subscribe(events::add);
        unsubscribe.run();
        loader.load(YearMonth.of(2026, 9), temp);
        assertEquals(List.of(), events);
    }
}
