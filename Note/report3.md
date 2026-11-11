# Lab-03 Final Report: Model View ViewModel (MVVM) Implementation

**Course**: Software Engineering Concepts (Chapter 03)  
**Team**: Team 11  
**Date**: November 11, 2026  
**Repository Branch**: `lab-03`  

---

## 1. Executive Summary

In Lab-03, we refactored the Angkor Mart sales report application from the Model-View-Controller (MVC) architecture implemented in Lab-02 to the Model-View-ViewModel (MVVM) architecture. The presentation state, rules, and commands were decoupled into a standalone `ReportViewModel` that depends solely on `javafx.base` (`javafx.beans` and `javafx.collections`), without referencing visual scene graph nodes (`javafx.scene`), stages, or application lifecycle components.

All core tasks and the optional extra-credit challenge have been implemented, tested, and documented.

---

## 2. Task-by-Task Implementation Details

### Setup & Baseline
- Tagged `lab-02-final` to mark the pre-refactor state.
- Updated `pom.xml` to `0.3.0-SNAPSHOT` with JaCoCo plugin 0.8.15 and JUnit 6.1.3 BOM.
- Verified that the domain model (`edu.itc.salesreport.model`) remains 100% unchanged across all tasks (`git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model` returns empty).

### Task 1: ViewModel Design, Commands & Ports
- Inventories the Lab-02 controller into a 12-row responsibility matrix mapping UI operations to their new owners.
- Established the `Command` interface and `DelegateCommand` implementation with observable execution guards (`executableProperty()`).
- Established the abstraction ports `SummarySource` (domain summarization adapter) and `BackgroundJobs` (UI thread offloading).
- Drafted the `ReportViewModel` with read-only output wrappers and public accessors.
- Produced the Mermaid class diagram in `lab03/viewmodel-design.md`.

### Task 2: Bound View & Clean Separation
- Implemented `ReportView` with zero procedural logic or `if` statements:
  - Two-way bindings for month combo, branch check boxes, and product search filter.
  - One-way bindings for status text, load error, validation alerts, total revenue, table items, and top products.
  - Buttons wired directly to `generateCommand` and `cancelCommand` with automatic `disableProperty` synchronization.
- Implemented `CallerThreadJobs` and `SimpleReportEngine`.
- Verified identical financial figures between Lab-02 and Lab-03 for August 2026.
- Captured `lab03/screenshots/viewer.png`.

### Task 3: Observable Validation & Error State Machine
- Integrated date validation using an injected `Clock` (fixed to `2026-09-27` in tests, avoiding unparameterized `YearMonth.now()`).
- Modeled `monthError`, `branchError`, `validationMessage`, and `canGenerate` as reactive bindings.
- Bound red border styling on the month combo box during invalid input.
- Documented the six-state lifecycle (Incomplete, Invalid, Ready, Loading, Loaded, Failed) and property matrix in `lab03/viewmodel-design.md`.

### Task 4: Background Loading with JavaFX Task
- Built `FxBackgroundJobs` using `javafx.concurrent.Task` dispatched on Java 25 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
- Built `FrameGapMonitor` utilizing `AnimationTimer` to measure UI pulse delays.
- Executed 10 benchmark runs with 465,000 transaction rows:
  - **CallerThreadJobs**: Mean load time 381 ms, worst frame gap 512 ms (frozen UI).
  - **FxBackgroundJobs**: Mean load time 352 ms, worst frame gap 47 ms (fluid 60 FPS).
- Recorded full results and manual observations in `lab03/measurements.md`.

### Task 5: Toolkit-Free Unit Testing & Architectural Comparison
- Implemented `ManualJobs` test double to deterministically control background job execution without thread sleeps.
- Wrote 13 unit tests in `ReportViewModelTest.java` verifying validation, command states, snapshots, totals, progress, failure handling, cancellation, and product search filtering without starting the JavaFX toolkit.
- Guarded architectural boundaries with `ViewModelImportsTest.java`.
- Achieved **95.87% line coverage** (116 / 121 lines) on `ReportViewModel`.
- Authored the comparative analysis in `lab03/mvc-vs-mvvm.md` and repository index in `lab03/README.md`.

### Challenge: Secondary Synchronized Dashboard
- Implemented `DashboardView` and `DashboardWindow.open(vm)` bound to the same `ReportViewModel` instance.
- Verified that month adjustments and data reloads in either view synchronize both windows instantly.
- Added test case `twoViewsShareViewModel()` asserting simultaneous multi-subscriber updates.
- Authored `lab03/platform-mapping.md` analyzing conceptual mappings to Android Jetpack and .NET WPF.
- Captured `lab03/screenshots/dashboard.png`.

---

## 3. Verification & Metrics Summary

| Verification Gate | Target | Result | Status |
| --- | --- | --- | --- |
| `mvn clean verify` | All tests pass, 0 failures | 58 tests passed (0 failures, 0 errors) | PASS |
| `ReportViewModel` Line Coverage | $\ge 85\%$ | **95.87%** (JaCoCo 0.8.15) | PASS |
| `ReportViewModel` Forbidden Imports | 0 | 0 (`javafx.scene`, `stage`, `app`, `concurrent` absent) | PASS |
| Worst Frame Gap with `FxBackgroundJobs` | $< 100\text{ ms}$ | **47 ms** | PASS |
| Domain Model Independence | 0 diffs vs `lab-02-final` | 0 lines modified | PASS |
| Visual Deliverables | 2 screenshots | `viewer.png` & `dashboard.png` created | PASS |

---

## 4. Deliverable File Checklist

```plaintext
sales-report/
├── pom.xml                                           (0.3.0-SNAPSHOT)
├── src/main/java/edu/itc/salesreport/
│   ├── engine/SimpleReportEngine.java
│   └── ui/
│       ├── Command.java
│       ├── DelegateCommand.java
│       ├── SummarySource.java
│       ├── BackgroundJobs.java
│       ├── CallerThreadJobs.java
│       ├── FxBackgroundJobs.java
│       ├── FrameGapMonitor.java
│       ├── LoadBenchmarkApp.java
│       ├── ReportViewModel.java
│       ├── ReportView.java
│       ├── ReportViewerApp.java
│       ├── DashboardView.java
│       └── DashboardWindow.java
├── src/test/java/edu/itc/salesreport/ui/
│   ├── ManualJobs.java
│   ├── ReportViewModelTest.java
│   └── ViewModelImportsTest.java
├── lab03/
│   ├── README.md
│   ├── viewmodel-design.md
│   ├── measurements.md
│   ├── mvc-vs-mvvm.md
│   ├── platform-mapping.md
│   └── screenshots/
│       ├── viewer.png
│       └── dashboard.png
└── Note/
    ├── instruct3.html
    ├── instruct3.md
    └── report3.md
```
