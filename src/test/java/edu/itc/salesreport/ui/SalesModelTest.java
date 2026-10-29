package edu.itc.salesreport.ui;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SalesModelTest {
    private SalesModel model(java.nio.file.Path dir) throws Exception {
        TestData.writeSample(dir);
        return new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
    }

    @Test
    void loadPublishesEventAndChainSummary() throws Exception {
        var dir = Files.createTempDirectory("sales-model");
        var model = model(dir);
        var events = new ArrayList<SalesEvent>();
        model.addListener(events::add);
        model.load(dir, YearMonth.of(2026, 9));
        assertEquals(List.of(new SalesEvent.MonthLoaded(YearMonth.of(2026, 9), 3, 1)), events);
        assertEquals(new BigDecimal("52.00"), model.summary().revenue());
    }

    @Test
    void selectingBranchPublishesOnlyOnChange() throws Exception {
        var dir = Files.createTempDirectory("sales-model");
        var model = model(dir);
        var events = new ArrayList<SalesEvent>();
        model.addListener(events::add);
        model.load(dir, YearMonth.of(2026, 9));
        events.clear();
        model.selectBranch("PNH");
        model.selectBranch("PNH");
        assertEquals(1, events.size());
        assertEquals(new BigDecimal("31.00"), model.summary().revenue());
        assertEquals(new BigDecimal("31.00"), model.summary().averageBasket());
    }

    @Test
    void failedLoadKeepsOldStateAndPublishesNothing() throws Exception {
        var dir = Files.createTempDirectory("sales-model");
        var model = model(dir);
        model.load(dir, YearMonth.of(2026, 9));
        var events = new ArrayList<SalesEvent>();
        model.addListener(events::add);
        assertThrows(NoSuchFileException.class,
                () -> model.load(dir, YearMonth.of(2026, 7)));
        assertEquals(3, model.visibleTransactions().size());
        assertTrue(events.isEmpty());
    }

    @Test
    void removedListenerIsNotCalled() throws Exception {
        var dir = Files.createTempDirectory("sales-model");
        var model = model(dir);
        var events = new ArrayList<SalesEvent>();
        SalesModelListener listener = events::add;
        model.addListener(listener);
        model.removeListener(listener);
        model.load(dir, YearMonth.of(2026, 9));
        assertTrue(events.isEmpty());
    }
}
