# Lab-01: Sales Report Architecture & Implementation
**Course:** Software Architecture & Engineering  
**Institution:** Institute of Technology of Cambodia (ITC)  
**Team:** Team 11 (`wasd-qec/sales-report-Team11`)

---

## 1. Overview
This project implements a decoupled, robust sales report pipeline for multi-branch retail operations (Phnom Penh `PNH`, Siem Reap `REP`, Battambang `BTB`). The system processes batch sales CSV files, gracefully tolerates and reports corrupted records, aggregates metrics into an immutable domain model, and renders results into multiple presentation formats (ASCII Console, HTML, and CSV).

---

## 2. Architecture & Design Highlights

* **Layered & Decoupled Architecture**:
  * `edu.itc.salesreport.model`: Core immutable domain models (`SaleTransaction`, `BranchSummary`, `ProductTotal`, `MonthlyReport`) and enumerations (`PaymentMethod`). Has **zero** external package dependencies.
  * `edu.itc.salesreport.ingest`: File discovery, line-by-line parsing strategies (`TransactionParser`, `CsvTransactionParser`), and lifecycle event streaming (`MonthLoader`, `LoadEvent`, `LoadListener`, `ConsoleProgress`).
  * `edu.itc.salesreport.render`: Presentation polymorphism (`ReportRenderer`, `TextReportRenderer`, `HtmlReportRenderer`, `CsvReportRenderer`).
  * `edu.itc.salesreport`: Application entrypoint (`App`).

* **Design Patterns**:
  * **Strategy Pattern**: `TransactionParser` (for format parsing) and `ReportRenderer` (for presentation formats).
  * **Observer Pattern**: `LoadListener` and `LoadEvent` decouples ingestion progress from terminal output.
  * **Immutability & Value Objects**: Java `record` types ensure thread-safe, tamper-proof calculations.

* **Automated Architecture Conformance**:
  * `DependencyRulesTest` uses **ArchUnit** to verify strict package boundaries and guarantee zero circular dependencies on every build.

---

## 3. Directory Layout

```
sales-report-Team11/
├── .gitignore                      target/ and data/
├── pom.xml                         Maven configuration with JUnit 5 & ArchUnit
├── tools/
│   └── SampleData.java             Sample data generator with intentional bad rows
├── src/main/java/edu/itc/salesreport/
│   ├── App.java                    Application CLI entrypoint
│   ├── model/
│   │   ├── PaymentMethod.java      Enum: CASH, CARD, KHQR
│   │   ├── SaleTransaction.java    Record representing individual line item
│   │   ├── ProductTotal.java       Record aggregating product totals
│   │   ├── BranchSummary.java      Record aggregating branch metrics
│   │   └── MonthlyReport.java      Record aggregating monthly top-level stats
│   ├── ingest/
│   │   ├── TransactionParser.java  Parser strategy interface
│   │   ├── InvalidRowException.java Exception capturing row diagnostics
│   │   ├── CsvTransactionParser.java CSV row parser and validator
│   │   ├── LoadEvent.java          Ingestion lifecycle event
│   │   ├── LoadListener.java       Observer callback interface
│   │   ├── MonthLoader.java        Batch loader and metrics aggregator
│   │   └── ConsoleProgress.java    CLI progress and warning printer
│   └── render/
│       ├── ReportRenderer.java     Report rendering strategy interface
│       ├── TextReportRenderer.java ASCII / console table renderer
│       ├── HtmlReportRenderer.java Responsive modern HTML report renderer
│       └── CsvReportRenderer.java  Structured CSV summary renderer
├── src/test/java/edu/itc/salesreport/
│   ├── DependencyRulesTest.java    ArchUnit architecture rule tests
│   ├── model/
│   │   └── SaleTransactionTest.java Unit tests for domain calculations
│   ├── ingest/
│   │   ├── CsvTransactionParserTest.java Tests for valid & invalid rows
│   │   └── MonthLoaderTest.java    Tests for multi-file loading & events
│   └── render/
│       ├── TestReports.java        Test data fixture
│       └── ReportRendererTest.java Tests for Text, HTML, and CSV renderers
└── lab01/
    ├── README.md                   This guide
    ├── quality-scenarios.md        Six-part SEI quality scenarios
    ├── c4-context.mmd              C4 System Context diagram (Mermaid)
    ├── c4-container.mmd            C4 Container diagram (Mermaid)
    ├── c4-component.mmd            C4 Component diagram (Mermaid)
    ├── design-review.md            Design review against SOLID and quality attributes
    ├── adr/
    │   ├── 0001-immutable-records-for-domain-models.md
    │   └── 0002-observer-pattern-for-ingestion-events.md
    └── atam.md                     Architecture Tradeoff Analysis Method report
```

---

## 4. How to Build & Run

### 4.1 Generate Sample Data
To generate monthly test data with intentionally bad rows:
```bash
java tools/SampleData.java 2026-09 5 200
```
This writes 15 CSV files (3,000 total rows) to `data/2026-09/` with 2 deliberately bad rows:
- `REP-2026-09-02.csv`: non-numeric quantity `"two"`
- `BTB-2026-09-04.csv`: invalid payment method `"PAYPAL"`

### 4.2 Run All Tests (Including ArchUnit Architecture Guardrails)
```bash
mvn clean test
```
All 18 unit and architecture tests will execute and pass.

### 4.3 Run the Application
Generate console ASCII report:
```bash
mvn compile exec:java
```

Or run directly with Java:
```bash
java -cp target/classes edu.itc.salesreport.App 2026-09 all
```
Output formats supported: `text`, `html`, `csv`, or `all`.
