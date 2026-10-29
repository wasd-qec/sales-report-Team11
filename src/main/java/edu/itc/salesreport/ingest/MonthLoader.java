package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MonthLoader {
    private final TransactionParser parser;
    private final List<LoadListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> rejectedRows = new ArrayList<>();

    public MonthLoader(TransactionParser parser) {
        this.parser = parser;
    }

    public Runnable subscribe(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public List<String> rejectedRows() {
        return List.copyOf(rejectedRows);
    }

    public List<SaleTransaction> load(YearMonth month, Path dataDir) {
        var started = Instant.now();
        rejectedRows.clear();
        var transactions = new ArrayList<SaleTransaction>();
        var directory = dataDir.resolve(month.toString());
        final List<Path> files;
        try (var paths = Files.list(directory)) {
            files = paths.filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("unable to list " + directory, e);
        }

        var acceptedTotal = 0;
        var rejectedTotal = 0;
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            var file = files.get(fileIndex);
            var accepted = 0;
            var rejected = 0;
            try {
                var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                    try {
                        transactions.add(parser.parse(lines.get(lineIndex)));
                        accepted++;
                    } catch (InvalidRowException e) {
                        rejected++;
                        rejectedRows.add(file.getFileName() + ":" + (lineIndex + 1)
                                + " " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("unable to read " + file, e);
            }
            acceptedTotal += accepted;
            rejectedTotal += rejected;
            publish(new LoadEvent.FileLoaded(file, accepted, rejected,
                    fileIndex + 1, files.size()));
        }
        publish(new LoadEvent.LoadFinished(acceptedTotal, rejectedTotal,
                Duration.between(started, Instant.now())));
        return List.copyOf(transactions);
    }

    private void publish(LoadEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
