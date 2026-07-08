# training-data-atcoder

This module is the AtCoder vertical OJ slice for `platform-training-data`.

It owns Kenkoooo source access, AtCoder ODS ingest, recent-window submission collection, startup and low-frequency problem-list collection, AtCoder ODS purge, AtCoder ODS migrations/upsert SQL, AtCoder ODS-to-DWD/DWM/DWS SQL task resources, the AtCoder refresh-interval JDBC adapter, Spring wiring, and focused tests. AtCoder DWD/DWM/DWS physical tables are created by `training-data-common` because they follow the shared same-layer warehouse contract; the AtCoder cleaning SQL stays in this vertical module.

## Layer Rules

Dependency direction:

```text
config -> app/domain/infra/web
app -> domain + training-data-common contracts
infra -> domain
web -> app
```

Rules:

- `domain` owns AtCoder ODS records and ports; it must not depend on Spring, JDBC, HTTP, or common web DTOs.
- `app` orchestrates AtCoder use cases and plugs AtCoder adapters into common collection contracts.
- `infra` owns Kenkoooo HTTP access, Jackson parsing, JDBC writers, and ODS deletion SQL.
- AtCoder-specific HTTP endpoints are not currently exposed. Recent-window collection, collection jobs, OJ handle management, purge, and warehouse queries use common HTTP controllers in `training-data-common`.
- Do not put AtCoder source clients, parsers, writers, ODS SQL, or cleaning SQL in the Codeforces or common modules.
- AtCoder DWD/DWM/DWS table DDL belongs in `training-data-common` only while it matches the common same-layer table contract.

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/atcoder/
  app/                 # AtCoder ODS ingest, submission collection, and problem-list collection use cases
  config/              # typed properties, scheduler, and Spring Bean wiring
  domain/              # AtCoder ODS records and source/parser/writer ports
  infra/               # Kenkoooo client, Jackson parser, JDBC writer/purge/refresh interval adapters

src/main/resources/
  db/migration/        # AtCoder ODS Flyway migrations
  sql/dwd/             # AtCoder ODS-to-DWD SQL task
  sql/dwm/             # AtCoder DWD-to-DWM SQL task
  sql/dws/             # AtCoder DWM-to-DWS SQL task
  sql/ods/             # AtCoder ODS upsert SQL
  sql/tasks/           # AtCoder SQL task DAG manifest

src/test/java/com/custacm/platform/trainingdata/atcoder/
  app/                 # collection and ingest orchestration tests
  config/              # typed property default tests
  infra/               # source client, parser, writer, and purge tests
```

## Main File Responsibilities

| File | Layer | Responsibility |
| --- | --- | --- |
| `pom.xml` | build | Declares the AtCoder jar module and its `common-core`, `training-data-common`, Spring JDBC/Web, H2 test, and Spring test dependencies. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderOdsBatchUpsertResult.java` | app | Application result for AtCoder ODS upserts: batch id, table name, written row count, and fetch time. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderOdsIngestService.java` | app | Validates raw Kenkoooo arrays, creates collect batches, parses submission/problem ODS records, writes them through AtCoder writers, and returns upsert metadata. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderSubmissionCollectionAdapter.java` | app | AtCoder implementation on top of the common collection adapter base: pages Kenkoooo `user/submissions` forward with `from_second`, reuses common epoch-second window filtering and boundary conversion, reports history-start reachability when the query starts at `from_second=0`, and writes matched submissions to ODS. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderSubmissionCollectionService.java` | app | AtCoder collector facade implementing the common dispatcher contract; wires common recent-window orchestration with the AtCoder adapter and handle-account resolver. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderProblemListCollectionService.java` | app | Fetches the Kenkoooo problem list through the shared retry/rate-limit executor and upserts it into `ods_atcoder__problem`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderCollectorProperties.java` | config | Typed Kenkoooo client properties: base URL, page size, connect timeout, read timeout, request interval, and max request attempts. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderProblemListCollectorProperties.java` | config | Typed enabled-by-default problem-list properties: schedule enablement, startup bootstrap enablement, empty-table bootstrap guard, cron, and zone. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderProblemListBootstrapRunner.java` | config | Startup bootstrap runner that auto-collects Kenkoooo problem metadata when `ods_atcoder__problem` is empty, logging stable failures without blocking service startup. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderProblemListSchedulingConfig.java` | config | Registers the optional low-frequency problem-list scheduled task and logs boundary failures with a stable error code. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderTrainingDataConfig.java` | config | Registers AtCoder parser, writers, source client, ODS ingest service, submission collector, problem-list collector, ODS purge adapter, refresh interval repository, and configured common warehouse refresh service/handler beans. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderCollectBatch.java` | domain | ODS collect batch identity and fetch time; validates required batch metadata. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderOdsSubmission.java` | domain | ODS record preserving Kenkoooo user submission fields, raw payload, payload hash, batch id, and required-field validation. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderOdsProblem.java` | domain | ODS record preserving Kenkoooo problem-list fields, raw payload, payload hash, batch id, and required-field validation. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderSubmissionSourceClient.java` | domain | Port for fetching Kenkoooo user submission pages by AtCoder user id and `from_second`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderProblemSourceClient.java` | domain | Port for fetching Kenkoooo `resources/problems.json`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderSubmissionPayloadParser.java` | domain | Parser port for converting raw Kenkoooo submission arrays into AtCoder ODS records. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderProblemPayloadParser.java` | domain | Parser port for converting raw Kenkoooo problem arrays into AtCoder ODS records. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderOdsSubmissionWriter.java` | domain | Writer contract for idempotent AtCoder submission ODS upsert. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderOdsProblemWriter.java` | domain | Writer contract for idempotent AtCoder problem-list ODS upsert. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/AtcoderApiException.java` | infra | Runtime exception for Kenkoooo source failures; exposes stable source-client error codes to the common collection failure handler. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/RestClientAtcoderSourceClient.java` | infra | Spring `RestClient` implementation for Kenkoooo submissions and problem-list endpoints with response shape validation. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JacksonAtcoderPayloadParser.java` | infra | Jackson-backed parser for AtCoder submissions/problems; stores raw JSON and computes SHA-256 payload hashes. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderOdsSubmissionWriter.java` | infra | JDBC writer for `ods_atcoder__submission`; loads ODS upsert SQL and writes UTC+8 local `fetched_at`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderOdsProblemWriter.java` | infra | JDBC writer for `ods_atcoder__problem`; loads ODS upsert SQL and writes UTC+8 local `fetched_at`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderOdsDataPurgeRepository.java` | infra | JDBC adapter that deletes all `ods_atcoder__submission` rows for one AtCoder handle by `user_id`. |
| `src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java` | infra | OJ-specific implementation of the common refresh interval repository; derives an AtCoder refresh interval from batch submission dates and existing DWM first-accepted dates touched by the batch. |

## Resource File Responsibilities

| File | Responsibility |
| --- | --- |
| `src/main/resources/db/migration/V019__create_atcoder_ods_tables.sql` | Creates AtCoder ODS landing tables for Kenkoooo user submissions and `resources/problems.json` problem-list items. |
| `src/main/resources/sql/ods/upsert_ods_atcoder__submission.sql` | Idempotent AtCoder submission ODS upsert keyed by `atcoder_submission_id`. |
| `src/main/resources/sql/ods/upsert_ods_atcoder__problem.sql` | Idempotent AtCoder problem-list ODS upsert keyed by `problem_id`. |
| `src/main/resources/sql/dwd/upsert_dwd_atcoder__submission.sql` | Refresh-interval-parameterized ODS-to-DWD transform that maps Kenkoooo submissions to the common DWD contract and enriches problem metadata from AtCoder ODS problem rows. |
| `src/main/resources/sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql` | Refresh-interval-parameterized DWD-to-DWM transform that ranks AtCoder accepted submissions by handle/problem and reloads first accepted rows in the effective interval. |
| `src/main/resources/sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql` | Refresh-interval-parameterized DWM-to-DWS transform that aggregates AtCoder first accepted rows by handle/date/difficulty, currently grouping null difficulty as `UNRATED`. |
| `src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml` | SQL task DAG manifest for AtCoder refresh; declares DWD -> DWM -> DWS task ids, SQL locations, dependencies, descriptions, and per-node timeouts. |

## Test File Responsibilities

| File | Responsibility |
| --- | --- |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderSubmissionCollectionServiceTest.java` | Verifies AtCoder recent-window collection with fake clients: handle resolution, `from_second` paging, epoch-second window filtering including sub-second bounds, collection-state update callbacks, no-match behavior, and ODS write metadata. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderProblemListCollectionServiceTest.java` | Verifies Kenkoooo problem-list fetch and ODS upsert orchestration with fake clients/writers. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderCollectorPropertiesTest.java` | Verifies AtCoder collector and problem-list schedule/bootstrap defaults. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderProblemListBootstrapRunnerTest.java` | Verifies startup problem-list bootstrap runs on empty ODS, skips populated ODS, respects disabled bootstrap, and supports always-bootstrap mode. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JacksonAtcoderPayloadParserTest.java` | Verifies submission/problem parsing, raw payload hashing, and invalid required-field rejection. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderOdsDataPurgeRepositoryTest.java` | Verifies handle-based AtCoder ODS deletion without touching other handles. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderOdsWriterTest.java` | Verifies AtCoder submission/problem ODS writer upserts and stored UTC+8 `fetched_at`. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepositoryTest.java` | Verifies AtCoder batch interval derivation, stale first-accepted date expansion, and empty-batch behavior. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/AtcoderWarehouseSqlTaskTest.java` | Runs AtCoder ODS/DWD/DWM/DWS SQL tasks against H2 data and validates idempotent warehouse output plus stale DWS cleanup. |
| `src/test/java/com/custacm/platform/trainingdata/atcoder/infra/RestClientAtcoderSourceClientTest.java` | Verifies Kenkoooo HTTP request paths/query params, response validation, and transport failure wrapping against a local fake server. |

## Maintenance Rules

- Update this README whenever AtCoder source files, resource files, tests, package layout, table contracts, or responsibilities change.
- Keep ODS field semantics synchronized with `../docs/ods-submission.md`.
- Keep collection design details synchronized with `../docs/atcoder-collection.md`.
- Do not document generated files under `target/`.
