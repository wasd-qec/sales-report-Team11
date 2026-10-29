# Fat controller refactoring

Task 4 keeps the old JavaFX entry point as a compatibility adapter, but moves
validation, file access, parsing, filtering and aggregation into the existing
toolkit-free MVC classes. The adapter now forwards the two user inputs to
`ReportController`; it does not know the CSV format or calculate money.

## Smells and consequences

| Smell in the legacy handler | Consequence | Refactoring |
| --- | --- | --- |
| `onLoad` owns I/O, parsing, filtering and presentation | One change requires editing a large event handler and makes failures hard to isolate | Delegate to `ReportController` and `SalesModel` |
| `Files.list(dir)` is not closed | Directory handles can remain open, especially on Windows | `MonthLoader` uses try-with-resources |
| `split(",")` is used as a CSV parser | Empty trailing fields are dropped and malformed rows can shift columns | `CsvTransactionParser` validates exactly ten fields |
| Every row is trusted | A bad quantity or price aborts the whole load (or produces a wrong total) | Invalid rows are rejected and reported with file/line context |
| `catch (Exception)` displays only `"Error"` | Users and tests cannot distinguish invalid input, missing data and read failures | `InputRules` and `ReportController` provide precise messages |
| Widgets are read inside the file/row loop | UI state is coupled to computation and is repeatedly consulted | Read raw input once, then issue typed commands |
| `"data"` and the money formula are hard-coded in the handler | The handler cannot be reused with another data directory or domain policy | Inject `Path` and use the domain model/aggregator |
| No rounding or domain validation is performed | Negative quantities and invalid discounts can silently corrupt revenue | `SaleTransaction` and the parser enforce domain invariants |

## Before and after

### Before: fat event handler

```text
TextField/ComboBox
        |
        v
onLoad: Path.of + Files.list + readAllLines + split
        |              |              |
        +--> filtering + BigDecimal arithmetic --> Label
```

### After: MVC boundary

```text
JavaFX widgets --> FatReportController (compatibility adapter)
                         |
                         v
                 ReportController
                  /           \
             InputRules      SalesModel
                              /    \
                         MonthLoader  Aggregator
                              |
                         ErrorDisplay
```

The old adapter is intentionally thin: `onLoad` contains two delegation calls.
The model, ingest and engine can therefore be tested without starting JavaFX.

## Broken-row comparison

Using the Task 1 sample data (2026-09, branch `ALL`) with the drinking-water
row containing quantity `-3`:

| Case | Legacy fat controller | MVC after refactoring |
| --- | --- | --- |
| Broken row present | `Revenue: 26715.40` (wrong, no warning) | `USD 26721.90`, 3,600 accepted rows, 1 rejected |
| Input `2026/09` | Generic `Error` | `"'2026/09' is not a month in the form yyyy-MM"` |
| Missing month directory | Generic `Error` | `No sales data found in <path>` |
| Tests without JavaFX | No isolated handler tests | Parser, rules, model and controller tests plus `UiIndependenceTest` |

The broken-row difference is deliberate: the old code subtracts the discount
once from a quantity-times-price total and accepts invalid rows, while the
domain parser rejects invalid transactions before aggregation.
