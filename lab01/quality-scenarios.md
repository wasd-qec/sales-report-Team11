# Quality-attribute scenarios: Angkor Mart monthly sales report

## Stakeholders
- Head-office analyst: ...
- Branch manager (PNH, REP, BTB): ...

## Scenarios
| ID | Attribute | Source | Stimulus | Artifact | Environment | Response | Response measure | Rank |
|----|-----------|--------|----------|----------|-------------|----------|------------------|------|
| QA-1 | Performance | Scheduler, 06:00 on day 2 of M+1 | Starts the report for a month of about 3 M rows | Report engine | Normal operation, 8-core server, files complete | Report files written, "ready" event published | <= 60 s wall-clock, median of 5 runs after 2 warm-up runs | (H,H) |
| QA-2 | Scalability | Operations / Business expansion | Onboarding a 4th branch (KPS), adding ~1 M rows/month (+33% data volume) | Ingest pipeline & Report engine | Normal batch run, unchanged 8-core server, standard heap (-Xmx512m) | All 4 branches ingested and aggregated without OutOfMemoryError | Runtime increases by <= 35% compared to 3-branch baseline; heap peaks < 400 MB (median of 5 runs after 2 warm-up runs) | (M,M) |
| QA-3 | Availability | | | | | | | |
| QA-4 | Modifiability | | | | | | | |
| QA-5 | Security | | | | | | | |
| QA-6 | Testability | | | | | | | |

## Rank justifications
- QA-1 (H,H): the report is due in the morning; 3 M rows with BigDecimal ...
- QA-2 (M,M): adding branches expands data volume linearly (+33%); a near-future expansion requiring streaming ingestion rather than buffering all rows in memory.

## Assumptions
- A1: the report server has 8 cores and an SSD (to confirm with the client).
- A2: daily branch CSV files are independent and can be parsed and aggregated concurrently or sequentially without cross-branch contention.

## Utility tree
```mermaid
flowchart LR
  U((Utility)) --> P[Performance]
  U --> S[Scalability]
  P --> QA1["QA-1 (H,H) 3 M rows in 60 s or less"]
  S --> QA2["QA-2 (M,M) 4th branch adds at most 35 % run time"]
```

## Architectural drivers
QA-?, QA-?, QA-?