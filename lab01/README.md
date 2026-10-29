# Lab-01: Architecture foundations of the sales report system

Team: Team 11 · Members: repository contributors

## Files produced in this lab
| File | Purpose | Author(s) |
|------|---------|-----------|
| lab01/quality-scenarios.md | QA-1..QA-6, utility tree, drivers | Team 11 |
| lab01/c4-context.mmd, .png | C4 level 1 System Context | Team 11 |
| lab01/c4-container.mmd, .png | C4 level 2 Container diagram (Release 1) | Team 11 |
| lab01/c4-notes.md | C4 architectural notes & QA scenario mapping | Team 11 |
| lab01/design-review.md | Cohesion and coupling review | Team 11 |
| lab01/adr/0001-architecture-style-release-1.md | ADR-0001: Architecture style for release 1 | Team 11 |
| lab01/adr/0002-branch-to-head-office-data-flow.md | ADR-0002: Branch data flow & partial failure policy | Team 11 |
| lab01/atam.md | ATAM comparison of three candidate architectures | Team 11 |
| pom.xml, src/** | sales-report 0.1.0-SNAPSHOT model, parser, tests, renderers, and loader | Team 11 |

## C4 Architecture Notes
See [c4-notes.md](c4-notes.md) for architectural notes, quality scenario linkages, and client questions.

## Inputs reused from earlier labs

None (first lab).

## Change log

| Date | Lab | Change | Commit or tag |
|------|-----|--------|---------------|
| 2026-10-29 | Lab-01 | Completed model/parser verification, renderer and observer implementation, architecture review, and two ADRs | working tree |

## Verification

Run `mvn -q verify` from the repository root. The test report and the step-by-step
record are in [Note/report1.md](../Note/report1.md).
