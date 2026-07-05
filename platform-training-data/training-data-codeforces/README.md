# training-data-codeforces

`training-data-codeforces` is the vertical Codeforces warehouse slice.

It owns the Codeforces ODS HTTP ingress, ingest orchestration, source parser, ODS writer contract and JDBC implementation, table migrations, fixture data, and SQL task resources for DWD/DWM/DWS derived tables.

## Directory Layout

```text
training-data-codeforces/
  src/main/java/com/custacm/platform/trainingdata/codeforces/app/
  src/main/java/com/custacm/platform/trainingdata/codeforces/config/
  src/main/java/com/custacm/platform/trainingdata/codeforces/domain/
  src/main/java/com/custacm/platform/trainingdata/codeforces/infra/
  src/main/java/com/custacm/platform/trainingdata/codeforces/web/
  src/main/resources/db/migration/
  src/main/resources/fixtures/codeforces/
  src/main/resources/sql/
  src/test/java/com/custacm/platform/trainingdata/codeforces/
```

## Dependency And Layer Rules

- Codeforces-specific records, parsers, writers, SQL, fixtures, and HTTP ingress stay in this module.
- Do not introduce a shared cross-OJ submission model until there is a concrete downstream product query.
- The HTTP ingest endpoint is admin-only and must stay under `/api/training-data/admin/**`.
- Keep controllers thin; parsing and write orchestration belong in the app and infra layers.

## File Responsibilities

- `app/CodeforcesOdsSubmissionIngestService.java` - orchestrates parsing and ODS batch upsert.
- `app/CodeforcesOdsBatchUpsertResult.java` - app-layer result for ODS upsert.
- `config/CodeforcesTrainingDataConfig.java` - Spring wiring for parser and JDBC writer.
- `domain/CodeforcesCollectBatch.java` - collect batch context.
- `domain/CodeforcesOdsSubmission.java` - Codeforces ODS submission record.
- `domain/CodeforcesOdsSubmissionWriter.java` - ODS writer port.
- `infra/CodeforcesSubmissionParser.java` - converts raw Codeforces JSON into ODS records.
- `infra/JdbcCodeforcesOdsSubmissionWriter.java` - JDBC implementation for idempotent ODS upsert.
- `web/CodeforcesOdsSubmissionIngestController.java` - admin HTTP endpoint for batch upsert.
- `web/CodeforcesOdsBatchUpsertResponse.java` - HTTP response DTO.
- `db/migration/` - ODS/DWD/DWM/DWS table DDL.
- `sql/` - idempotent SQL task resources for derived tables.
- `fixtures/codeforces/` - local fixture data for repeatable tests and local replay.
