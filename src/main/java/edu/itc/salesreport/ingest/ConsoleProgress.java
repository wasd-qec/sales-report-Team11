package edu.itc.salesreport.ingest;

import java.io.PrintStream;

/**
 * Console observer that outputs visual progress updates and row validation warnings.
 */
public class ConsoleProgress implements LoadListener {

    private final PrintStream out;
    private final PrintStream err;

    public ConsoleProgress() {
        this(System.out, System.err);
    }

    public ConsoleProgress(PrintStream out, PrintStream err) {
        this.out = out != null ? out : System.out;
        this.err = err != null ? err : System.err;
    }

    @Override
    public void onEvent(LoadEvent event) {
        switch (event.getType()) {
            case FILE_STARTED -> out.printf("--> Loading %s...%n",
                    event.getFile() != null ? event.getFile().getFileName() : "file");
            case ROW_INVALID -> {
                String fileName = event.getFile() != null ? event.getFile().getFileName().toString() : "unknown";
                err.printf("    [WARN] Skipped invalid row (%s:%d): %s%n",
                        fileName,
                        event.getLineNumber(),
                        event.getError() != null ? event.getError().getMessage() : "Unknown parse failure");
            }
            case FILE_COMPLETED -> out.printf("    Completed %s (valid: %d, invalid: %d)%n",
                    event.getFile() != null ? event.getFile().getFileName() : "file",
                    event.getValidCount(),
                    event.getInvalidCount());
            case LOAD_COMPLETED -> out.printf("==> %s%n%n", event.getMessage());
            default -> {
                // ROW_PARSED is not logged individually to avoid console spamming
            }
        }
    }
}
