# Lab-02 completion report

Date: 2026-10-29  
Repository: `sales-report-Team11`  
Branch: `lab-02`  
Tag: `lab-02`  
Java: JDK 25  
Maven: 3.9

## Summary

Lab-02 was completed using the existing Lab-01 domain model, parser, sample
data conventions, and architecture decisions. The project was upgraded for
JavaFX and implemented in five task commits, with tests run after each task.

The optional Spring MVC challenge was not implemented.

## Setup

Updated [pom.xml](../pom.xml) for Lab-02:

- Changed the project version to `0.2.0-SNAPSHOT`.
- Added JavaFX Controls and FXML dependencies, version `25.0.4`.
- Added the JavaFX Maven plugin.
- Added the configurable `javafx.mainClass` property.
- Preserved the `exec-maven-plugin` so the existing Lab-01 console application
  can still be run.
- Added [SalesDataGenerator.java](../src/main/java/edu/itc/salesreport/ingest/SalesDataGenerator.java)
  for deterministic sample data.

Converted the supplied HTML instructions into
[instruct2.md](instruct2.md) for easier reference.

## Task 1 - Observable model

Commit: `a61fcc5` — `Task 1: observable SalesModel`

Implemented:

- `MonthData`
- `CsvMonthReader`
- `SalesAggregator`
- `DefaultSalesAggregator`
- `SalesEvent`
- `SalesModelListener`
- `SalesModel`
- `ConsoleReportView`
- `TestData`
- `SalesModelTest`

Behavior implemented:

- Reads sorted branch CSV files for a selected month.
- Skips CSV headers.
- Collects invalid rows with file and line information.
- Keeps valid rows only.
- Aggregates revenue, discounts, receipts, categories, payment methods, and
  top products.
- Supports `ALL`, `PNH`, `REP`, and `BTB` branch selection.
- Fires `MonthLoaded` and `BranchSelected` events.
- Does not fire an event when selecting the current branch.
- Preserves the previous model state when loading fails.

Verified sample totals:

```text
ALL revenue: 26721.90
PNH revenue: 8455.90
```

## Task 2 - MVC JavaFX view

Commit: `045ae05` — `Task 2: JavaFX MVC report view`

Implemented:

- Toolkit-free `ErrorDisplay`
- JavaFX `ReportView`
- JavaFX `ReportApp`

The view contains:

- Month input field
- Branch selector
- Transaction table
- Date, receipt, product, category, quantity, revenue, and discount columns
- Revenue-by-category bar chart
- Status label
- Error label

The view observes `SalesModel` and refreshes from model data. Calculations and
file loading remain outside the view.

## Task 3 - Controller and validation

Commit: `1410814` — `Task 3: validate MVC controller inputs`

Implemented:

- `Validation`
- `InputRules`
- `ReportCommand`
- `ReportController`
- MVC view binding

The controller:

- Validates month input using an injected `Clock`.
- Validates and normalizes branch input.
- Converts valid input into sealed commands.
- Loads data through the model.
- Clears errors after successful commands.
- Reports missing folders and I/O failures through `ErrorDisplay`.
- Keeps JavaFX imports out of the controller and validation code.

Added tests for:

- Valid and invalid month input.
- Blank input.
- Incorrect month formats.
- Future months.
- Unknown branches.
- Branch normalization.
- Missing month folders.
- Successful model updates.

## Task 4 - Headless tests and refactoring

Commit: `404a5cf` — `Task 4: headless tests and refactoring`

Implemented:

- `UiIndependenceTest`
- Legacy `FatReportController`
- Refactored legacy controller delegation
- [refactoring.md](../lab02/refactoring.md)

`UiIndependenceTest` checks that the toolkit-free packages do not import:

- `javafx.*`
- `java.awt.*`
- `javax.swing.*`

The legacy controller now delegates to `ReportController` instead of handling
file I/O, parsing, filtering, and calculations directly.

The refactoring report documents eight issues, including:

- Unclosed file streams.
- Missing row validation.
- No monetary rounding.
- Broad exception handling.
- UI widgets used inside business logic.
- Hard-coded data paths.
- Unsafe CSV splitting.
- Mixed responsibilities.

## Task 5 - MVP variant

Commit: `6371388` — `Task 5: add MVP presentation variant`

Implemented:

- `RowItem`
- `ReportScreen`
- `ReportPresenter`
- `FxReportScreen`
- `ReportMvpApp`
- `ReportPresenterTest`
- MVP documentation and comparison
- Presentation-pattern ADR
- Lab-02 README

The presenter:

- Receives model events.
- Converts transactions into display rows.
- Formats money and status text.
- Sends values to a passive screen.
- Can be tested without starting JavaFX.

The MVP test verifies:

```text
PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00
```

It also verifies the formatted product row and money formatting:

```text
1,234.50
```

The MVC and MVP comparison is documented in
[mvc-vs-mvp.md](../lab02/mvc-vs-mvp.md).

The presentation decision is documented in
[adr-002-presentation-pattern.md](../lab02/adr-002-presentation-pattern.md).
MVP was selected for Lab-03 because it keeps display logic testable without a
GUI toolkit and supports future presentation adapters.

## Commit history

| Commit | Description |
|---|---|
| `786edaa` | Prepare Lab-02 JavaFX setup |
| `a61fcc5` | Task 1: observable SalesModel |
| `045ae05` | Task 2: JavaFX MVC report view |
| `1410814` | Task 3: validate MVC controller inputs |
| `404a5cf` | Task 4: headless tests and refactoring |
| `6371388` | Task 5: add MVP presentation variant |

The final task commit was tagged:

```text
lab-02
```

## Verification

The following command was run after completing all tasks:

```powershell
mvn -q verify
```

Result:

```text
BUILD SUCCESS
44 tests
0 failures
0 errors
```

The tests cover:

- Lab-01 parser and model behavior.
- Month loading and aggregation.
- MVC model behavior.
- Input rules.
- MVC controller behavior.
- JavaFX independence.
- MVP presenter behavior.
- Existing renderer behavior.

## Current status

- Lab-02 mandatory tasks: complete.
- Task-specific commits: complete.
- `lab-02` tag: created.
- Final verification: passed.
- Working tree: clean at the time of completion.
- Optional Spring MVC challenge: not implemented.
