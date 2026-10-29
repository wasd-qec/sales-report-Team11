package edu.itc.salesreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DependencyRulesTest {
    private static final Path SRC = Path.of("src/main/java/edu/itc/salesreport");

    private static List<String> importsOf(String pkg) throws IOException {
        try (Stream<Path> files = Files.walk(SRC.resolve(pkg))) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(DependencyRulesTest::lines)
                    .filter(line -> line.startsWith("import edu.itc.salesreport."))
                    .toList();
        }
    }

    private static Stream<String> lines(Path path) {
        try {
            return Files.readAllLines(path).stream().map(String::strip);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void modelImportsNoOtherProjectPackage() throws IOException {
        var offending = importsOf("model").stream()
                .filter(line -> !line.startsWith("import edu.itc.salesreport.model."))
                .toList();
        assertEquals(List.of(), offending);
    }

    @Test
    void ingestDoesNotKnowRendering() throws IOException {
        var offending = importsOf("ingest").stream()
                .filter(line -> line.contains(".render."))
                .toList();
        assertEquals(List.of(), offending);
    }
}
