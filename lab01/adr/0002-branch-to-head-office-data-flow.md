# ADR-0002: Have branches push daily CSV files to the landing zone

Status: Accepted  
Date: 2026-10-29 · Deciders: Team 11

## Context

Each branch produces one CSV file per day and the report is due the next
morning. A branch link can fail, so the data flow must support a partial report
without duplicating transactions when a file is retried (QA-3). The first
release has a shared landing zone and a small operations team.

## Decision

Branches will push `BRANCH-yyyy-MM-dd.csv` over authenticated SFTP to the
head-office landing zone. The ingest job waits until 06:00, retries a missing
file three times with exponential backoff, and publishes a partial report by
07:00 with missing branches clearly marked. A file's branch/date name and
SHA-256 checksum form its identity. Re-uploading the same content is ignored;
re-uploading corrected content replaces the staged version and causes an
idempotent re-run for that branch/date rather than appending duplicate rows.
When a late file arrives, it is validated, merged, and the affected report is
regenerated within five minutes, with an alert recorded for the earlier
partial report.

## Alternatives considered

- **Head office pulls from each branch:** Central scheduling is familiar, but
  it requires inbound connectivity or credentials at every branch and makes
  branch outages harder to distinguish from scheduler failures.
- **One event per receipt through a message broker:** This can stream late
  receipts and improve availability, but adds a broker, ordering/replay
  concerns, and serialization/operations overhead for QA-1 and QA-6.

## Consequences

+ SFTP push works with intermittent branch links and lets a recovered branch
  retry without waiting for a central poll (QA-3).
+ File identity and replacement semantics make reruns auditable and
  idempotent (QA-3).
+ The landing zone preserves the original export for troubleshooting.
- The report can be incomplete at 07:00 and must expose the missing branch
  rather than silently presenting a complete-looking total (QA-3).
- SFTP credentials, key rotation, disk capacity, and checksum tracking require
  operational ownership (QA-5).
- Corrected files cause a later report regeneration, so consumers must handle
  a changed result.

Revisit when: branches need sub-hour freshness, daily files exceed the landing
zone capacity, or operations can reliably run a broker and its replay policy.
