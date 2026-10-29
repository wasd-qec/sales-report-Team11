package edu.itc.salesreport;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiIndependenceTest {
    @Test
    void modelIngestEngineAndToolkitFreeUiDoNotImportJavaFx() throws IOException {
        var sourceRoot = Path.of("src", "main", "java", "edu", "itc", "salesreport");
        for (String packageName : List.of("model", "ingest", "engine", "ui")) {
            var packageDirectory = sourceRoot.resolve(packageName);
            try (var files = Files.list(packageDirectory)) {
                for (var source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    var contents = Files.readString(source);
                    assertFalse(contents.contains("import javafx."),
                            source + " must remain independent of JavaFX");
                }
            }
        }
    }
}
