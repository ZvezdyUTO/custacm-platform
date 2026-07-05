# Submission Warehouse Tables

This document is the agent-readable contract for current Codeforces submission warehouse storage.

## Scope

The training-data module currently keeps independent OJ data warehouse domains and OJ-specific ODS batch-upsert entrypoints.

Implemented Codeforces tables:

- `ods_codeforces__submission`
- `dwd_codeforces__submission`
- `dwm_codeforces__handle_problem_first_accepted`
- `dws_codeforces__handle_daily_rating_accepted_summary`

Each implemented OJ is a vertical Maven module and owns its own HTTP ingress, ingest application service, collect batch type, record, parser, writer, fixture, DDL, ODS upsert SQL, DWD/DWM/DWS SQL task resources, Spring config, and tests:

- `training-data-codeforces`

Do not reintroduce a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, `SourcePlatform`, or shared collect batch to flatten different OJ submission shapes.

External collectors should post raw submission arrays to the OJ-specific HTTP ingest endpoint with a platform JWT that has the `admin` role. The OJ module creates its own `batch_id`, `fetched_at`, `raw_payload`, and `payload_hash`, then writes through its own writer.

Codeforces DWD/DWM/DWS transforms are idempotent SQL task resources. Java scheduling/execution is not implemented yet, and ADS physical tables are not implemented yet. Future cross-OJ transforms should stay independent until a concrete cross-OJ query or ADS workflow needs a unified view or wide table.

## Source Access Notes

Codeforces default test data comes from a local fixture shaped like the public `user.status` API:

```text
https://codeforces.com/api/user.status?handle=tourist&from=1&count=2
```

The reusable local chain-test fixture is `training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json`. It contains 1000 unique submissions captured once from multiple `user.status` requests on `2026-07-03`, with submission times spanning `2022-10-15T14:36:48Z` through `2026-07-03T02:41:35Z`.

Fixture metadata is stored next to the data in `submissions_multi_user_1000.metadata.json`. See [test-data.md](test-data.md) for the source URLs and local API replay command. Do not make default tests or local chain checks refresh this data from Codeforces.

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
| `fetched_at` | collect time | Yes |
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
| `submitted_at` | `creation_time_seconds` added to UTC epoch |
| `submitted_date_utc` | UTC date from `submitted_at` |
| `problem_key` | `problem_contest_id + ':' + problem_index`; null when either part is missing |
| `is_accepted` | `verdict = 'OK'` |
| `ods_batch_id` | ODS `batch_id` |
| `ods_fetched_at` | ODS `fetched_at` |
| `ods_payload_hash` | ODS `payload_hash` |

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwd/upsert_dwd_codeforces__submission.sql
```

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
  and submitted_at is not null
  and submitted_date_utc is not null
```

Tie-break rule:

```text
earliest submitted_at, then smallest codeforces_submission_id
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
```

## Codeforces DWS Daily Rating Summary

`dws_codeforces__handle_daily_rating_accepted_summary` summarizes first accepted problems by handle, UTC date, and Codeforces problem rating.

Grain:

```text
one author_handle + accepted_date_utc + problem_rating_key
```

Primary key:

```text
id
```

Business unique key:

```text
author_handle + accepted_date_utc + problem_rating_key
```

Rating key rule:

```text
problem_rating is null -> UNRATED
problem_rating is not null -> string value of problem_rating
```

Source rule:

```text
dwm_codeforces__handle_problem_first_accepted
group by author_handle, first_accepted_date_utc, problem_rating_key
```

The task SQL is:

```text
training-data-codeforces/src/main/resources/sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

## SQL Task Order

Run the SQL tasks in this order:

```text
sql/dwd/upsert_dwd_codeforces__submission.sql
sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql
sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql
```

Each task is designed to be repeatable. DWD uses `insert ... select ... on duplicate key update`; DWM/DWS tasks delete target-grain rows that no longer exist upstream, then upsert the current derived result. Java code should trigger these SQL files as set-based database work; it should not read rows into Java and transform them one by one.

## HTTP Ingest

External collectors can write ODS through HTTP without connecting directly to the database:

```text
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
```

The endpoint is under the platform admin URL tier, requires the platform `admin` role, and accepts a JSON array, not a wrapped object. Each array item is the raw source submission object for that OJ. There is no DAG/pipeline endpoint or DWD/DWM/DWS refresh HTTP endpoint in the current slice.

## Adding Another OJ

Add a new OJ-specific slice instead of editing an existing OJ table or using a shared submission record:

1. Add a new Maven module such as `training-data-<oj>`.
2. Add OJ-owned HTTP ingress, ingest application service, and collect batch type.
3. Add `ods_<oj>__*` DDL and upsert SQL inside that module.
4. Add `<Oj>Ods...` domain record and writer contract.
5. Add `<Oj>SubmissionParser` and a local fixture.
6. Add a JDBC writer for the new OJ table.
7. Add OJ-owned DWD/DWM/DWS tables and SQL tasks only after the OJ has a concrete downstream query.
8. Add parser, writer, controller, SQL task, and domain tests inside that module.
9. Update this document, module docs, and context-map entries.
