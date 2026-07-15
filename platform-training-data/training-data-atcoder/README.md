# training-data-atcoder

`training-data-atcoder` is the AtCoder-specific training-data jar. It owns Kenkoooo submission/problem metadata access, payload parsing, AtCoder ODS persistence, purge/refresh adapters and warehouse SQL.

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/atcoder/
  app/     ODS ingest, submission collection and metadata collection
  config/  source properties, bean assembly, bootstrap and schedules
  domain/  AtCoder batches, records and ports
  infra/   REST client, parser and JDBC adapters
src/main/resources/
  db/migration/  AtCoder ODS and problem-model schema history
  sql/           ODS upsert and warehouse manifests/SQL
src/test/
```

## Dependency And Layer Rules

- Depends on `training-data-common` contracts and the `common-core` SQL task runner.
- Contains no HTTP controller. Blog API reaches it through common contracts and `ojName=ATCODER` dispatch.
- Kenkoooo payloads, pagination/error mapping, ODS tables, metadata and cleaning SQL stay in this module.
- A submission collection run creates one `AtcoderCollectBatch`; every filtered source page is upserted immediately with that same batch id, keeping memory proportional to the source page size.
- External-source tests use fixtures, fakes or a local server, never live Kenkoooo services.
- Submission collection, metadata scheduling and startup bootstrap remain disabled by default.
- Metadata bootstrap runs only when explicitly enabled; the default `bootstrapOnlyWhenEmpty=true` prevents unnecessary refills.

## Key Entries

| Path | Responsibility |
| --- | --- |
| `app/AtcoderOdsIngestService.java` | Open submission batches and persist bounded submission, problem and problem-model chunks. |
| `app/AtcoderSubmissionCollectionAdapter.java` | Adapt paged Kenkoooo submissions to the common batch-writer contract. |
| `app/AtcoderProblemListCollectionService.java` | Collect problem and difficulty metadata. |
| `config/AtcoderCollectorProperties.java` | Submission source, paging, retry and timeout configuration. |
| `config/AtcoderProblemListCollectorProperties.java` | Opt-in metadata schedule/bootstrap configuration. |
| `config/AtcoderTrainingDataConfig.java` | Source, writer, collector, purge and refresh bean assembly. |
| `config/AtcoderProblemListBootstrapRunner.java` and `AtcoderProblemListSchedulingConfig.java` | Explicitly enabled metadata startup/schedule entrypoints. |
| `domain/` | AtCoder-specific batch, ODS and source contracts. |
| `infra/RestClientAtcoderSourceClient.java` | Kenkoooo client and response validation. |
| `infra/JacksonAtcoderPayloadParser.java` | JSON-to-ODS parsing. |
| `infra/JdbcAtcoderOds*Writer.java` | Idempotent submission/problem/problem-model writes. |
| `infra/JdbcAtcoderOdsDataPurgeRepository.java` | Purge submissions for one handle. |
| `infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java` | Resolve refresh intervals from valid batches. |
| `src/main/resources/db/migration/` | AtCoder ODS and metadata schema history. |
| `src/main/resources/sql/` | ODS upsert and idempotent DWD/DWM/DWS refresh. |
| `src/test/` | Parser, collector, metadata, source, JDBC and SQL-task tests. |

## Verification

Run from the repository root:

```bash
mvn clean test
```
