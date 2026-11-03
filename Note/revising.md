# Lab-02 Revision Checklist & Missing Items

Based on the instructions and rubric in `Note/instruct2.html`, the following items need to be addressed or completed.

## 1. Screenshots
- [ ] `lab02/screenshots/mvc-viewer.png`: Screenshot of the JavaFX MVC viewer window running (`mvn javafx:run`), showing 3,600 rows, 6 categories in the chart, and the status bar.
- [ ] `lab02/screenshots/validation.png`: Screenshot showing an input error/validation message in the JavaFX viewer (e.g. invalid month or the appended broken row reporting 1 rejected row).

## 2. Documentation & Reports
- [ ] `lab02/README.md`:
  - Update the "Files produced" table to include all files created across Tasks 1 through 5 (currently only lists Task 5 files).
  - Update the "Author" column to specify the actual partner names for each file instead of generic "Team 11".
- [ ] `lab02/refactoring.md`:
  - Explicitly record before/after line counts for `FatReportController` and its handler (e.g., handler lines: ~20 down to 1 delegation call).

## 3. Test Suite Discrepancies
- [ ] `UiIndependenceTest.java`:
  - Update the architecture test to assert absence of `import java.awt.` and `import javax.swing.` in addition to `import javafx.`.
- [ ] `ReportPresenterTest.java`:
  - Ensure 2 separate `@Test` methods run (e.g. separating status/row verification and money formatting) to match the expected 2 tests run in the lab instructions.
- [ ] `InputRulesTest.java`:
  - Separate or expand test cases to match the expected 9 test executions (covering trimmed month, 6 invalid inputs, future month message, and branch normalization).

## 4. Git & Submission
- [ ] Merge work via branch `lab-02` and a pull request titled `Lab-02 – <team name>`.
- [ ] Tag the final merged commit with `lab-02`.
- [ ] Ensure git commit history shows contributions from both partners across at least two tasks each.

## 5. Optional Challenge (Extra Credit)
- [ ] `sales-web/`: Sibling Spring Boot 4 / Thymeleaf project proving UI independence of the domain and model (+20 marks).

