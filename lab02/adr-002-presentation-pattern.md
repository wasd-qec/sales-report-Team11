# ADR-002: Keep MVP for presentation

## Status

Accepted for Lab-03.

## Context

Task 5 implements the same report screen twice. MVC is compact, but
`ReportView` listens to the model and formats status and row values while also
owning JavaFX widgets. MVP gives the screen a passive `ReportScreen` contract;
`ReportPresenter` owns validation, formatting, and display decisions. The MVP
test verifies the exact status, row, and money formatting without starting a
toolkit.

## Decision

Keep MVP as the presentation pattern for Lab-03. It directly supports the
Lab-01 quality scenarios for adding a new presentation behavior without
changing parsing (QA-4: modifiability) and for deterministic tests without a
display (QA-6: testability). A fake screen tests those messages and values
without a display, and another screen (web or Swing) can implement the same
contract.

## Consequences

MVP costs one interface per screen and some callback boilerplate. In return,
display logic is independently testable and JavaFX remains an adapter. Lab-03
will move toward MVVM, where bindings should replace some of this presenter
plumbing; the MVP boundary remains a useful baseline for that comparison.
