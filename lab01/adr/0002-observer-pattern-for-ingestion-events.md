# ADR 0002: Observer Pattern for Ingestion Monitoring and Error Handling

## Context and Problem Statement
When ingesting daily batches from multiple store locations, files may contain corrupted data or format anomalies (e.g., non-numeric quantities or unrecognized payment methods). Furthermore, users need progress tracking during execution, while headless batch runs require silent logging or metrics monitoring. Directly embedding console logging (`System.out.println`) into `MonthLoader` or `CsvTransactionParser` would violate the Single Responsibility Principle (SRP) and couple ingestion core logic to terminal output.

## Decision Drivers
* Decoupling business logic from user interface / logging frameworks.
* Fault isolation: errors on individual lines must not crash the batch job.
* Extensibility: support multiple UI/logging targets (CLI progress bars, SLF4J loggers, test event recorders).
* Clean testability: ability to assert event counts without redirecting standard streams.

## Considered Options
1. Hardcoded `System.out.println` / `System.err.println` inside `MonthLoader`.
2. Direct SLF4J / Logback logging framework integration inside `MonthLoader`.
3. Observer Pattern with `LoadEvent` and `LoadListener` abstractions.

## Decision Outcome
Chosen option: **Observer Pattern with `LoadEvent` and `LoadListener` interfaces**.

### Rationale
* `MonthLoader` focuses purely on scanning files, catching `InvalidRowException`, and assembling aggregates.
* `ConsoleProgress` implements `LoadListener` to handle user-facing console printing and warning diagnostics.
* Unit tests (such as `MonthLoaderTest`) can attach anonymous listeners to verify parsing stages, error counts, and lifecycle transitions without mocking console output.
* If future requirements dictate sending progress updates to a web dashboard or monitoring daemon, a new `LoadListener` can be added without changing a single line of `MonthLoader`.

## Consequences
### Positive
* Perfect separation of concerns (SoC).
* Non-blocking error recovery: invalid rows emit `ROW_INVALID` events and processing proceeds cleanly.
* Highly testable event contract.

### Negative / Trade-offs
* Minor overhead of instantiating lightweight event objects per file/error (negligible in relation to I/O costs).
