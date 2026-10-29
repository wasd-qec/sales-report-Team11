# Design review: release 1 modular boundaries

Date: 2026-10-29  
Reviewers: Team 11

## Cohesion

- `model` contains immutable domain values and money rules only. Its
  responsibilities are strongly related, so cohesion is high.
- `ingest` converts external rows into domain transactions and reports
  rejected rows/progress. It does not render reports or contain UI concerns.
- `render` consumes `MonthlyReport` through `ReportRenderer`; each renderer
  has one output responsibility.
- The composition root wires the parser, loader, renderer, and observer. It
  owns orchestration rather than business rules.

## Coupling

The review uses afferent coupling (`Ca`, incoming dependencies) and efferent
coupling (`Ce`, outgoing dependencies) at the package level:

| Package | Ca | Ce | Interpretation |
|---------|----|----|----------------|
| model | 2 | 0 | Stable domain core; no project-package dependencies |
| ingest | 1 | 1 | Depends on model; independent of rendering |
| render | 0 | 1 | Depends on model; each strategy is replaceable |
| application/composition root | 0 | 1 | Wiring layer depends on ingest |

`model` has the highest stability (`I = Ce / (Ca + Ce) = 0`) and is therefore
appropriate as the dependency target. `ingest` has `I = 1 / (1 + 1) = 0.50`
and `render` has `I = 1 / (0 + 1) = 1.00`; both remain replaceable through
interfaces. The composition root has `I = 1 / (0 + 1) = 1.00`. The
dependency test prevents model from depending on another project package and
prevents ingest from depending on render.

## Findings and actions

1. Keep `TransactionParser`, `ReportRenderer`, and progress listeners as
   interfaces so changes remain local (QA-4).
2. Keep rejected rows as data and emit progress events so a late or malformed
   file does not terminate the batch (QA-3).
3. Keep `BigDecimal` calculations in model values and test rounding at the
   boundary (QA-1).
4. Recalculate the coupling table after adding the engine and web adapter;
   those adapters must not introduce a dependency from model to UI.
