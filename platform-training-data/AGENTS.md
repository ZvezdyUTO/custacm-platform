# platform-training-data Agent Notes

This module owns the training-data warehouse storage model.

Current state:

- Maven parent module with independent OJ module `training-data-codeforces`, plus runnable `training-data-web`.
- Implemented Codeforces storage targets are `ods_codeforces__submission`, `dwd_codeforces__submission`, `dwm_codeforces__handle_problem_first_accepted`, and `dws_codeforces__handle_daily_rating_accepted_summary` where DWS is a handle/date wide row with fixed rating count columns.
- Codeforces owns its own HTTP ingress, ingest application service, collect batch type, ODS record, parser, writer, fixture, DDL, ODS upsert SQL, DWD/DWM/DWS SQL task resources, DWD/DWM/DWS query repositories/services, Spring config, and tests.
- Codeforces Java code is organized inside the OJ module as `domain/model`, `domain/value`, `domain/criteria`, `domain/parser`, `domain/repo`, `app/service`, `app/result`, `infra/parser`, `infra/repo`, `web/controller`, `web/response`, and `config`.
- `training-data-web` exposes module health/info and OJ-specific ODS batch-upsert endpoints.
- OJ-specific ODS batch-upsert endpoints require the platform `admin` role.
- `training-data-web` includes MySQL runtime driver support and applies `classpath:db/migration` scripts with Flyway.
- Training-data warehouse `datetime` and date-grain fields use UTC+8 (`Asia/Shanghai`) semantics; Codeforces epoch seconds are shifted to UTC+8 before DWD/DWM/DWS day-grain processing.
- `training-data-web` must use the project file logging contract from [../docs/logging.md](../docs/logging.md).
- Submission warehouse table contracts, SQL task order, and source-access notes live in [docs/ods-submission.md](docs/ods-submission.md).

Rules:

- Keep the current slice focused on Codeforces warehouse modeling, OJ-specific batch upsert, SQL task resources, and narrowly scoped downstream query services backed by existing warehouse tables.
- Do not reintroduce DAG, pipeline run state, scheduler, generic task executors, or SQL model execution until there is a concrete downstream refresh workflow.
- Keep each OJ as a vertical Maven module. Shared modules must not own OJ ingress/data organization.
- Keep OJ HTTP controllers thin; orchestration belongs in the OJ-owned app service.
- Do not add a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, or `SourcePlatform` for cross-OJ submission storage. Add OJ-specific records, parsers, writers, fixtures, SQL, and tests instead.
- Do not add ADS physical tables or cross-OJ DWD/DWM/DWS tables until there is a real downstream query or product workflow.
- Keep Codeforces DWD/DWM/DWS transforms as idempotent SQL resources. Do not add Java row-by-row transformation logic.
- Keep warehouse query code as read-side access to existing DWD/DWM/DWS tables. Put cross-row business composition in app services, not JDBC repositories.
- Keep query request objects in `domain/criteria`; they may validate required query invariants and normalize values, while application services should remain responsible for use-case composition.
- Keep new training-data warehouse query boundaries aligned to the UTC+8 warehouse convention; do not introduce UTC date-grain fields without an explicit product decision.
- Test external-source parsing with local fixtures. Do not make default tests depend on live Codeforces availability.
- When changing module boundaries or HTTP behavior, update [../docs/architecture.md](../docs/architecture.md), [../docs/api.md](../docs/api.md), [../docs/agent/context-map.md](../docs/agent/context-map.md), and [../docs/doc-sync-map.tsv](../docs/doc-sync-map.tsv) if routing rules change.
