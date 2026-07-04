# Submission ODS Tables

This document is the agent-readable contract for current submission ODS storage.

## Scope

The training-data module currently keeps only independent OJ data warehouse domains and ODS batch-upsert entrypoints.

Implemented ODS tables:

- `ods_codeforces__submission`

Each implemented OJ is a vertical Maven module and owns its own HTTP ingress, ingest application service, collect batch type, record, parser, writer, fixture, DDL, upsert SQL, Spring config, and tests:

- `training-data-codeforces`

Do not reintroduce a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, `SourcePlatform`, or shared collect batch to flatten different OJ submission shapes.

External collectors should post raw submission arrays to the OJ-specific HTTP ingest endpoint with a Keycloak token that has the platform `admin` role. The OJ module creates its own `batch_id`, `fetched_at`, `raw_payload`, and `payload_hash`, then writes through its own writer.

DWD/DWS/ADS physical tables are not implemented yet. Future OJ-specific transforms such as `dwd_codeforces__*` should stay independent until a concrete cross-OJ query or ADS workflow needs a unified view or wide table.

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

## HTTP Ingest

External collectors can write ODS through HTTP without connecting directly to the database:

```text
POST /api/training-data/ods/codeforces/submissions:batch-upsert
```

The endpoint requires the platform `admin` role and accepts a JSON array, not a wrapped object. Each array item is the raw source submission object for that OJ. There is no DAG/pipeline endpoint in the current slice.

## Adding Another OJ

Add a new OJ-specific slice instead of editing an existing OJ table or using a shared submission record:

1. Add a new Maven module such as `training-data-<oj>`.
2. Add OJ-owned HTTP ingress, ingest application service, and collect batch type.
3. Add `ods_<oj>__*` DDL and upsert SQL inside that module.
4. Add `<Oj>Ods...` domain record and writer contract.
5. Add `<Oj>SubmissionParser` and a local fixture.
6. Add a JDBC writer for the new OJ table.
7. Add parser, writer, controller, and ODS domain tests inside that module.
8. Update this document, module docs, and context-map entries.
