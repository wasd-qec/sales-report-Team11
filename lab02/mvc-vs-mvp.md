# MVC versus MVP

Measurements use `Get-Content file | Measure-Object -Line` on this checkout.

| Criterion | MVC (`ReportView` + `ReportController`) | MVP (`FxReportScreen` + `ReportPresenter`) |
|---|---|---|
| Lines of code (view / controller-presenter) | 118 / 49 | 85 / 80 |
| Tests that run without JavaFX | 10 test classes (including model, controller and rules) | 1 dedicated presenter test |
| Where money and status text are formatted | JavaFX view and `BranchSummary` values | toolkit-free `ReportPresenter` |
| Files changed to add a “Discount” column | `ui/fx/ReportView.java` | `ui/mvp/ReportPresenter.java`, `ui/mvp/ReportScreen.java`, `ui/fx/FxReportScreen.java` |
| Does the view hold a model reference? | Yes, for listener updates | No; it only receives values |
| Effort for a second screen (web, Swing) | Reuse model/controller, adapt view | Implement `ReportScreen`; presenter is reused unchanged |

Both variants use the same `SalesModel`, so the comparison changes only the
presentation boundary. MVP moves row, money, and status formatting out of the
toolkit adapter; its additional interface and callback plumbing are the
trade-off for testing that formatting headlessly.
