# Lab-02: MVC and MVP

## Files produced

| Path | Task | Author |
|---|---|---|
| `src/main/java/edu/itc/salesreport/ui/mvp/RowItem.java` | 5 | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportScreen.java` | 5 | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java` | 5 | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/fx/FxReportScreen.java` | 5 | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportMvpApp.java` | 5 | Team 11 |
| `src/test/java/edu/itc/salesreport/ui/mvp/ReportPresenterTest.java` | 5 | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportView.java` | 5, Discount column | Team 11 |
| `src/test/java/edu/itc/salesreport/UiIndependenceTest.java` | 5 | Team 11 |
| `lab02/mvc-vs-mvp.md` | 5, comparison | Team 11 |
| `lab02/adr-002-presentation-pattern.md` | 5, decision | Team 11 |

## Lab-01 inputs reused

The model, ingestion, aggregation, sample-data conventions, and QA scenarios
from Lab-01 are reused from commit `c3a58e9` (`Complete Lab-01 implementation and
documentation`). No Lab-01 source behavior was removed.

## Change log

| Date | Lab | Change | Commit or tag |
|---|---|---|---|
| 2026-10-29 | Lab-02 Task 5 | Added toolkit-free MVP, JavaFX MVP adapter/app, presenter tests, Discount columns, comparison, and ADR | `lab-02` (created by parent) |

## Verification

Run `mvn test` from the repository root. Both the MVC and MVP applications share
the unchanged model; the MVP application can be launched with
`mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp`.
