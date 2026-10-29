package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class CsvMonthReader {
    private static final Pattern FILE_NAME = Pattern.compile(
            "(PNH|REP|BTB)-\\d{4}-\\d{2}-\\d{2}\\.csv");
    private final TransactionParser parser;

    public CsvMonthReader(TransactionParser parser) {
        this.parser = parser;
    }

    public MonthData read(Path dataDir, YearMonth month) throws IOException {
        Path directory = dataDir.resolve(month.toString());
        if (!Files.isDirectory(directory)) {
            throw new NoSuchFileException(directory.toString());
        }
        List<Path> files;
        try (var paths = Files.list(directory)) {
            files = paths.filter(path -> FILE_NAME.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        var rows = new ArrayList<SaleTransaction>();
        var rejected = new ArrayList<String>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                try {
                    rows.add(parser.parse(lines.get(i)));
                } catch (InvalidRowException e) {
                    rejected.add(file.getFileName() + ":" + (i + 1) + " " + e.getMessage());
                }
            }
        }
        return new MonthData(month, rows, rejected);
    }
}
