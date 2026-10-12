package edu.itc.salesreport.ingest;

import java.nio.file.Path;

/**
 * Exception thrown when a record/row cannot be parsed due to invalid format or values.
 */
public class InvalidRowException extends Exception {
    private final Path file;
    private final int lineNumber;
    private final String rawLine;

    public InvalidRowException(Path file, int lineNumber, String rawLine, String reason) {
        super(String.format("Invalid row at line %d%s: %s [raw: \"%s\"]",
                lineNumber,
                file != null ? " in " + file.getFileName() : "",
                reason,
                rawLine));
        this.file = file;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public InvalidRowException(Path file, int lineNumber, String rawLine, String reason, Throwable cause) {
        super(String.format("Invalid row at line %d%s: %s [raw: \"%s\"]",
                lineNumber,
                file != null ? " in " + file.getFileName() : "",
                reason,
                rawLine), cause);
        this.file = file;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public Path getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getRawLine() {
        return rawLine;
    }
}
