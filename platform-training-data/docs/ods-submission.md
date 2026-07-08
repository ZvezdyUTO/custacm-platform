# Submission Warehouse Tables

This document is the agent-readable contract for current OJ ODS and submission warehouse storage.

## Scope

The training-data module currently keeps independent OJ data warehouse domains and OJ-specific ODS writers behind collection application services. Manual ODS batch-upsert HTTP endpoints are intentionally not exposed.

Implemented Codeforces warehouse tables:

- `ods_codeforces__submission`
- `dwd_codeforces__submission`
- `dwm_codeforces__handle_problem_first_accepted`
- `dws_codeforces__handle_daily_rating_accepted_summary`

Implemented AtCoder landing and warehouse tables:

- `ods_atcoder__submission`
- `ods_atcoder__problem`
- `ods_atcoder__problem_model`
- `dwd_atcoder__submission`
- `dwm_atcoder__handle_problem_first_accepted`
- `dws_atcoder__handle_daily_rating_accepted_summary`

Each implemented OJ is a vertical Maven module and owns its own ingest application service, source collection adapter when collection is supported, collect batch type, record, parser, writer, fixture or fake-client tests, DDL, ODS upsert SQL, SQL task resources/manifests when it has a cleaning chain, OJ-specific refresh interval SQL, Spring config, and tests. Generic warehouse refresh service and handler code lives in `training-data-common` and is configured with each OJ's manifest and interval repository. Shared recent-lookback collection orchestration lives in `training-data-common` and is reused through OJ collectors:

- `training-data-codeforces`
- `training-data-atcoder` owns the AtCoder ODS slice, Kenkoooo source client, submission/problem/problem-model parser, ODS writers, recent-window submission collector, startup and low-frequency problem metadata collector, and AtCoder DWD/DWM/DWS SQL task resources. `training-data-common` owns the AtCoder same-layer DWD/DWM/DWS table DDL because those tables follow the common warehouse contract.

Do not reintroduce a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, `SourcePlatform`, or shared collect batch to flatten different OJ submission shapes.

ODS writes happen through OJ collection application services. The OJ module creates its own `batch_id`, `fetched_at`, `raw_payload`, and `payload_hash`, then writes through its own writer. Reintroducing external/manual ODS ingest HTTP endpoints requires a new product decision.

The common web layer exposes a Codeforces-compatible admin recent-lookback collection endpoint that calls the OJ collection dispatcher for a platform `studentIdentity` and writes matching submissions through the selected OJ ODS ingest path. The endpoint accepts `studentIdentity`, `lookbackHours`, and optional `ojName`; omitted `ojName` uses the configured default OJ, currently `CODEFORCES`, while explicit `ATCODER` selects the AtCoder collector. The shared collection service resolves the identity to the selected OJ handle from the OJ handle map, uses its current execution instant as the right boundary, and collects submissions from that instant back by the requested number of hours. Successful handle collection writes `oj_handle_account.collection_states_json[OJ_NAME]` with `lastCollectedAt` equal to the collector execution instant and `historyStartReached` when the adapter proves the historical left edge has been reached. Browser-driven batch collection uses an in-process job service that runs multiple identities sequentially and exposes start/list snapshots for polling; it is not persisted across backend restarts and must not become a general pipeline run-state table. Scheduled collection reads accounts whose `oj_handle_account.need_collect` flag is true through the handle-account service, selects the handle named by the schedule `ojName`, de-duplicates by handle, and triggers the selected OJ warehouse refresh through the common SQL-task handler when ODS ingest returns a batch. The shared request executor handles rate limiting and retries; OJ adapters own source-specific pagination, timestamp extraction, source failure mapping, OJ-specific history-start detection, and ODS writes.

Training data exposes an admin student-data purge endpoint for high-risk account cleanup. It accepts a platform `studentIdentity` and required `ojName`; a blank or omitted `ojName` is rejected. The common purge service resolves that OJ handle binding, deletes same-layer DWS/DWM/DWD rows through the warehouse purge adapter and ODS rows through the OJ-specific purge adapter in one transaction, and keeps the `oj_handle_account` row. Missing handle bindings return zero deletion counts. Auth account deletion remains owned by `auth-web`; callers that need full user deletion should clear training data once per currently bound OJ first and then call the auth admin delete endpoint.

Codeforces and AtCoder DWD/DWM/DWS transforms are idempotent SQL task resources. The current Java execution path is backed by the shared SQL task DAG executor, common refresh service/handler, and common OJ refresh dispatcher, invoked either by collection jobs when requested or by enabled scheduled collection when a batch is produced; there is no standalone warehouse refresh HTTP endpoint, persistent task run state, or ADS physical table. OJ automatic collection has an enabled-by-default Spring scheduled trigger wired by `training-data-common` that calls the same recent-lookback collection use case with the schedule `ojName`; it is not a persistent pipeline scheduler.

Read-side Java repositories can query existing DWD/DWM/DWS warehouse tables directly when the target table already matches the query grain. Current query support is exposed through guest HTTP endpoints that keep the Codeforces path and accept `ojName` as the pass-through OJ parameter; app services resolve that OJ's handle from `oj_handle_account.handles_json`, and JDBC repositories select same-layer tables by normalized OJ name, for example `dwd_codeforces__submission`.

AtCoder currently has ODS landing tables, Kenkoooo source clients, ODS parsers/writers, recent-window submission collection, startup and low-frequency problem metadata collection, an ODS purge adapter, and ODS-to-DWD/DWM/DWS refresh SQL in `training-data-atcoder`. Same-layer DWD/DWM/DWS physical tables live in `training-data-common` for common warehouse reads and student-scoped cleanup.

## Java Package Layout

Common OJ logic and common HTTP endpoints live in `training-data-common`; Codeforces keeps only its OJ-specific app/domain/infra/config code and resources:

```text
training-data-common/
  app/
    account/     # OJ handle-account use cases and app errors
    purge/       # student-scoped OJ data deletion use case
    query/       # DWD/DWM/DWS read use cases and query results
    warehouse/   # generic SQL-task-backed warehouse refresh service
  collector/
    config/      # typed scheduled-collection properties
    dispatch/    # OJ collector registry/dispatcher contracts
    job/         # in-process collection job service and snapshots
    result/      # collection result records and status enums
  domain/
    oj/          # OJ handle-account, DWD/DWM/DWS, purge, and value contracts
  infra/
    oj/          # OJ-common JDBC account, query, table-name, and warehouse purge adapters
  scheduler/     # enabled-by-default scheduled collection wiring
  web/
    account/     # handle-account controller, requests, responses, and error mapping
    collector/   # collection controller, requests, responses, and error mapping
    purge/       # student-data purge controller and response DTOs
    query/       # public warehouse query controller and response DTOs

training-data-codeforces/
  app/           # Codeforces ODS ingest and collection facade/adapter
  config/        # typed Codeforces source properties and Spring Bean wiring
  domain/        # Codeforces source, ODS, parser, writer, and purge contracts
  infra/         # Codeforces source client, payload parser, ODS writer, ODS purge, and refresh interval adapters

training-data-atcoder/
  app/           # AtCoder ODS ingest, submission collection, and problem metadata collection use cases
  config/        # typed Kenkoooo properties, problem metadata schedule, and Spring Bean wiring
  domain/        # AtCoder source, ODS, parser, writer, and purge contracts
  infra/         # Kenkoooo client, payload parser, ODS writers, ODS purge, and refresh interval adapters
```

Tests mirror the same package layout under each module's `src/test/java`.

## Warehouse Time Zone

Training-data warehouse tables use UTC+8 (`Asia/Shanghai`) as the canonical local time zone for stored `datetime` and date-grain fields.

Codeforces source `creationTimeSeconds` and AtCoder source `epoch_second` are epoch seconds. DWD converts them to `submitted_at_utc_plus8` by adding eight hours to the UTC epoch timestamp, then derives `submitted_date_utc_plus8` from that UTC+8 local timestamp. DWM and DWS inherit the same UTC+8 day boundary. Java read-side query objects use `LocalDateTime` / `LocalDate` values that are already in UTC+8.

ODS `fetched_at` is also written as a UTC+8 local `datetime`. Runtime MySQL configuration should keep `serverTimezone=Asia/Shanghai`.

## Source Access Notes

Codeforces default test data comes from a local fixture shaped like the public `user.status` API:

```text
https://codeforces.com/api/user.status?handle=tourist&from=1&count=2
```

The reusable local chain-test fixture is `training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json`. It contains 1000 unique submissions captured once from multiple `user.status` requests on `2026-07-03`, with submission times spanning `2022-10-15T14:36:48Z` through `2026-07-03T02:41:35Z`.

Fixture metadata is stored next to the data in `submissions_multi_user_1000.metadata.json`. See [test-data.md](test-data.md) for the source URLs and local API replay command. Do not make default tests or local chain checks refresh this data from Codeforces.

The internal collector uses `user.status` with `handle`, 1-based `from`, and `count` pagination. Codeforces returns newest submissions first and does not accept a time-window parameter, so the collector pages per handle, filters by `creationTimeSeconds` against the service-computed `[now - lookback, now)` window, and stops when a returned page is smaller than the configured page size or when a full page is older than the requested window. Default tests use fake clients or local HTTP servers; they must not call live Codeforces.

Kenkoooo's AtCoder Problems API documents the submission endpoint and static problem resource:

```text
https://kenkoooo.com/atcoder/atcoder-api/v3/user/submissions?user={user_id}&from_second={unix_second}
https://kenkoooo.com/atcoder/resources/problems.json
https://kenkoooo.com/atcoder/resources/problem-models.json
```

User submissions require an AtCoder user id and a starting unix second; the API returns up to 500 submissions after that time. The internal collector starts from the computed lookback-window start, advances by the largest returned `epoch_second + 1`, and filters by `[windowStartInclusive, windowEndExclusive)`. The source fields observed from the documented endpoint are `id`, `epoch_second`, `problem_id`, `contest_id`, `user_id`, `language`, `point`, `length`, `result`, and `execution_time`. The problem-list JSON fields observed from `resources/problems.json` are `id`, `contest_id`, `problem_index`, `name`, and `title`. The problem-model JSON from `resources/problem-models.json` is an object keyed by problem id; model fields include `slope`, `intercept`, `variance`, `difficulty`, `discrimination`, `irt_loglikelihood`, `irt_users`, and `is_experimental`. Kenkoooo asks clients to sleep for more than one second between accesses, so the AtCoder collector defaults to `request-interval=2s`.

## AtCoder ODS Fields

`ods_atcoder__submission` preserves Kenkoooo AtCoder user submission semantics.

| Column | Source field | Required for ODS |
| --- | --- | --- |
| `atcoder_submission_id` | `id` | Yes |
| `epoch_second` | `epoch_second` | Yes |
| `problem_id` | `problem_id` | No |
| `contest_id` | `contest_id` | No |
| `user_id` | `user_id` | Yes |
| `language` | `language` | No |
| `point` | `point` | No |
| `source_code_length` | `length` | No |
| `result` | `result` | No |
| `execution_time_millis` | `execution_time` | No |
| `batch_id` | collect batch id | Yes |
| `fetched_at` | collect time stored as UTC+8 local `datetime` | Yes |
| `raw_payload` | raw source item JSON | Yes |
| `payload_hash` | SHA-256 of `raw_payload` | Yes |

The unique key is `atcoder_submission_id`.

`ods_atcoder__problem` preserves Kenkoooo `resources/problems.json` problem-list items.

| Column | Source field | Required for ODS |
| --- | --- | --- |
| `problem_id` | `id` | Yes |
| `contest_id` | `contest_id` | No |
| `problem_index` | `problem_index` | No |
| `problem_name` | `name` | No |
| `title` | `title` | No |
| `batch_id` | collect batch id | Yes |
| `fetched_at` | collect time stored as UTC+8 local `datetime` | Yes |
| `raw_payload` | raw source item JSON | Yes |
| `payload_hash` | SHA-256 of `raw_payload` | Yes |

The unique key is `problem_id`.

`ods_atcoder__problem_model` preserves Kenkoooo `resources/problem-models.json` problem-model items and stores both raw and clipped difficulty. The clipped value follows Kenkoooo's AtCoder Problems display formula: raw difficulty values below 400 are transformed with `round(400 / exp(1 - difficulty / 400))`; values at least 400 are kept unchanged.

| Column | Source field | Required for ODS |
| --- | --- | --- |
| `problem_id` | object key | Yes |
| `slope` | `slope` | No |
| `intercept` | `intercept` | No |
| `variance` | `variance` | No |
| `raw_difficulty` | `difficulty` | No |
| `clipped_difficulty` | derived from `difficulty` | No |
| `discrimination` | `discrimination` | No |
| `irt_loglikelihood` | `irt_loglikelihood` | No |
| `irt_users` | `irt_users` | No |
| `is_experimental` | `is_experimental` | No |
| `batch_id` | collect batch id | Yes |
| `fetched_at` | collect time stored as UTC+8 local `datetime` | Yes |
| `raw_payload` | raw source item JSON | Yes |
| `payload_hash` | SHA-256 of `raw_payload` | Yes |

The unique key is `problem_id`.

## AtCoder Warehouse Tables

AtCoder DWD/DWM/DWS tables intentionally match the common same-layer warehouse query contract:

- `dwd_atcoder__submission`
- `dwm_atcoder__handle_problem_first_accepted`
- `dws_atcoder__handle_daily_rating_accepted_summary`

They are created by `training-data-common/src/main/resources/db/migration/V020__create_atcoder_warehouse_tables.sql`.

The SQL task resources are:

```text
training-data-atcoder/src/main/resources/sql/dwd/upsert_dwd_atcoder__submission.sql
training-data-atcoder/src/main/resources/sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql
training-data-atcoder/src/main/resources/sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql
training-data-atcoder/src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml
```

`dwd_atcoder__submission` is the cleaned single-submission detail table derived from `ods_atcoder__submission` and optionally enriched from `ods_atcoder__problem` plus `ods_atcoder__problem_model`.

Important derived fields:

| Column | Rule |
| --- | --- |
| `ods_submission_id` | ODS submission row `id` |
| `submission_id` | ODS `atcoder_submission_id` converted to string |
| `handle` | ODS `user_id` |
| `submitted_at_utc_plus8` | ODS `epoch_second` added to UTC epoch, then shifted to UTC+8 local `datetime` |
| `submitted_date_utc_plus8` | UTC+8 local date from `submitted_at_utc_plus8` |
| `problem_key` | ODS `problem_id` |
| `problem_index` | `ods_atcoder__problem.problem_index` when problem-list metadata exists |
| `problem_name` | `ods_atcoder__problem.title`, then `problem_name`, then ODS `problem_id` |
| `difficulty` | AtCoder Problems bucket lower bound derived from `ods_atcoder__problem_model.clipped_difficulty` for non-experimental ABC/ARC/AGC tasks: `0`, `400`, `800`, `1200`, `1600`, `2000`, `2400`, or `2800+`; `null` when no model row exists, the model is experimental, or the task is from another contest family |
| `language` | ODS `language` |
| `is_accepted` | `result = 'AC'` |
| `source_url` | `https://atcoder.jp/contests/{contest_id}/submissions/{submission_id}` when contest id is available |
| `ods_batch_id` | ODS `batch_id` |
| `ods_fetched_at` | ODS `fetched_at` |
| `ods_payload_hash` | ODS `payload_hash` |

`dwm_atcoder__handle_problem_first_accepted` ranks accepted AtCoder DWD rows by `handle + problem_key`, earliest UTC+8 submission time, then smaller source-code length and submission id. It records the first accepted submission whose final first-accepted date is inside the effective refresh interval.

`dws_atcoder__handle_daily_rating_accepted_summary` groups AtCoder DWM rows by `handle`, UTC+8 first-accepted date, and `coalesce(difficulty, 'UNRATED')`. AtCoder uses its own range buckets independent of Codeforces, and missing model data remains `UNRATED`.

## Codeforces ODS Fields

`ods_codeforces__submission` preserves Codeforces `Submission` / `user.status` semantics at `submission + collected handle` grain. The collector fetches one user's `user.status` page at a time; if Codeforces returns a team submission for that page, the ODS row is attributed to the handle being collected, after verifying that handle appears in `author.members`.

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
| `author_handle` | collected handle for the current `user.status?handle=...` request | Yes |
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

The business unique key is `codeforces_submission_id + author_handle`.

## Codeforces DWD Submission

`dwd_codeforces__submission` is the cleaned single-submission detail table derived from `ods_codeforces__submission`, also at `submission + handle` grain.

Grain:

```text
one Codeforces submission per collected handle
```

Primary key:

```text
id
```

Business unique key:

```text
submission_id + handle
```

Important derived fields:

| Column | Rule |
| --- | --- |
| `ods_submission_id` | ODS row `id` |
| `submission_id` | ODS `codeforces_submission_id` converted to string |
| `handle` | ODS `author_handle` |
| `submitted_at_utc_plus8` | `creation_time_seconds` added to UTC epoch, then shifted to UTC+8 local `datetime` |
| `submitted_date_utc_plus8` | UTC+8 local date from `submitted_at_utc_plus8` |
| `problem_key` | `problem_contest_id + ':' + problem_index`; null when either part is missing |
| `difficulty` | ODS `problem_rating` converted to string; null when missing |
| `language` | ODS `programming_language` |
| `is_accepted` | `verdict = 'OK'` |
| `source_url` | `https://codeforces.com/contest/{contest_id}/submission/{submission_id}` when contest id is available |
| `ods_batch_id` | ODS `batch_id` |
| `ods_fetched_at` | ODS `fetched_at` |
| `ods_payload_hash` | ODS `payload_hash` |

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwd/upsert_dwd_codeforces__submission.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/submissions/by-student?ojName=CODEFORCES
GET /api/training-data/codeforces/submissions/by-problem?ojName=CODEFORCES
 -> OjWarehouseQueryController
 -> OjSubmissionQueryService
 -> OjSubmissionRepository
 -> JdbcOjSubmissionRepository
 -> dwd_{oj}__submission
```

At the app layer, personal DWD reads accept platform `studentIdentity` plus the requested OJ name; the service resolves it through `oj_handle_account.handles_json[OJ_NAME]` and then builds repository handle criteria. The DWD repository query model supports two atomic reads:

- by requested handle, optional inclusive UTC+8 submitted time range, optional Codeforces rating lower/upper bounds mapped to string `difficulty`, and backend pagination `limit/offset`;
- by requested `problemKey`, optional inclusive UTC+8 submitted time range, across all handles, and backend pagination `limit/offset`.

Null time bounds mean no lower or upper time limit. Null problem rating bounds mean all rated and unrated rows. Setting either problem rating bound filters to the requested inclusive rating interval and excludes unrated rows.
The public HTTP submission detail endpoints accept `page` and `limit`; `page` is 1-based, `limit` defaults to `15`, and `limit` is capped at `2000`. Each response returns exact `total`, `totalPages`, `hasMore`, and the requested page's submission rows.

The repository returns DWD atomic submission rows. The app service returns report records that keep matching submission details:

- personal query: requested `studentIdentity`, resolved handle, and matching submission detail items with `studentIdentity + handle`;
- problem query: requested problem key and matching submission detail items across handles with `studentIdentity + handle`; unbound result handles fail the app query.

Time and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## Codeforces DWM First Accepted

`dwm_codeforces__handle_problem_first_accepted` records the first accepted submission for each Codeforces handle and problem. It is DWM because it is a reusable intermediate fact derived from DWD, not the final topic summary.

Grain:

```text
one handle + problem_key
```

Primary key:

```text
id
```

Business unique key:

```text
handle + problem_key
```

Source rule:

```text
dwd_codeforces__submission
where is_accepted = 1
  and problem_key is not null
  and problem_index is not null
  and submitted_at_utc_plus8 is not null
  and submitted_date_utc_plus8 is not null
```

Tie-break rule:

```text
earliest submitted_at_utc_plus8, then smallest submission_id
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/first-accepted/by-student?ojName=CODEFORCES
GET /api/training-data/codeforces/first-accepted/by-problem?ojName=CODEFORCES
 -> OjWarehouseQueryController
 -> OjFirstAcceptedProblemQueryService
 -> OjFirstAcceptedProblemRepository
 -> JdbcOjFirstAcceptedProblemRepository
 -> dwm_{oj}__handle_problem_first_accepted
```

At the app layer, personal DWM reads accept platform `studentIdentity` plus the requested OJ name; the service resolves it through `oj_handle_account.handles_json[OJ_NAME]` and then builds repository handle criteria. The DWM repository query model supports two atomic reads:

- by requested handle, optional inclusive UTC+8 first-accepted time range, and optional Codeforces rating lower/upper bounds mapped to string `difficulty`;
- by requested `problemKey`, plus optional inclusive UTC+8 first-accepted time range, across all handles.

Null time bounds mean no lower or upper time limit. Null problem rating bounds mean all rated and unrated rows. Setting either problem rating bound filters to the requested inclusive rating interval and excludes unrated rows.

The repository returns DWM atomic first-accepted rows. The app service returns report records that keep the current query's required detail list:

- personal query: requested `studentIdentity`, resolved handle, accepted problem total, and first-accepted problem detail items;
- problem query: requested problem key, accepted handle count, and accepted `studentIdentity + handle` list with each handle's first accepted UTC+8 time; unbound result handles fail the app query.

Time and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## Codeforces DWS Daily Rating Summary

`dws_codeforces__handle_daily_rating_accepted_summary` summarizes first accepted problems by handle, UTC+8 local date, and difficulty. The table name keeps the historical Codeforces rating wording, but the physical shape is now a narrow `difficulty + accepted_problem_count` summary.

Grain:

```text
one handle + accepted_date_utc_plus8 + difficulty
```

Primary key:

```text
id
```

Business unique key:

```text
handle + accepted_date_utc_plus8 + difficulty
```

Summary columns:

```text
difficulty
accepted_problem_count
```

Source rule:

```text
dwm_codeforces__handle_problem_first_accepted
group by handle, first_accepted_date_utc_plus8, coalesce(difficulty, 'UNRATED')
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

Public HTTP and Java query boundary:

```text
GET /api/training-data/codeforces/accepted-summary?ojName=CODEFORCES
 -> OjWarehouseQueryController
 -> OjAcceptedSummaryQueryService
 -> OjAcceptedSummaryRepository
 -> JdbcOjAcceptedSummaryRepository
 -> dws_{oj}__handle_daily_rating_accepted_summary
```

At the app layer, single-student DWS reads accept platform `studentIdentity` plus the requested OJ name; the service resolves it through `oj_handle_account.handles_json[OJ_NAME]` and then builds repository handle criteria. There is no public automatic-summary query endpoint. The repository query model supports:

- requested handle;
- optional inclusive UTC+8 date range;
- optional Codeforces rating lower/upper bounds mapped to string `difficulty`. DWS includes unrated only when both problem rating bounds are absent.

The repository returns DWS atomic rows at the table grain:

```text
handle + accepted_date_utc_plus8 + difficulty
```

The app service returns aggregated reports with:

- the requested `studentIdentity` and resolved handle;
- interval-level rating totals derived from narrow difficulty rows;
- total accepted problem count for the requested interval.

Date and rating filters affect the selected rows, but report payloads do not echo those request criteria.

## SQL Task Order

Run each OJ's SQL tasks in this order. The common refresh service passes `batchId` plus the effective UTC+8 refresh interval as `refreshFromDateUtcPlus8` and `refreshToDateUtcPlus8`.

This branch intentionally reshapes the Codeforces warehouse to the common same-layer table contract by rebuilding the Codeforces DWD/DWM/DWS tables. `V017__reshape_codeforces_warehouse_to_common_contract.sql` drops and recreates `dwd_codeforces__submission`, `dwm_codeforces__handle_problem_first_accepted`, and `dws_codeforces__handle_daily_rating_accepted_summary`; it does not migrate previous DWD/DWM/DWS rows into the new contract. The ODS landing table remains the durable source for future refreshes, but public warehouse reads can be empty immediately after deployment until fresh collection/refresh work repopulates the warehouse tables. This rebuild is a one-time impact of the `V017` upgrade; the other current migrations do not require clearing or rebuilding the warehouse. Future upgrades must preserve warehouse data by default unless their Flyway script, module documentation, and changelog explicitly declare another destructive rebuild.

Codeforces:

```text
sql/dwd/upsert_dwd_codeforces__submission.sql
sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

AtCoder:

```text
sql/dwd/upsert_dwd_atcoder__submission.sql
sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql
sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql
```

The executable manifests are:

```text
training-data-codeforces/src/main/resources/sql/tasks/codeforces-warehouse-refresh.yml
training-data-atcoder/src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml
```

The shared SQL task runner reads this manifest on every refresh request, rebuilds the adjacency-list graph, checks that it is a DAG, and then executes the topological plan.

Each task is designed to be repeatable for the effective UTC+8 date interval. The base interval is the inclusive date range derived from the batch's ODS submission epoch seconds after converting them to UTC+8 local dates, so it covers the full min/max submission time span. If the `batchId` has no ODS rows with a usable submission epoch second, the refresh request is rejected before SQL task execution instead of running a no-op refresh with an empty interval. Before inserting, DWD deletes rows whose `submitted_date_utc_plus8` is inside that interval, DWM deletes rows whose `first_accepted_date_utc_plus8` is inside that interval, and DWS deletes rows whose `accepted_date_utc_plus8` is inside that interval. The insert side then reloads the same date segment from the lower table. DWM still ranks against all DWD accepted submissions to preserve the global first-accepted rule, then only inserts first-accepted rows whose final first-accepted date falls inside the effective interval.

Codeforces computes the interval from `ods_codeforces__submission.creation_time_seconds` by taking the min/max UTC+8 submitted dates in the batch.

AtCoder computes its interval from `ods_atcoder__submission.epoch_second` and expands it up front with existing DWM first-accepted dates for accepted `handle + problem_key` pairs touched by the batch. That covers the stale-row case where a newly collected AtCoder accepted submission moves the global first accepted date earlier than the batch's current date segment.

Java code triggers these SQL files as set-based database work and may query interval/impact metadata; it must not read rows into Java and transform them one by one.

## ODS Ingest

ODS ingest is now an internal application-service step in the recent-window collection flow. The module keeps OJ-specific parsers, records, writers, and SQL contracts, but it no longer exposes a manual HTTP endpoint for posting raw submission arrays.

## HTTP Collection

Admins can ask the backend to collect Codeforces or AtCoder submissions for a recent lookback window:

```text
POST /api/training-data/admin/codeforces/submissions:collect
```

Request body:

```json
{
  "studentIdentity": "112487张三",
  "lookbackHours": 120,
  "ojName": null
}
```

`studentIdentity` must have a handle for the requested OJ in its OJ handle map and `lookbackHours` must be positive. `ojName=null` uses the dispatcher default OJ, currently `CODEFORCES`; `ojName=ATCODER` selects the Kenkoooo AtCoder collector. The shared collection service computes `[now - lookbackHours, now)` at execution time. The Codeforces adapter compares that window to each source submission's `creationTimeSeconds`; the AtCoder adapter compares it to Kenkoooo `epoch_second`. The endpoint is admin-only. A successful run returns aggregate status plus the resolved handle's result and echoes the computed `windowStartInclusive` / `windowEndExclusive` in the response. After a successful handle run, common code updates `collection_states_json` for that OJ even when the window matched no submissions; failed handle runs and failed ODS writes do not mark a successful collection. Codeforces sets `historyStartReached` only when pagination reaches the source's final page. AtCoder sets it only when the Kenkoooo submission query starts from `from_second=0`. Each Codeforces page request defaults to `connect-timeout=10s`, `read-timeout=30s`, `request-interval=4s`, and `max-request-attempts=3`. Each AtCoder request defaults to `connect-timeout=10s`, `read-timeout=30s`, `request-interval=2s`, and `max-request-attempts=3`. If the handle request still fails after those attempts, the collector writes no rows, logs a stable `errorCode` with a handle hash, and returns the failed handle's error code/message in the response.

The current scheduled collection path returns only accounts with `oj_handle_account.need_collect=true` and a handle for the schedule `ojName`, in stable `student_identity` order, and de-duplicates by handle before requesting the source. Do not add scheduled collection filtering directly in the HTTP controller; keep that logic in the collection use case or the handle-account read path.

The scheduler path is driven by `platform.training-data.collector.schedules` in `application.yml` and enabled by default. The default config file includes `daily-recent-submissions` with `oj-name=CODEFORCES`, `cron="0 0 12 * * *"`, and `atcoder-daily-recent-submissions` with `oj-name=ATCODER`, `cron="0 15 12 * * *"`; both use `zone=Asia/Shanghai`, `lookback=120h`, and `enabled=true`. The automatic job runs the same collection service and collects from the trigger execution instant back by the configured lookback duration. When the collection result has a `batchId`, the scheduler calls the shared OJ warehouse refresh dispatcher bean used by collection jobs so DWD/DWM/DWS are refreshed after ODS ingest. A no-batch run skips warehouse refresh.

Browser-facing batch collection should use the admin job endpoints:

```text
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
GET  /api/training-data/admin/codeforces/submissions/collect-batch-jobs
```

The job service keeps at most 50 snapshots in memory, exposes per-identity `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` state, waits the common `platform.training-data.collector.job-item-interval` between adjacent identities, and may run the selected OJ's configured common warehouse refresh handler after each successful batch when requested. This makes frontend refresh/page switching safe for long collection, but it is still process-local and not durable pipeline state. There is no single-job query endpoint; callers list retained jobs and find the target job client-side.

Direct single-identity collection writes ODS and returns the generated `batchId`; it does not expose a separate refresh call. Browser-facing batch collection can request `refreshWarehouse=true`, in which case each successful collected batch calls the common warehouse refresh app service configured for the selected OJ inside the same background job. Scheduled collection always asks for the same refresh when a batch exists. Current successful refresh handlers are Codeforces and AtCoder.

AtCoder problem metadata is collected through in-process startup bootstrap and a low-frequency scheduler. `training-data-web` has an enabled-by-default startup bootstrap under `platform.training-data.atcoder.problem-list-collector`: with `bootstrap-on-startup=true` and `bootstrap-only-when-empty=true`, it collects `resources/problems.json` and `resources/problem-models.json` once after startup when either `ods_atcoder__problem` or `ods_atcoder__problem_model` is empty. The same property group has an enabled-by-default scheduler with the default cron `0 30 3 1/3 * ?` in `Asia/Shanghai`, so metadata is refreshed every three days. There is no manual HTTP endpoint for refreshing AtCoder problem metadata.

## Adding Another OJ

Add a new OJ-specific slice instead of editing an existing OJ table or using a shared submission record:

1. Add a new Maven module such as `training-data-<oj>`.
2. Add OJ-owned ingest application service, source collection adapter when collection is supported, and collect batch type.
3. Add `ods_<oj>__*` DDL and upsert SQL inside that module.
4. Add `<Oj>Ods...` domain record and writer contract.
5. Add `<Oj>SubmissionParser` and a local fixture.
6. Add a JDBC writer for the new OJ table.
7. Add common-contract DWD/DWM/DWS table DDL under `training-data-common` only after the OJ has a concrete downstream query.
8. Add SQL task resources, a manifest, and an OJ-specific implementation of the common refresh interval repository when collection jobs need to refresh warehouse tables; wire the common refresh service/handler with the OJ name and manifest.
9. Add parser, writer, collection HTTP wiring if exposed, SQL task, interval repository, and domain tests in the module that owns the corresponding behavior.
10. Update this document, module docs, and context-map entries.
