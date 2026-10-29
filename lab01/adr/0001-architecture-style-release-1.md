# ADR-0001: Use a modular monolith batch for release 1

Status: Accepted  
Date: 2026-10-29 · Deciders: Team 11

## Context

The report must process about 3 million rows by 07:00 on day 2 of the
following month (QA-1), continue producing a useful result when one branch
feed is late (QA-3), and allow a new renderer to be added quickly (QA-4).
The team is small, the first release is one Maven project, and the brief does
not require independent service deployment.

## Decision

We will implement release 1 as a modular monolith batch running in one Java 25
JVM. The `ingest`, `model`, `engine`, and `render` packages are separated by
interfaces and dependency rules, while ingestion and report generation remain
separately invocable jobs.

## Alternatives considered

- **Process pipeline:** Separate ingestion, aggregation, and rendering
  processes would isolate failures and support independent scaling, but adds
  IPC, deployment, operational monitoring, and serialization overhead before
  the team has measured a bottleneck.
- **Event-driven microservices:** Independent services and a broker would
  support late files and future branch growth, but introduces infrastructure,
  distributed consistency, security surfaces, and operational work that makes
  QA-1 and QA-6 harder to verify in release 1.

## Consequences

+ A single deployment and in-process calls keep the critical batch path simple
  and measurable for QA-1.
+ Package interfaces and renderer strategies make the requested output-format
  change local to QA-4.
+ A modular boundary permits later extraction of a slow component without
  changing the domain model.
- One JVM limits independent scaling and failure isolation for QA-2 and QA-3.
- A process-wide failure can interrupt the whole batch unless rejected rows and
  late files are explicitly isolated.
- The team must enforce dependency rules and avoid accidental package coupling.

Revisit when: a normal month exceeds 10 million rows, one component requires
independent deployment, or the measured batch misses QA-1 on the target server.
