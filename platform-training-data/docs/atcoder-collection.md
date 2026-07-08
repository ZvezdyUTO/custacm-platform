# AtCoder Collection Design

This document records the current AtCoder/Kenkoooo collection design for agents and maintainers.

## Scope

The current slice adds AtCoder source ingestion plus the ODS-to-DWD/DWM/DWS warehouse refresh chain:

- recent-window user submissions from Kenkoooo `user/submissions`;
- startup and low-frequency problem metadata collection from Kenkoooo `resources/problems.json` and `resources/problem-models.json`;
- ODS upsert into `ods_atcoder__submission`, `ods_atcoder__problem`, and `ods_atcoder__problem_model`;
- idempotent SQL task refresh into `dwd_atcoder__submission`, `dwm_atcoder__handle_problem_first_accepted`, and `dws_atcoder__handle_daily_rating_accepted_summary`;
- enabled-by-default startup bootstrap and scheduler for problem metadata refresh.

AtCoder DWD/DWM/DWS physical tables live in `training-data-common` because they follow the common warehouse query contract. AtCoder-specific cleaning SQL and the refresh service/handler live in `training-data-atcoder`.

## Module Split

`training-data-common` owns OJ-generic orchestration:

- `OjSubmissionCollectionService` computes windows, resolves handles, skips overlapping JVM-local runs, and aggregates results.
- `OjCollectionRequestExecutor` owns retry and request interval behavior.
- `OjSubmissionCollectionDispatcher` selects an OJ collector by normalized `ojName`.
- `OjWarehouseRefreshDispatcher` selects an OJ warehouse refresh handler by normalized `ojName`.
- Common HTTP routes keep the Codeforces-compatible collection paths and pass `ojName` through.
- Scheduled recent-submission collection reads `oj_handle_account.need_collect=true` accounts for the schedule `ojName` and refreshes the selected OJ warehouse when ODS ingest returns a batch.
- Successful recent-submission collection updates `oj_handle_account.collection_states_json.ATCODER.lastCollectedAt` with the collector execution instant. `historyStartReached` is only set when the Kenkoooo submission query starts from `from_second=0`.
- AtCoder problem metadata startup bootstrap checks `ods_atcoder__problem` and `ods_atcoder__problem_model`, then collects Kenkoooo metadata when either table is empty.

`training-data-atcoder` owns AtCoder-specific behavior:

- Kenkoooo source client paths and response validation.
- AtCoder submission pagination using `from_second`.
- AtCoder problem-list/problem-model fetching and startup bootstrap.
- AtCoder ODS records, parsers, writers, upsert SQL, and purge adapter.
- AtCoder ODS-to-DWD/DWM/DWS SQL tasks, refresh interval derivation, and refresh handler used by collection jobs and scheduled collection.
- AtCoder problem metadata startup bootstrap and schedule.

This keeps the OJ boundary vertical: common can coordinate an OJ by contract, but it does not know Kenkoooo payload shapes or AtCoder ODS SQL.

## Submission Collection

AtCoder submission collection reuses the existing admin route:

```text
POST /api/training-data/admin/codeforces/submissions:collect
```

Request bodies set `ojName=ATCODER`. The shared app service resolves `studentIdentity` to the bound `ATCODER` handle in `oj_handle_account.handles_json`.

Kenkoooo accepts `from_second`, so the adapter starts at the computed window start and advances by the largest returned `epoch_second + 1`. Matching uses the shared recent-window contract `[windowStartInclusive, windowEndExclusive)`. Kenkoooo returns at most 500 submissions per call; the collector stops when the page is empty, smaller than `page-size`, or reaches the window end.

ODS batches use the `collector-atcoder-*` prefix. Upsert is idempotent by `atcoder_submission_id`, while each run refreshes `batch_id`, `fetched_at`, `raw_payload`, and `payload_hash` for the source row.

## Warehouse Refresh

Browser-facing batch collection can set `refreshWarehouse=true` on:

```text
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
```

When `ojName=ATCODER`, the common job service calls the AtCoder warehouse refresh handler after each successful ODS batch if `refreshWarehouse=true`; scheduled recent-submission collection calls the same handler whenever a batch is produced. There is no standalone AtCoder warehouse refresh HTTP endpoint.

The AtCoder manifest is:

```text
training-data-atcoder/src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml
```

Task order:

```text
sql/dwd/upsert_dwd_atcoder__submission.sql
sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql
sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql
```

The refresh interval is based on the batch's `ods_atcoder__submission.epoch_second` values converted to UTC+8 dates. The interval repository also includes existing DWM first-accepted dates for accepted `handle + problem_key` pairs touched by the batch, so stale DWS rows are removed when a newly collected accepted submission moves first AC to an earlier date.

DWD derives `submission_id` from `atcoder_submission_id`, `handle` from `user_id`, UTC+8 submitted time from `epoch_second`, `problem_key` from `problem_id`, and accepted status from `result='AC'`. It enriches `problem_index` and display name from `ods_atcoder__problem` when the low-frequency problem metadata has been refreshed. It enriches difficulty from `ods_atcoder__problem_model.clipped_difficulty` for non-experimental ABC/ARC/AGC tasks and stores the AtCoder Problems bucket lower-bound key: `0`, `400`, `800`, `1200`, `1600`, `2000`, `2400`, or `2800+`. Missing model data, experimental models, and non-ABC/ARC/AGC contest tasks are grouped under `UNRATED`.

## Problem Metadata Collection

AtCoder problem metadata changes much more slowly than submissions, so it is intentionally separate from recent-submission collection. The startup bootstrap and scheduler fetch `resources/problems.json`, validate an array response, parse the items into `AtcoderOdsProblem`, and upsert by `problem_id`. There is no manual HTTP endpoint for this refresh.
The same collection run also fetches `resources/problem-models.json`, validates an object response keyed by problem id, parses each item into `AtcoderOdsProblemModel`, stores raw difficulty plus Kenkoooo clipped difficulty, and upserts by `problem_id`.

Startup bootstrap and the enabled-by-default scheduler are configured by:

```yaml
platform.training-data.atcoder.problem-list-collector:
  enabled: true
  bootstrap-on-startup: true
  bootstrap-only-when-empty: true
  cron: "0 30 3 1/3 * ?"
  zone: Asia/Shanghai
```

On service startup, the bootstrap runner pulls problem metadata only when `ods_atcoder__problem` or `ods_atcoder__problem_model` is empty by default. Collection failures are logged with a stable error code and do not block service startup; the every-three-days scheduler remains available for recovery. This keeps high-volume submission polling and low-frequency dictionary refresh independent.

## Kenkoooo Access Policy

Kenkoooo asks clients to wait more than one second between accesses. The AtCoder collector defaults to:

```yaml
platform.training-data.atcoder.collector:
  request-interval: 2s
  max-request-attempts: 3
```

Tests use fake clients or local HTTP servers and must not depend on live Kenkoooo availability.

## Future Work

Do not add persistent pipeline run state, ADS tables, or a cross-OJ warehouse DAG in this slice. Future AtCoder extensions should stay AtCoder-owned unless the SQL or Java contract is genuinely common.
