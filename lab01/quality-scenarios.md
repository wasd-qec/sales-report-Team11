# Architecture Quality Attribute Scenarios
**System:** Sales Reporting System (`edu.itc.salesreport`)  
**Context:** Lab-01 Software Architecture & Design

Quality scenarios define verifiable, measurable requirements for system quality attributes according to the Software Engineering Institute (SEI) six-part framework:
1. **Source of Stimulus**: The entity initiating the event.
2. **Stimulus**: The condition or request arriving at the system.
3. **Artifact**: The stimulated component or architectural slice.
4. **Environment**: The runtime or development condition.
5. **Response**: The observable architectural reaction.
6. **Response Measure**: The quantitative, testable criteria for success.

---

## Scenario 1: Fault Tolerance & Robustness (Data Ingestion)
*Handling corrupt or unexpected data rows without failing the entire batch.*

| Part | Specification |
|---|---|
| **Source** | External branch POS data export |
| **Stimulus** | A daily CSV file containing malformed lines (e.g., non-numeric quantity `"two"`, unsupported payment method `"PAYPAL"`, or incomplete column counts). |
| **Artifact** | `CsvTransactionParser` and `MonthLoader` (`edu.itc.salesreport.ingest`) |
| **Environment** | Automated batch ingestion running over a month of data files. |
| **Response** | The parser intercepts faulty tokens, wraps them in an `InvalidRowException` with line number, file name, and exact text, and notifies registered `LoadListener` instances. The loader skips only the erroneous row and continues processing remaining records. |
| **Response Measure** | Zero process termination crashes; 100% of valid rows across all files are parsed and aggregated; exactly all bad rows are recorded in the skipped count and reported with line-level diagnostics. |

---

## Scenario 2: Modifiability & Extensibility (Open-Closed Principle)
*Adding a new report output format (e.g., JSON, PDF, or Excel) without altering existing code.*

| Part | Specification |
|---|---|
| **Source** | Software Developer / Architecture Team |
| **Stimulus** | Requirement to deliver a new output format (e.g., JSON or Markdown report export). |
| **Artifact** | `ReportRenderer` interface and `edu.itc.salesreport.render` package |
| **Environment** | Standard maintenance and development cycle. |
| **Response** | The developer implements the `ReportRenderer` interface without modifying `MonthLoader`, `CsvTransactionParser`, or any domain model records. |
| **Response Measure** | Zero modifications required in `edu.itc.salesreport.model` or `edu.itc.salesreport.ingest`; implementation time < 30 person-minutes; 100% regression test pass rate. |

---

## Scenario 3: Modifiability & Portability (Data Source Swapping)
*Switching input format from CSV to JSON or database streams.*

| Part | Specification |
|---|---|
| **Source** | Software Developer / Data Engineering Team |
| **Stimulus** | Requirement to ingest sales records from JSON or TSV instead of standard CSV. |
| **Artifact** | `TransactionParser` interface (`edu.itc.salesreport.ingest`) |
| **Environment** | System extension. |
| **Response** | Implement a new `JsonTransactionParser` conforming to `TransactionParser` and pass it into `MonthLoader` constructor via dependency injection. |
| **Response Measure** | `MonthLoader` and domain models remain 100% untouched; existing unit tests for domain aggregation remain unaffected. |

---

## Scenario 4: Testability & Architecture Conformance
*Enforcing architectural layer isolation and preventing cyclical dependencies automatically in CI/CD.*

| Part | Specification |
|---|---|
| **Source** | Automated CI pipeline / Developer running tests |
| **Stimulus** | A pull request introducing illegal coupling (e.g., a domain model class importing a renderer or an ingest parser). |
| **Artifact** | `DependencyRulesTest` using ArchUnit (`edu.itc.salesreport`) |
| **Environment** | Build phase (`mvn test`). |
| **Response** | ArchUnit scans compiled bytecode against predefined architectural rules, detects violations, and fails the build with a descriptive explanation of the illegal dependency. |
| **Response Measure** | 100% detection of cross-layer violations; zero circular dependencies across packages allowed into main branch. |

---

## Scenario 5: Performance & Scalability (Batch Processing)
*Processing multi-branch monthly data efficiently under standard memory constraints.*

| Part | Specification |
|---|---|
| **Source** | End user or scheduled reporting cron job |
| **Stimulus** | Ingestion of 15+ CSV files totaling 10,000+ transaction lines for a full business month. |
| **Artifact** | `MonthLoader` and file streaming pipelines |
| **Environment** | Standard workstation or container (JVM heap <= 512 MB). |
| **Response** | Files are read sequentially using buffered streams (`BufferedReader`); data structures aggregate statistics in-memory using linear $O(N)$ passes. |
| **Response Measure** | End-to-end processing and report generation completed in < 1.0 second; memory footprint remains stable with zero OutOfMemoryErrors. |

---

## Scenario 6: Observability (Decoupled User Feedback)
*Providing real-time ingestion status and progress reporting without coupling business logic to terminal I/O.*

| Part | Specification |
|---|---|
| **Source** | Ingestion pipeline runtime |
| **Stimulus** | `MonthLoader` transitions between file loading, row parsing, error detection, and completion phases. |
| **Artifact** | `LoadEvent`, `LoadListener`, and `ConsoleProgress` |
| **Environment** | Interactive CLI execution or headless background execution. |
| **Response** | `MonthLoader` broadcasts immutable `LoadEvent` instances to registered `LoadListener`s. `ConsoleProgress` formats and prints terminal feedback. In headless mode, alternative listeners can log to SLF4J or metrics collectors without altering loader code. |
| **Response Measure** | Ingestion engine has zero direct dependencies on `System.out` or terminal formatting libraries; multiple observers can attach concurrently. |
