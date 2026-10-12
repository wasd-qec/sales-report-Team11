package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.nio.file.Path;

/**
 * Event object emitted during the data ingestion lifecycle.
 */
public class LoadEvent {

    public enum Type {
        FILE_STARTED,
        ROW_PARSED,
        ROW_INVALID,
        FILE_COMPLETED,
        LOAD_COMPLETED
    }

    private final Type type;
    private final Path file;
    private final int lineNumber;
    private final SaleTransaction transaction;
    private final InvalidRowException error;
    private final String message;
    private final int validCount;
    private final int invalidCount;

    public LoadEvent(
            Type type,
            Path file,
            int lineNumber,
            SaleTransaction transaction,
            InvalidRowException error,
            String message,
            int validCount,
            int invalidCount
    ) {
        this.type = type;
        this.file = file;
        this.lineNumber = lineNumber;
        this.transaction = transaction;
        this.error = error;
        this.message = message;
        this.validCount = validCount;
        this.invalidCount = invalidCount;
    }

    public static LoadEvent fileStarted(Path file) {
        return new LoadEvent(
                Type.FILE_STARTED,
                file,
                0,
                null,
                null,
                "Started loading file: " + (file != null ? file.getFileName() : "N/A"),
                0,
                0
        );
    }

    public static LoadEvent rowParsed(Path file, int lineNumber, SaleTransaction transaction) {
        return new LoadEvent(
                Type.ROW_PARSED,
                file,
                lineNumber,
                transaction,
                null,
                null,
                1,
                0
        );
    }

    public static LoadEvent rowInvalid(Path file, int lineNumber, InvalidRowException error) {
        return new LoadEvent(
                Type.ROW_INVALID,
                file,
                lineNumber,
                null,
                error,
                error.getMessage(),
                0,
                1
        );
    }

    public static LoadEvent fileCompleted(Path file, int validCount, int invalidCount) {
        return new LoadEvent(
                Type.FILE_COMPLETED,
                file,
                0,
                null,
                null,
                String.format("Finished %s [Valid: %d, Invalid: %d]",
                        file != null ? file.getFileName() : "file", validCount, invalidCount),
                validCount,
                invalidCount
        );
    }

    public static LoadEvent loadCompleted(int totalFiles, int totalValid, int totalInvalid) {
        return new LoadEvent(
                Type.LOAD_COMPLETED,
                null,
                0,
                null,
                null,
                String.format("Ingestion complete: %d files, %d valid transactions, %d invalid rows",
                        totalFiles, totalValid, totalInvalid),
                totalValid,
                totalInvalid
        );
    }

    public Type getType() {
        return type;
    }

    public Path getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public SaleTransaction getTransaction() {
        return transaction;
    }

    public InvalidRowException getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getValidCount() {
        return validCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }
}
