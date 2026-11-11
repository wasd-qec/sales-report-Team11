# Lab-03 Task 5: MVC vs MVVM Architectural Comparison

## 1. Quantitative Comparison

| Metric | MVC (Lab-02, `ReportController`) | MVVM (Lab-03, `ReportViewModel`) | How Measured |
| --- | --- | --- | --- |
| **Lines of presentation code** | 49 (controller) in 107-line viewer | 302 ViewModel + 147 View + 165 ports/commands/jobs | Line count (`Measure-Object`) |
| **Presentation-logic line coverage without toolkit** | 0 % (UI controls require JavaFX toolkit initialization) | 95.87 % (116 of 121 lines covered) | JaCoCo 0.8.15 |
| **`javafx.scene` imports in logic class** | 6 (`ComboBox`, `CheckBox`, `Label`, `TableView`, `TableColumn`, etc.) | 0 (only `javafx.beans.*` and `javafx.collections.*`) | Static analysis (`Select-String`) |
| **Control types referenced by logic class** | 4 (`ComboBox`, `CheckBox`, `Label`, `TableView`) | 0 (pure properties and commands) | Code inspection |
| **Test run time of presentation logic** | N/A (untestable without starting JavaFX Application) | 0.087 s (13 unit tests via Surefire) | Maven Surefire Report |
| **UI frozen during background load** | Yes (worst frame gap: 512 ms) | No (worst frame gap: 47 ms) | Task 4 `FrameGapMonitor` |

---

## 2. Qualitative Trade-offs and Reflection

Adopting the Model-View-ViewModel (MVVM) pattern dramatically improves presentation logic testability by removing direct coupling to JavaFX visual controls (`Node`, `Control`, `Stage`). With MVVM, the entire state machine, input validation, and asynchronous command dispatching can be verified in milliseconds inside standard JUnit tests with test doubles (`ManualJobs`, fixed `Clock`) without starting a GUI environment. Furthermore, bidirectional property bindings and reactive observables allow multiple views (such as the main viewer and dashboard) to stay perfectly synchronized automatically.

However, MVVM introduces architectural complexity: the number of classes, interfaces, and boilerplate wrappers (`ReadOnlyBooleanWrapper`, `DelegateCommand`, port interfaces) increases noticeably. In addition, debugging property binding chains is more subtle than tracking procedural controller code because failures often surface as silent binding omissions rather than clear stack traces. We would still select MVC for lightweight utilities, rapid proof-of-concept prototypes, or simple monolithic tools where UI state is minimal and the overhead of ports and reactive bindings outweighs the benefits of automated unit testing.
