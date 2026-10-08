package edu.itc.salesreport.ingest;

/**
 * Observer interface for reacting to ingestion lifecycle events.
 */
@FunctionalInterface
public interface LoadListener {

    /**
     * Called whenever a LoadEvent is emitted by the loader.
     *
     * @param event the load event details
     */
    void onEvent(LoadEvent event);
}
