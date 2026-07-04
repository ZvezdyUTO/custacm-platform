# platform-training-data Agent Notes

This module owns the training-data warehouse storage model.

Current state:

- Maven parent module with independent OJ module `training-data-codeforces`, plus runnable `training-data-web`.
- Implemented ODS storage target is `ods_codeforces__submission`.
- Codeforces owns its own HTTP ingress, ingest application service, collect batch type, ODS record, parser, writer, fixture, DDL, upsert SQL, Spring config, and tests.
- `training-data-web` exposes module health/info and OJ-specific ODS batch-upsert endpoints.
- OJ-specific ODS batch-upsert endpoints require the platform `admin` role.
- `training-data-web` includes MySQL runtime driver support and applies `classpath:db/migration` scripts with Flyway.
- `training-data-web` must use the project file logging contract from [../docs/logging.md](../docs/logging.md).
- Submission ODS field mappings and source-access notes live in [docs/ods-submission.md](docs/ods-submission.md).

Rules:

- Keep the current slice focused on ODS warehouse modeling and OJ-specific batch upsert.
- Do not reintroduce DAG, pipeline run state, scheduler, generic task executors, or SQL model execution until there is a concrete downstream query or product workflow.
- Keep each OJ as a vertical Maven module. Shared modules must not own OJ ingress/data organization.
- Keep OJ HTTP controllers thin; orchestration belongs in the OJ-owned app service.
- Do not add a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, or `SourcePlatform` for cross-OJ submission storage. Add OJ-specific records, parsers, writers, fixtures, SQL, and tests instead.
- Do not add DWD/DWS/ADS physical tables until there is a real downstream query or product workflow.
- Test external-source parsing with local fixtures. Do not make default tests depend on live Codeforces availability.
- When changing module boundaries or HTTP behavior, update [../docs/architecture.md](../docs/architecture.md), [../docs/api.md](../docs/api.md), [../docs/agent/context-map.md](../docs/agent/context-map.md), and [../docs/doc-sync-map.tsv](../docs/doc-sync-map.tsv) if routing rules change.
