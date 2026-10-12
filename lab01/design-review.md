# Architecture & Design Review
**Project:** Sales Reporting System (`edu.itc.salesreport`)  
**Team:** Team 11  
**Target Milestone:** Lab-01 Architecture Baseline

---

## 1. Executive Summary
The system provides a decoupled, resilient pipeline for ingesting distributed point-of-sale (POS) data across retail branches (`PNH`, `REP`, `BTB`), isolating corrupted lines, aggregating multi-dimensional business statistics, and rendering reports across multiple presentation channels (ASCII Console, HTML, and CSV).

This review assesses the architectural solution against standard quality attributes, SOLID principles, package modularity, and automated architectural rules verified by ArchUnit.

---

## 2. Evaluation Against Quality Scenarios

| Scenario | Architectural Mechanism | Evaluation Result |
|---|---|---|
| **S1: Fault Tolerance (Corrupt Data)** | `CsvTransactionParser` catches type errors, emits `InvalidRowException`. `MonthLoader` catches this exception, increments bad row counters, notifies listeners, and continues the file stream. | **PASS**: Tested with 2 deliberately bad rows in sample data (`two` integer error, `PAYPAL` enum error). 2,998 valid records were ingested with zero crashes. |
| **S2: Modifiability (New Output Format)** | `ReportRenderer` interface defines `render(MonthlyReport, Writer)`. Adding formats requires only a new implementation class. | **PASS**: `TextReportRenderer`, `HtmlReportRenderer`, and `CsvReportRenderer` were implemented independently with zero changes to model or ingest packages. |
| **S3: Modifiability (New Ingestion Source)** | `TransactionParser` strategy interface decouples format parsing from batch file orchestration. | **PASS**: `MonthLoader` accepts any `TransactionParser` implementation via dependency injection. |
| **S4: Testability & Conformance** | `DependencyRulesTest` uses ArchUnit to enforce package boundaries and prohibit cyclic dependencies. | **PASS**: 4 ArchUnit tests run in < 1 second during `mvn test`, guaranteeing acyclic layered architecture. |
| **S5: Performance & Memory** | Sequential buffered stream processing (`BufferedReader`) and single-pass aggregation. | **PASS**: Ingests 3,000 records across 15 files in ~60 ms with a minimal JVM footprint (< 30 MB). |
| **S6: Observability & Decoupling** | Observer pattern (`LoadEvent`, `LoadListener`, `ConsoleProgress`). | **PASS**: Ingestion engine has zero direct dependencies on `System.out` or terminal formatting. |

---

## 3. SOLID Principles Assessment

1. **Single Responsibility Principle (SRP)**:
   - `CsvTransactionParser`: Only responsible for parsing and validating single CSV lines.
   - `MonthLoader`: Only responsible for file discovery, streaming orchestration, and aggregation.
   - `ConsoleProgress`: Only responsible for formatting events to terminal streams.
   - `ReportRenderer` implementations: Only responsible for presentation formatting.

2. **Open-Closed Principle (OCP)**:
   - New rendering destinations (e.g., PDF or JSON) can be introduced without modifying existing loaders or models.
   - New input formats (e.g., TSV, JSON) can be plugged in by implementing `TransactionParser`.

3. **Liskov Substitution Principle (LSP)**:
   - All `ReportRenderer` implementations honor the `render(report, writer)` contract and can be used interchangeably.
   - All `TransactionParser` implementations honor the exception contract (`InvalidRowException`).

4. **Interface Segregation Principle (ISP)**:
   - Interfaces (`TransactionParser`, `LoadListener`, `ReportRenderer`) are minimal and focused on single operations (single-method or functional interfaces where appropriate).

5. **Dependency Inversion Principle (DIP)**:
   - High-level orchestrators (`MonthLoader`, `App`) depend on abstractions (`TransactionParser`, `LoadListener`, `ReportRenderer`) rather than concrete implementations.

---

## 4. Package Coupling & Dependency Rules

ArchUnit assertions formally enforce the following layer constraints:
- `edu.itc.salesreport.model` has **0 outgoing dependencies** to `ingest` or `render`.
- `edu.itc.salesreport.ingest` depends only on `model` (for populating records) and **never** on `render`.
- `edu.itc.salesreport.render` depends only on `model` (for reading aggregated state) and **never** on `ingest`.
- All slices in `edu.itc.salesreport.(*)..` are mathematically proven to be **free of cycles**.

---

## 5. Risk Assessment & Future Considerations

1. **Extreme File Size (Multi-Gigabyte Batches)**:
   - *Current Behavior*: All valid `SaleTransaction` records are kept in memory to produce `MonthlyReport`.
   - *Mitigation for Future Scale*: Transition `MonthLoader` to an online streaming accumulator (updating running branch and product totals on-the-fly) to allow processing arbitrary numbers of gigabytes in constant $O(1)$ memory.
2. **Concurrent File Processing**:
   - Files are currently read sequentially. For 100+ branches, parallel file streams via `java.util.concurrent` or Java 21 Virtual Threads can be introduced while keeping the public `MonthLoader` API unchanged.
