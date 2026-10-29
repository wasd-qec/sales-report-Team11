package edu.itc.salesreport.ingest;

import java.nio.file.Path;
import java.time.Duration;

public sealed interface LoadEvent
        permits LoadEvent.FileLoaded, LoadEvent.LoadFinished {
    record FileLoaded(Path file, int accepted, int rejected,
            int filesDone, int filesTotal) implements LoadEvent { }

    record LoadFinished(int accepted, int rejected, Duration took)
            implements LoadEvent { }
}
