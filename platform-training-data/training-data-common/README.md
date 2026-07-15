# training-data-common

`training-data-common` is the OJ-neutral training-data library. It provides username/OJ binding contracts, transport-neutral queries, collection orchestration, in-process jobs, scheduling, warehouse refresh and data purge.

It does not contain concrete OJ payloads, ODS writers, HTTP controllers or a runnable application.

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/common/
  app/        account, query, purge and warehouse use cases
  collector/  collection windows, dispatch, jobs and results
  config/     common Spring bean assembly
  domain/oj/  models, criteria, values and repository ports
  infra/oj/   normalized identity and warehouse JDBC adapters
  scheduler/  opt-in collection schedules
  support/    small non-business helpers
  web/        transport DTOs reused by Blog API adapters
src/main/resources/db/migration/
src/test/
```

## Dependency And Layer Rules

- Depends on `common-core`, Spring Context/JDBC/transactions and SLF4J; it does not depend on Spring MVC, Blog API, Codeforces or AtCoder.
- Concrete OJ modules register source collection, ODS purge and warehouse refresh implementations through common ports.
- Common may query shared-shape DWD/DWM/DWS tables by validated OJ name, but cannot parse external payloads or write OJ-specific ODS tables.
- Blog API owns `top.naccl` HTTP adapters and authorization. This module exposes application reports and shared job DTOs only.

## Identity And Collection Invariants

- `username` is the training identity; OJ names are normalized by `OjNames`.
- Applied migration `V034__normalize_oj_handle_accounts.sql` created `training_member` and `oj_handle_binding`. Production repositories use these tables.
- Do not edit V034. The legacy `oj_handle_account` table remains in the schema; dropping it requires a later migration after explicit production verification.
- Blog API purges data for each removed or changed OJ before calling complete-set handle replacement. Unchanged handles retain their cursor; new or changed handles start without one.
- Missing `lastCollectedAt` means full history. Later collections use the configured lookback and advance the cursor only after successful completion.
- One logical collection run opens one OJ batch writer. Each bounded source page is filtered and written immediately under that batch id; the common service retains only per-handle counters and outcomes, not all matched submissions.
- A source failure leaves only that handle's cursor unchanged. A sink/write failure aborts the run before any handle cursor is advanced, so a retry can safely replay idempotent ODS upserts.
- Manual jobs and automatic schedules share one in-process execution coordinator. Collection plus warehouse refresh is exclusive per OJ, while Codeforces and AtCoder may run concurrently.
- Multi-user accepted summaries resolve the handle set once and use one batch repository query. The repository performs `SUM ... GROUP BY handle, difficulty`; application code only applies bucket ordering and unknown-to-unrated folding.
- Jobs are in-memory only. All automatic schedules are disabled unless explicitly enabled.

## Key Entries

| Path | Responsibility |
| --- | --- |
| `config/CommonTrainingDataConfig.java` | Common repository, service, query, dispatcher and job assembly. |
| `app/account/TrainingUserDirectory.java` | Internal username, handle and collection-cursor contract. |
| `app/account/OjHandleAccountService.java` | Binding validation, complete-set replacement and cursor updates. |
| `app/query/OjWarehouseQueryFacade.java` | Validated transport-neutral query facade. |
| `app/query/` | Accepted-summary, submission and first-accepted query use cases and reports. |
| `app/purge/OjStudentDataPurgeService.java` | OJ-specific ODS and warehouse purge dispatch. |
| `app/warehouse/OjWarehouseRefreshService.java` | OJ manifest refresh orchestration. |
| `collector/OjSubmissionCollectionBatchWriter.java` | Bounded chunk sink for one logical OJ collection batch. |
| `collector/` and `scheduler/` | Collection windows, retries, page-at-a-time writes, jobs and opt-in schedules. |
| `domain/oj/model/OjRatingAcceptedSummary.java` | Database aggregate for one handle and normalized difficulty key. |
| `domain/oj/repo/` | Identity, query, purge and refresh repository ports. |
| `infra/oj/repo/query/JdbcOjAcceptedSummaryRepository.java` | SQL-side accepted-count aggregation by handle and difficulty. |
| `infra/oj/repo/` | JDBC implementations for normalized identity and warehouse queries. |
| `web/collector/` | Collection-job request and response DTOs reused by Blog API. |
| `src/main/resources/db/migration/V034__normalize_oj_handle_accounts.sql` | Applied normalized identity migration; never modify in place. |
| `src/test/` | Application, repository, collection and scheduling tests. |

This table is a stable navigation map rather than an exhaustive class list.

## Verification

Run from the repository root:

```bash
mvn clean test
```
