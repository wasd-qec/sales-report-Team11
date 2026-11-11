# Lab-03: Model View ViewModel (MVVM) Design Pattern

## 1. Files Produced in Lab-03

| Path | Task | Purpose | Author |
| --- | --- | --- | --- |
| `src/main/java/edu/itc/salesreport/ui/Command.java` | Task 1 | Interface for observable executable actions | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/DelegateCommand.java` | Task 1 | Guarded command implementation delegating to `Runnable` | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/SummarySource.java` | Task 1 | Port decoupling ViewModel from domain data aggregation | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/BackgroundJobs.java` | Task 1 | Port for offloading tasks to background threads | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/ReportViewModel.java` | Tasks 1–4 | Presentation logic, state properties, validation and commands | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/CallerThreadJobs.java` | Task 2 | Synchronous BackgroundJobs runner (UI freeze baseline) | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/ReportView.java` | Task 2 | JavaFX view containing only controls and bindings | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/ReportViewerApp.java` | Task 2 | Main composition root configuring dependencies | Team 11 |
| `src/main/java/edu/itc/salesreport/engine/SimpleReportEngine.java` | Task 2 | Sequential engine implementation for summary generation | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/FxBackgroundJobs.java` | Task 4 | BackgroundJobs running `javafx.concurrent.Task` on virtual threads | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/FrameGapMonitor.java` | Task 4 | AnimationTimer pulse monitor measuring UI frame gaps | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/LoadBenchmarkApp.java` | Task 4 | Harness measuring load times and frame gaps across modes | Team 11 |
| `src/test/java/edu/itc/salesreport/ui/ManualJobs.java` | Task 5 | Deterministic test double for background jobs queue | Team 11 |
| `src/test/java/edu/itc/salesreport/ui/ReportViewModelTest.java` | Task 5 | 13 toolkit-free unit tests for validation, commands, state | Team 11 |
| `src/test/java/edu/itc/salesreport/ui/ViewModelImportsTest.java` | Task 5 | Architecture test guarding ViewModel imports | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/DashboardView.java` | Challenge | Compact secondary view bound to the same ViewModel | Team 11 |
| `src/main/java/edu/itc/salesreport/ui/DashboardWindow.java` | Challenge | Secondary stage window wiring for the dashboard | Team 11 |
| `lab03/viewmodel-design.md` | Tasks 1, 3 | Controller inventory, state machine, and class diagram | Team 11 |
| `lab03/measurements.md` | Task 4 | 10 benchmark runs, mean load times, and frame gaps | Team 11 |
| `lab03/mvc-vs-mvvm.md` | Task 5 | Quantitative metric comparison between MVC and MVVM | Team 11 |
| `lab03/platform-mapping.md` | Challenge | Cross-platform architectural mapping to Android & WPF | Team 11 |
| `lab03/screenshots/viewer.png` | Task 2 | Screenshot of loaded MVVM report viewer | Team 11 |
| `lab03/screenshots/dashboard.png` | Challenge | Screenshot of synchronized dashboard window | Team 11 |

---

## 2. Inputs Reused from Lab-02

The domain model, parser records, and dataset generation conventions are reused directly from Lab-02 tagged commit `lab-02-final`:
- `edu.itc.salesreport.model` records (`SaleTransaction`, `BranchSummary`, `MonthlyReport`, `ProductTotal`, `PaymentMethod`).
- Model package was kept strictly unchanged (`git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model` produces no output).
- Ingestion and data generators (`SalesDataGenerator`, `CsvTransactionParser`) were adapted for large benchmark measurements.

---

## 3. Change Log

| Date | Lab Task | What Changed | Author | Model Changed? |
| --- | --- | --- | --- | --- |
| 2026-11-11 | Setup & Task 1 | Updated `pom.xml`, created `Command`, `DelegateCommand`, `SummarySource`, `BackgroundJobs`, and `ReportViewModel` skeleton; documented inventory in `viewmodel-design.md`. | Team 11 | No |
| 2026-11-11 | Task 2 | Implemented `ReportView` with two-way and one-way bindings, `CallerThreadJobs`, `SimpleReportEngine`, and `ReportViewerApp`. Captured `viewer.png`. | Team 11 | No |
| 2026-11-11 | Task 3 | Added validation rules using injected `Clock`, joined error messages, red input border binding, and documented state diagram. | Team 11 | No |
| 2026-11-11 | Task 4 | Implemented `FxBackgroundJobs`, `FrameGapMonitor`, and `LoadBenchmarkApp`. Measured 10 runs in `measurements.md` proving fluid 60 FPS. | Team 11 | No |
| 2026-11-11 | Task 5 | Created `ManualJobs`, 13 `ReportViewModelTest` unit tests (95.87% coverage), `ViewModelImportsTest`, and `mvc-vs-mvvm.md`. | Team 11 | No |
| 2026-11-11 | Challenge | Created `DashboardView`, `DashboardWindow`, multi-view synchronization test, and `platform-mapping.md`. Captured `dashboard.png`. | Team 11 | No |

---

## 4. Verification

Run the test suite and JaCoCo coverage from the repository root:
```bash
mvn clean verify
```
Launch the MVVM report viewer application:
```bash
mvn javafx:run
```
Launch the UI responsiveness benchmark harness:
```bash
mvn javafx:run -Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp -Djavafx.args="--data=data --month=2026-08 --warmup=2 --runs=5"
```

Pull Request: [Lab-03 – Team 11](https://github.com/itc-salesreport/sales-report-Team11/pull/3)
