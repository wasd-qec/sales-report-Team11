# Lab-01 completion report

Date: 2026-10-29  
Repository: `sales-report-Team11`  
Java: JDK 25.0.4.1  
Maven: 3.9.16

## What was completed

The remaining Lab-01 work was completed against
[Note/instruct.md](instruct.md):

1. **Task 1 - quality scenarios:** Reworked
   [lab01/quality-scenarios.md](../lab01/quality-scenarios.md) so QA-1 through
   QA-6 have all six scenario parts, numeric measurement methods, rank
   justifications, stakeholders, drivers, and a utility tree.
2. **Task 2 - C4 documentation:** Kept the existing context and container
   sources and PNG exports, and retained the architectural notes in
   [lab01/c4-notes.md](../lab01/c4-notes.md).
3. **Task 3 - model and parser tests:** Expanded
   [CsvTransactionParserTest.java](../src/test/java/edu/itc/salesreport/ingest/CsvTransactionParserTest.java)
   to check every valid field, whitespace trimming, and invalid rows for each
   validation rule. Added rounding and immutable-copy tests to
   [SaleTransactionTest.java](../src/test/java/edu/itc/salesreport/model/SaleTransactionTest.java).
4. **Task 4 - Strategy and Observer:** Added three sealed renderer strategies
   (`TextReportRenderer`, `HtmlReportRenderer`, and `CsvReportRenderer`),
   renderer selection, HTML escaping, and renderer tests. Added the sealed
   progress-event hierarchy, listener subscription/unsubscription,
   `MonthLoader`, console progress output, application composition root, loader
   tests, and dependency-rule tests.
5. **Task 5 - architecture decisions:** Added
   [ADR-0001](../lab01/adr/0001-architecture-style-release-1.md) for the
   modular-monolith decision and
   [ADR-0002](../lab01/adr/0002-branch-to-head-office-data-flow.md) for
   SFTP, partial reports, checksums, and idempotent reruns. Added the
   [design review](../lab01/design-review.md) with cohesion and `Ca`/`Ce`
   coupling calculations.
6. **Optional challenge:** Added
   [lab01/atam.md](../lab01/atam.md), comparing the modular monolith, process
   pipeline, and event-driven candidates with QA-1 arithmetic, Amdahl's law,
   sensitivities, trade-offs, risks, and a recommendation.
7. **Lab record:** Updated
   [lab01/README.md](../lab01/README.md) with the produced-file list, no
   earlier inputs statement, change log, and verification command.

## Files added

### Application code

- `src/main/java/edu/itc/salesreport/App.java`
- `src/main/java/edu/itc/salesreport/ingest/ConsoleProgress.java`
- `src/main/java/edu/itc/salesreport/ingest/LoadEvent.java`
- `src/main/java/edu/itc/salesreport/ingest/LoadListener.java`
- `src/main/java/edu/itc/salesreport/ingest/MonthLoader.java`
- `src/main/java/edu/itc/salesreport/render/ReportRenderer.java`
- `src/main/java/edu/itc/salesreport/render/TextReportRenderer.java`
- `src/main/java/edu/itc/salesreport/render/HtmlReportRenderer.java`
- `src/main/java/edu/itc/salesreport/render/CsvReportRenderer.java`

### Tests

- `src/test/java/edu/itc/salesreport/DependencyRulesTest.java`
- `src/test/java/edu/itc/salesreport/ingest/MonthLoaderTest.java`
- `src/test/java/edu/itc/salesreport/render/TestReports.java`
- `src/test/java/edu/itc/salesreport/render/ReportRendererTest.java`

### Documentation

- `lab01/adr/0001-architecture-style-release-1.md`
- `lab01/adr/0002-branch-to-head-office-data-flow.md`
- `lab01/atam.md`
- `lab01/design-review.md`

## Existing files updated

- `lab01/README.md`
- `lab01/quality-scenarios.md`
- `src/test/java/edu/itc/salesreport/ingest/CsvTransactionParserTest.java`
- `src/test/java/edu/itc/salesreport/model/SaleTransactionTest.java`

The existing Java 25 Maven configuration in
[pom.xml](../pom.xml) was preserved.

## Commands run and results

### Focused parser test

```text
mvn -q -Dtest=CsvTransactionParserTest test
```

Result: passed.

### Full verification

```text
mvn -q verify
```

Result: `BUILD SUCCESS`.

Test counts:

| Test class | Tests | Failures | Errors |
|---|---:|---:|---:|
| `DependencyRulesTest` | 2 | 0 | 0 |
| `CsvTransactionParserTest` | 15 | 0 | 0 |
| `MonthLoaderTest` | 2 | 0 | 0 |
| `SaleTransactionTest` | 2 | 0 | 0 |
| `ReportRendererTest` | 7 | 0 | 0 |
| **Total** | **28** | **0** | **0** |

### Sample application

```text
mvn -q compile exec:java
```

Result:

- 15 CSV files were processed in sorted order.
- 2,998 rows were accepted.
- 2 rows were rejected and retained with file and line numbers:
  - `BTB-2026-09-04.csv:21` - invalid `PAYPAL` payment method.
  - `REP-2026-09-02.csv:11` - non-numeric quantity `two`.
- Progress events were printed for every file and a final completion event.

## Notes for review

- The sample data contains intentional invalid rows, so the two rejections are
  expected behavior, not test failures.
- No source files were reverted or reset.
- The repository was not committed or tagged by this task; the working tree
  contains the changes so they can be reviewed first. If required by the
  course, create the requested `lab-01` commit/tag after peer review.
- Lab-01 establishes the model, parser, renderers, and loader. The full report
  aggregation engine, PDF/Excel integrations, and web authorization boundary
  described as future system containers are not implemented by this lab.
