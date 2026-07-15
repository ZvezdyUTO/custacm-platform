# training-data-codeforces

`training-data-codeforces` is the Codeforces-specific training-data jar. It owns `user.status` access, payload parsing, Codeforces ODS persistence, purge/refresh adapters and warehouse SQL.

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/codeforces/
  app/     ODS ingest and common collection adapter
  config/  source properties and bean assembly
  domain/  Codeforces batches, records and ports
  infra/   REST client, parser and JDBC adapters
src/main/resources/
  db/migration/  Codeforces ODS schema history
  fixtures/      local source fixtures
  sql/           ODS upsert and warehouse manifests/SQL
src/test/
```

## Dependency And Layer Rules

- Depends on `training-data-common` contracts and the `common-core` SQL task runner.
- Contains no HTTP controller. Blog API reaches it through common contracts and `ojName=CODEFORCES` dispatch.
- Codeforces payloads, pagination/error mapping, ODS tables and cleaning SQL stay in this module.
- A collection run creates one `CodeforcesCollectBatch`; every filtered `user.status` page is upserted immediately with that same batch id, keeping memory proportional to the configured source page size.
- Username ownership, handle bindings and collection cursors come from common normalized repositories; this module does not read the legacy JSON identity table.
- External-source tests use fixtures, fakes or a local server, never the live Codeforces API.
- Automatic collection is configured by Blog API and remains disabled by default.

## Key Entries

| Path | Responsibility |
| --- | --- |
| `app/CodeforcesOdsSubmissionIngestService.java` | Open a logical submission batch and parse/persist each bounded chunk. |
| `app/CodeforcesSubmissionCollectionAdapter.java` | Adapt paged `user.status` collection to the common batch-writer contract. |
| `config/CodeforcesCollectorProperties.java` | Source endpoint, paging, retry and timeout configuration. |
| `config/CodeforcesTrainingDataConfig.java` | Source, writer, collector, purge and refresh bean assembly. |
| `domain/` | Codeforces-specific batch, ODS and source contracts. |
| `infra/RestClientCodeforcesSubmissionSourceClient.java` | Codeforces API client and response validation. |
| `infra/JacksonSubmissionPayloadParser.java` | JSON-to-ODS parsing. |
| `infra/JdbcCodeforcesOdsSubmissionWriter.java` | Idempotent ODS upsert. |
| `infra/JdbcCodeforcesOdsDataPurgeRepository.java` | Purge submissions for one handle. |
| `infra/JdbcCodeforcesWarehouseRefreshIntervalRepository.java` | Resolve refresh intervals from valid batches. |
| `src/main/resources/db/migration/` | Codeforces ODS schema history. |
| `src/main/resources/sql/` | ODS upsert and idempotent DWD/DWM/DWS refresh. |
| `src/test/` | Parser, collector, source, JDBC and SQL-task tests. |

Team submissions are attributed to a collected handle only when that handle occurs in `author.members`; ODS and DWD therefore use submission-plus-handle grain.

## Verification

Run from the repository root:

```bash
mvn clean test
```
