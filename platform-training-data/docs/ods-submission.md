# Submission Warehouse Tables

This document is the agent-readable contract for current Codeforces submission warehouse storage.

## Scope

The training-data module currently keeps independent OJ data warehouse domains and OJ-specific ODS batch-upsert entrypoints.

Implemented Codeforces tables:

- `ods_codeforces__submission`
- `dwd_codeforces__submission`
- `dwm_codeforces__handle_problem_first_accepted`
- `dws_codeforces__handle_daily_rating_accepted_summary`

Each implemented OJ is a vertical Maven module and owns its own HTTP ingress, ingest application service, recent-lookback source collection, collect batch type, record, parser, writer, fixture, DDL, ODS upsert SQL, DWD/DWM/DWS SQL task resources, SQL task manifest, Spring config, and tests:

- `training-data-codeforces`

Do not reintroduce a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, `SourcePlatform`, or shared collect batch to flatten different OJ submission shapes.

External collectors should post raw submission arrays to the OJ-specific HTTP ingest endpoint with a platform JWT that has the `admin` role. The OJ module creates its own `batch_id`, `fetched_at`, `raw_payload`, and `payload_hash`, then writes through its own writer.

Codeforces also exposes an admin recent-lookback collection endpoint that calls the public Codeforces `user.status` API for a platform `studentIdentity` and writes matching submissions through the same ODS ingest path. The endpoint accepts `studentIdentity` and `lookbackHours`; the service resolves the identity to its bound Codeforces handle, uses its current execution instant as the right boundary, and collects submissions from that instant back by the requested number of hours. Browser-driven batch collection uses an in-process job service that runs multiple identities sequentially and exposes job list/detail snapshots for polling; it is not persisted across backend restarts and must not become a general pipeline run-state table. Scheduled collection reads handles whose `codeforces_handle_account.need_collect` flag is true through the handle-account service and de-duplicates them by handle. Each Codeforces page request uses bounded connect/read timeouts and retries before the current handle is reported as failed.

Codeforces exposes an admin student-data purge endpoint for high-risk account cleanup. It accepts a platform `studentIdentity`, resolves the current Codeforces handle binding, deletes that handle's DWS, DWM, DWD, and ODS rows, and then removes the `codeforces_handle_account` row. Missing handle bindings return zero deletion counts. Auth account deletion remains owned by `auth-web`; callers that need full user deletion should clear training data first and then call the auth admin delete endpoint.

Codeforces DWD/DWM/DWS transforms are idempotent SQL task resources. The current Java execution path is an admin-triggered synchronous refresh endpoint backed by the shared SQL task DAG executor; persistent task run state and ADS physical tables are not implemented yet. The Codeforces collector has a disabled-by-default Spring scheduled trigger that calls the same recent-lookback collection use case; it is not a persistent pipeline scheduler. After collector ODS write, the code intentionally leaves a TODO where the future scheduler/orchestrator call should be added.

Codeforces read-side Java repositories can query existing warehouse tables directly when the target table already matches the query grain. Current DWD/DWM/DWS query support is also exposed through guest HTTP query endpoints.

## Codeforces Java Package Layout

Codeforces is still one vertical OJ Maven module. The Java packages are split by responsibility inside that module:

```text
codeforces/
  app/
    account/     # Codeforces handle-account use cases and app errors
    collector/   # recent-lookback submission collection use case, collection jobs, and collection results
    ingest/      # ODS ingest use case and ingest results
    purge/       # student-scoped Codeforces data deletion use case
    query/       # DWD/DWM/DWS read use cases and query results
    warehouse/   # admin SQL task refresh use case
  collector/
    config/      # typed Codeforces source and schedule properties
  config/        # Spring Bean wiring for Codeforces services, ports, adapters, and shared SQL runner
  domain/
    collector/   # external Codeforces source client port
    criteria/    # repository query criteria records and default filter helpers
    model/       # ODS/DWD/DWM/DWS records and read models
    parser/      # parser ports consumed by application services
    repo/        # repository/writer interfaces owned by the domain boundary
    value/       # reusable constants such as fixed rating buckets
  infra/
    collector/   # external Codeforces HTTP clients
    parser/      # external Codeforces payload parsing into OJ-owned records
    repo/        # JDBC implementations and SQL-facing row mapping
  scheduler/     # Spring scheduled adapters that invoke app use cases
  web/
    account/     # handle-account controller, requests, responses, and error mapping
    collector/   # submission-collection controller, requests, responses, and error mapping
    ingest/      # ODS ingest controller and response DTOs
    purge/       # admin student-data purge controller and response DTOs
    query/       # public warehouse query controller and response DTOs
    warehouse/   # warehouse refresh controller, request DTO, and error mapping
```

Tests mirror the same feature-oriented package layout under `src/test/java`.

## Warehouse Time Zone

Training-data warehouse tables use UTC+8 (`Asia/Shanghai`) as the canonical local time zone for stored `datetime` and date-grain fields.

Codeforces source `creationTimeSeconds` is an epoch second. DWD converts it to `submitted_at_utc_plus8` by adding eight hours to the UTC epoch timestamp, then derives `submitted_date_utc_plus8` from that UTC+8 local timestamp. DWM and DWS inherit the same UTC+8 day boundary. Java read-side query objects use `LocalDateTime` / `LocalDate` values that are already in UTC+8.

ODS `fetched_at` is also written as a UTC+8 local `datetime`. Runtime MySQL configuration should keep `serverTimezone=Asia/Shanghai`.

## Source Access Notes

Codeforces default test data comes from a local fixture shaped like the public `user.status` API:

```text
https://codeforces.com/api/user.status?handle=tourist&from=1&count=2
```

The reusable local chain-test fixture is `training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json`. It contains 1000 unique submissions captured once from multiple `user.status` requests on `2026-07-03`, with submission times spanning `2022-10-15T14:36:48Z` through `2026-07-03T02:41:35Z`.

Fixture metadata is stored next to the data in `submissions_multi_user_1000.metadata.json`. See [test-data.md](test-data.md) for the source URLs and local API replay command. Do not make default tests or local chain checks refresh this data from Codeforces.

The internal collector uses `user.status` with `handle`, 1-based `from`, and `count` pagination. Codeforces returns newest submissions first and does not accept a time-window parameter, so the collector pages per handle, filters by `creationTimeSeconds` against the service-computed `[now - lookback, now)` window, and stops when a returned page is smaller than the configured page size or when a full page is older than the requested window. Default tests use fake clients or local HTTP servers; they must not call live Codeforces.

## Codeforces ODS Fields

`ods_codeforces__submission` preserves Codeforces `Submission` / `user.status` semantics.

| Column | Source field | Required for ODS |
| --- | --- | --- |
| `codeforces_submission_id` | `id` | Yes |
| `contest_id` | `contestId` | No |
| `creation_time_seconds` | `creationTimeSeconds` | No |
| `relative_time_seconds` | `relativeTimeSeconds` | No |
| `problem_contest_id` | `problem.contestId` | No |
| `problem_index` | `problem.index` | No |
| `problem_name` | `problem.name` | No |
| `problem_type` | `problem.type` | No |
| `problem_points` | `problem.points` | No |
| `problem_rating` | `problem.rating` | No |
| `problem_tags_json` | `problem.tags` raw JSON | No |
| `author_handle` | first `author.members[].handle` | Yes |
| `author_participant_type` | `author.participantType` | No |
| `author_json` | `author` raw JSON | No |
| `programming_language` | `programmingLanguage` | No |
| `verdict` | `verdict` | No |
| `testset` | `testset` | No |
| `passed_test_count` | `passedTestCount` | No |
| `time_consumed_millis` | `timeConsumedMillis` | No |
| `memory_consumed_bytes` | `memoryConsumedBytes` | No |
| `batch_id` | collect batch id | Yes |
| `fetched_at` | collect time stored as UTC+8 local `datetime` | Yes |
| `raw_payload` | raw source item JSON | Yes |
| `payload_hash` | SHA-256 of `raw_payload` | Yes |

The unique key is `codeforces_submission_id`.

## Codeforces DWD Submission

`dwd_codeforces__submission` is the cleaned single-submission detail table derived from `ods_codeforces__submission`.

Grain:

```text
one Codeforces submission
```

Primary key:

```text
id
```

Business unique key:

```text
codeforces_submission_id
```

Important derived fields:

| Column | Rule |
| --- | --- |
| `ods_submission_id` | ODS row `id` |
| `submitted_at_utc_plus8` | `creation_time_seconds` added to UTC epoch, then shifted to UTC+8 local `datetime` |
| `submitted_date_utc_plus8` | UTC+8 local date from `submitted_at_utc_plus8` |
| `problem_key` | `problem_contest_id + ':' + problem_index`; null when either part is missing |
| `is_accepted` | `verdict = 'OK'` |
| `ods_batch_id` | ODS `batch_id` |
| `ods_fetched_at` | ODS `fetched_at` |
| `ods_payload_hash` | ODS `payload_hash` |

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwd/upsert_dwd_codeforces__submission.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/submissions/by-student
GET /api/training-data/codeforces/submissions/by-problem
 -> CodeforcesWarehouseQueryController
 -> CodeforcesSubmissionQueryService
 -> CodeforcesSubmissionRepository
 -> JdbcCodeforcesSubmissionRepository
 -> dwd_codeforces__submission
```

At the app layer, personal DWD reads accept platform `studentIdentity`; the service resolves it through `codeforces_handle_account` and then builds repository handle criteria. The DWD repository query model supports two atomic reads:

- by requested `authorHandle`, optional inclusive UTC+8 submitted time range, optional problem rating lower/upper bounds, and backend pagination `limit/offset`;
- by requested `problemKey`, optional inclusive UTC+8 submitted time range, across all handles, and backend pagination `limit/offset`.

Null time bounds mean no lower or upper time limit. Null problem rating bounds mean all rated and unrated rows. Setting either problem rating bound filters to the requested inclusive rating interval and excludes unrated rows.
The public HTTP submission detail endpoints accept `page` and `limit`; `page` is 1-based, `limit` defaults to `100`, and `limit` is capped at `2000`. Each response returns exact `total`, `totalPages`, `hasMore`, and the requested page's submission rows.

The repository returns DWD atomic submission rows. The app service returns report records that keep matching submission details:

- personal query: requested `studentIdentity`, resolved handle, and matching submission detail items with `studentIdentity + authorHandle`;
- problem query: requested problem key and matching submission detail items across handles with `studentIdentity + authorHandle`; unbound result handles fail the app query.

Time and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## Codeforces DWM First Accepted

`dwm_codeforces__handle_problem_first_accepted` records the first accepted submission for each Codeforces handle and problem. It is DWM because it is a reusable intermediate fact derived from DWD, not the final topic summary.

Grain:

```text
one author_handle + problem_key
```

Primary key:

```text
id
```

Business unique key:

```text
author_handle + problem_key
```

Source rule:

```text
dwd_codeforces__submission
where is_accepted = 1
  and problem_key is not null
  and problem_contest_id is not null
  and problem_index is not null
  and submitted_at_utc_plus8 is not null
  and submitted_date_utc_plus8 is not null
```

Tie-break rule:

```text
earliest submitted_at_utc_plus8, then smallest codeforces_submission_id
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/first-accepted/by-student
GET /api/training-data/codeforces/first-accepted/by-problem
 -> CodeforcesWarehouseQueryController
 -> CodeforcesFirstAcceptedProblemQueryService
 -> CodeforcesFirstAcceptedProblemRepository
 -> JdbcCodeforcesFirstAcceptedProblemRepository
 -> dwm_codeforces__handle_problem_first_accepted
```

At the app layer, personal DWM reads accept platform `studentIdentity`; the service resolves it through `codeforces_handle_account` and then builds repository handle criteria. The DWM repository query model supports two atomic reads:

- by requested `authorHandle`, optional inclusive UTC+8 first-accepted time range, and optional problem rating lower/upper bounds;
- by requested `problemKey`, plus optional inclusive UTC+8 first-accepted time range, across all handles.

Null time bounds mean no lower or upper time limit. Null problem rating bounds mean all rated and unrated rows. Setting either problem rating bound filters to the requested inclusive rating interval and excludes unrated rows.

The repository returns DWM atomic first-accepted rows. The app service returns report records that keep the current query's required detail list:

- personal query: requested `studentIdentity`, resolved handle, accepted problem total, and first-accepted problem detail items;
- problem query: requested problem key, accepted handle count, and accepted `studentIdentity + authorHandle` list with each handle's first accepted UTC+8 time; unbound result handles fail the app query.

Time and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## Codeforces DWS Daily Rating Summary

`dws_codeforces__handle_daily_rating_accepted_summary` summarizes first accepted problems by handle and UTC+8 local date. Each row is a wide daily summary with one count column for each fixed Codeforces problem rating bucket and one count column for unrated problems.

Grain:

```text
one author_handle + accepted_date_utc_plus8
```

Primary key:

```text
id
```

Business unique key:

```text
author_handle + accepted_date_utc_plus8
```

Rating columns:

```text
rating_800_accepted_problem_count
rating_900_accepted_problem_count
...
rating_3500_accepted_problem_count
unrated_accepted_problem_count
```

Source rule:

```text
dwm_codeforces__handle_problem_first_accepted
group by author_handle, first_accepted_date_utc_plus8

For each grouped DWM row:
problem_rating = 800 -> rating_800_accepted_problem_count
problem_rating = 900 -> rating_900_accepted_problem_count
...
problem_rating = 3500 -> rating_3500_accepted_problem_count
problem_rating is null -> unrated_accepted_problem_count
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/accepted-summary
GET /api/training-data/codeforces/accepted-summary/auto-collect-users
 -> CodeforcesWarehouseQueryController
 -> CodeforcesAcceptedSummaryQueryService
 -> CodeforcesAcceptedSummaryRepository
 -> JdbcCodeforcesAcceptedSummaryRepository
 -> dws_codeforces__handle_daily_rating_accepted_summary
```

At the app layer, single-student DWS reads accept platform `studentIdentity`; the service resolves it through `codeforces_handle_account` and then builds repository handle criteria. The automatic-collection list path reads all handle accounts with `need_collect=true`, reuses the same per-account DWS aggregation, and sorts the reports by total accepted problem count descending. The repository query model supports:

- requested `authorHandle`;
- optional inclusive UTC+8 date range;
- optional problem rating lower/upper bounds. DWS includes unrated only when both problem rating bounds are absent.

The repository returns DWS atomic rows at the table grain:

```text
author_handle + accepted_date_utc_plus8
```

The app service returns aggregated reports with:

- the requested `studentIdentity` and resolved handle;
- interval-level rating totals derived from the wide rating count columns;
- total accepted problem count for the requested interval.

Date and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## SQL Task Order

Run the SQL tasks in this order. The refresh service passes `batchId` plus the effective UTC+8 refresh interval as `refreshFromDateUtcPlus8` and `refreshToDateUtcPlus8`:

```text
sql/dwd/upsert_dwd_codeforces__submission.sql
sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

The executable manifest is:

```text
training-data-codeforces/src/main/resources/sql/tasks/codeforces-warehouse-refresh.yml
```

The shared SQL task runner reads this manifest on every refresh request, rebuilds the adjacency-list graph, checks that it is a DAG, and then executes the topological plan.

Each task is designed to be repeatable for the effective UTC+8 date interval. The initial interval is the inclusive date range derived from the batch's ODS `creation_time_seconds` values after converting them to UTC+8 local dates, so it covers the full min/max submission time span. If the `batchId` has no ODS rows with `creation_time_seconds`, the refresh request is rejected before SQL task execution instead of running a no-op refresh with an empty interval. Before inserting, DWD deletes rows whose `submitted_date_utc_plus8` is inside that interval, DWM deletes rows whose `first_accepted_date_utc_plus8` is inside that interval, and DWS deletes rows whose `accepted_date_utc_plus8` is inside that interval. The insert side then reloads the same date segment from the lower table. DWM still ranks against all DWD accepted submissions to preserve the global first-accepted rule, then only inserts first-accepted rows whose final first-accepted date falls inside the effective interval.

After a successful run, Java compares affected accepted handle/problem pairs against the previous DWM first-accepted dates and the current DWD global first-accepted dates. If a first-accepted fact moved outside the original batch interval, the service expands the effective interval to the min/max affected dates and reruns the same SQL task DAG. It repeats until the interval is stable, which prevents DWS from retaining a stale summary row on the old first-accepted date.

Java code triggers these SQL files as set-based database work and may query interval/impact metadata; it must not read rows into Java and transform them one by one.

## HTTP Ingest

External collectors can write ODS through HTTP without connecting directly to the database:

```text
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
```

The endpoint requires the platform `admin` role and accepts a JSON array, not a wrapped object. Each array item is the raw source submission object for that OJ.

## HTTP Collection

Admins can ask the backend to collect Codeforces submissions for a recent lookback window:

```text
POST /api/training-data/admin/codeforces/submissions:collect
```

Request body:

```json
{
  "studentIdentity": "112487张三",
  "lookbackHours": 120
}
```

`studentIdentity` must have a Codeforces handle binding and `lookbackHours` must be positive. The service computes `[now - lookbackHours, now)` at execution time and compares that window to each source submission's `creationTimeSeconds`. The endpoint is admin-only. A successful run returns aggregate status plus the resolved handle's result and echoes the computed `windowStartInclusive` / `windowEndExclusive` in the response. Each Codeforces page request defaults to `connect-timeout=10s`, `read-timeout=30s`, `request-interval=4s`, and `max-request-attempts=3`. If the handle request still fails after those attempts, the collector writes no rows, logs a stable `errorCode` with a handle hash, and returns the failed handle's error code/message in the response.

The current scheduled collection path returns only handles with `codeforces_handle_account.need_collect=true` in stable `student_identity` order and de-duplicates by handle before requesting Codeforces. Do not add scheduled collection filtering directly in the HTTP controller; keep that logic in the collection use case or the handle-account read path.

The scheduler path is driven by `platform.training-data.codeforces.collector.schedules` in `application.yml` and disabled by default. The default config file includes a `daily-recent-submissions` schedule with `enabled=false`, `cron="0 0 12 * * *"`, `zone=Asia/Shanghai`, and `lookback=120h`. When that schedule is enabled, the automatic job runs the same collection service and collects from the trigger execution instant back by the configured lookback duration.

Browser-facing batch collection should use the admin job endpoints:

```text
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
GET  /api/training-data/admin/codeforces/submissions/collect-batch-jobs
GET  /api/training-data/admin/codeforces/submissions/collect-batch-jobs/{jobId}
```

The job service keeps at most 50 snapshots in memory, exposes per-identity `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` state, waits the configured `request-interval` between adjacent identities, and may run `CodeforcesWarehouseRefreshService` after each successful batch when requested. This makes frontend refresh/page switching safe for long collection, but it is still process-local and not durable pipeline state. ODS import and manual warehouse refresh remain synchronous admin endpoints.

After ODS write, the direct collector currently does not invoke the unfinished downstream scheduler/orchestrator. The source code has a TODO at that handoff point. The existing manual SQL refresh endpoint remains available separately.

After ingest returns a `batchId`, admins can refresh Codeforces DWD/DWM/DWS through:

```text
POST /api/training-data/admin/codeforces/warehouse:refresh
```

Request body:

```json
{
  "batchId": "external-codeforces-...",
  "startFromTaskId": "codeforces.dwm.handle_problem_first_accepted"
}
```

`startFromTaskId` is optional. When it is present, the runner executes that task and its downstream tasks only; this is the supported manual resume path after a failed node. If the requested `batchId` has no ODS rows with `creation_time_seconds`, the endpoint returns `400` and does not start the SQL task runner. If the refresh detects that first-accepted movement expanded the effective interval, the same resume setting is used for the automatic rerun. Each SQL task uses its own transaction. A node failure stops the DAG immediately, returns `status=FAILED` with `failedTaskId`, marks downstream nodes as `SKIPPED`, and writes an error log with a stable `errorCode`.

## Adding Another OJ

Add a new OJ-specific slice instead of editing an existing OJ table or using a shared submission record:

1. Add a new Maven module such as `training-data-<oj>`.
2. Add OJ-owned HTTP ingress, ingest application service, and collect batch type.
3. Add `ods_<oj>__*` DDL and upsert SQL inside that module.
4. Add `<Oj>Ods...` domain record and writer contract.
5. Add `<Oj>SubmissionParser` and a local fixture.
6. Add a JDBC writer for the new OJ table.
7. Add OJ-owned DWD/DWM/DWS tables and SQL tasks only after the OJ has a concrete downstream query.
8. Add an OJ-owned SQL task manifest and admin refresh endpoint when the SQL chain needs manual execution.
9. Add parser, writer, controller, SQL task, refresh, and domain tests inside that module.
10. Update this document, module docs, and context-map entries.
