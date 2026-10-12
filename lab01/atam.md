# Architecture Tradeoff Analysis Method (ATAM) Report
**System:** Sales Reporting System (`edu.itc.salesreport`)  
**Scope:** Lab-01 Architecture Assessment & Tradeoff Analysis

---

## 1. Business Drivers
1. **Accurate Financial Aggregation**: Multi-branch sales data must be synthesized accurately without dropping valid receipts or suffering precision loss (use of `BigDecimal` for currency and discounts).
2. **High Fault Tolerance**: Unsynchronized or corrupt records from retail store POS terminals must not disrupt reporting workflows or crash the batch ingestion pipeline.
3. **Evolvability**: The business frequently requests new export formats (e.g. executive dashboards, BI CSV feeds, HTML summaries) and may expand to non-CSV inputs in future quarters.
4. **Fast Operational Turnaround**: Processing monthly data must complete in near real-time on commodity analyst workstations without heavy database infrastructure.

---

## 2. Quality Attribute Utility Tree

| Quality Attribute | Attribute Refinement | Scenario | Importance to Business | Architectural Difficulty |
|---|---|---|:---:|:---:|
| **Fault Tolerance** | Malformed row recovery | S1: Ingest CSV containing invalid types; skip bad row, report diagnostics, finish remaining batch. | **High** | Medium |
| **Modifiability** | Plug-in rendering format | S2: Add HTML/CSV/JSON report renderer without modifying ingest or model code. | **High** | Low |
| **Modifiability** | Parser strategy swapping | S3: Switch input from CSV to JSON/TSV without modifying `MonthLoader`. | Medium | Low |
| **Testability** | Conformance enforcement | S4: ArchUnit automated architecture rule tests fail build on layer leakage or cycles. | **High** | Low |
| **Performance** | Low-latency batch ingestion | S5: Process 10,000+ transaction lines under 1 second with < 512MB heap. | Medium | Medium |
| **Observability** | Non-invasive progress monitoring | S6: Broadcast load events to console, loggers, or test harnesses via Observer pattern. | Medium | Low |

---

## 3. Architectural Approaches & Tactics

### Tactic 1: Strategy Pattern for Parsers and Renderers
* **Approach**: `TransactionParser` and `ReportRenderer` interfaces isolate data translation from core domain logic.
* **Impact**:
  - Eliminates tight coupling between file formats and business entities.
  - OCP-compliant: new formats are purely additive classes.

### Tactic 2: Exception-Based Fault Isolation with Event Notifications
* **Approach**: Parsers throw specialized `InvalidRowException` carrying contextual line metadata. The loader catches exceptions at the line boundary, constructs `LoadEvent.rowInvalid()`, and broadcasts it to registered listeners.
* **Impact**:
  - Prevents single-line failures from causing cascade batch aborts.
  - Maintains detailed operational audit logs of corrupt inputs.

### Tactic 3: Immutable Domain Records and Centralized Validation
* **Approach**: `SaleTransaction`, `BranchSummary`, `ProductTotal`, and `MonthlyReport` are implemented as Java records with compact constructor invariants.
* **Impact**:
  - Guaranteed thread safety and tamper-proof financial calculations.
  - Zero getter/setter boilerplate.

---

## 4. Tradeoff Points & Sensitivity Analysis

### Tradeoff 1: In-Memory Record Retention vs. Online Streaming Aggregation
* **Sensitivity**: Memory usage vs. Analytical flexibility.
* **Tradeoff Analysis**:
  - *Option A (Chosen)*: Retain parsed `SaleTransaction` list in memory and compute aggregates in `MonthLoader.buildReport()`.
    - *Advantage*: High flexibility to compute complex, arbitrary slice metrics (e.g., top N products, payment breakdown per branch, average transaction values) without multiple file reads.
    - *Disadvantage*: Memory consumption scales with $O(N)$ transactions.
  - *Option B (Alternative)*: Single-pass streaming accumulator (updating branch running totals directly without storing individual line items).
    - *Advantage*: Minimal constant $O(1)$ memory usage.
    - *Disadvantage*: Difficult to support multi-faceted post-hoc queries or multi-pass report analyses without re-reading source files.
* **Resolution**: For target batch sizes (tens of thousands of rows per month), Option A easily stays under 30MB heap while granting maximum flexibility.

### Tradeoff 2: Strict Fail-Fast Validation vs. Best-Effort Coercion
* **Sensitivity**: Data completeness vs. Financial integrity.
* **Tradeoff Analysis**:
  - *Option A (Chosen)*: Strict validation (reject row if quantity is not an integer or payment method is unrecognized).
    - *Advantage*: Protects reporting accuracy; avoids silent data corruption.
    - *Disadvantage*: Slightly reduced transaction count if store clerks entered informal notes.
  - *Option B (Alternative)*: Permissive coercion (default unknown payment methods to `CASH` or set invalid quantity to `1`).
    - *Advantage*: Ingests 100% of rows.
    - *Disadvantage*: Distorts revenue metrics and conceals point-of-sale configuration issues.
* **Resolution**: Strict rejection of invalid rows with explicit diagnostic warnings preserves audit integrity.

---

## 5. Architectural Risks & Non-Risks

* **Non-Risk**: Adding new output formats: The decoupled `ReportRenderer` interface guarantees zero impact on existing code.
* **Non-Risk**: Cyclic dependency regression: `DependencyRulesTest` with ArchUnit guarantees immediate build failure upon any architectural violation.
* **Risk (Managed)**: Scalability beyond 10 million transactions in a single JVM run. Managed via documented migration path to streaming accumulators or partitioning by branch.
