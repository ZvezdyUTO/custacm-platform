# platform-training-data Agent Notes

This module owns the training-data warehouse storage model.

Current state:

- Maven parent module with independent OJ module `training-data-codeforces`, plus runnable `training-data-web`.
- Implemented Codeforces storage targets are `ods_codeforces__submission`, `dwd_codeforces__submission`, `dwm_codeforces__handle_problem_first_accepted`, and `dws_codeforces__handle_daily_rating_accepted_summary`.
- Implemented Codeforces handle-account storage target is `codeforces_handle_account`, keyed by `student_identity` with a unique immutable Codeforces handle.
- Codeforces owns its own HTTP ingress, ingest application service, `studentIdentity`-scoped recent-lookback submission collector with bounded source request timeout/retry, collect batch type, ODS record, parser, writer, handle-account mapping, fixture, DDL, ODS upsert SQL, DWD/DWM/DWS SQL task resources, SQL task manifest, Spring config, and tests.
- `training-data-web` exposes module health/info, OJ-specific ODS batch-upsert endpoints under `/api/training-data/admin/**`, Codeforces admin recent-lookback collection by `studentIdentity`, Codeforces admin handle-account write endpoints, Codeforces admin warehouse refresh, public Codeforces handle lookup, and public Codeforces DWD/DWM/DWS query endpoints.
- OJ-specific ODS batch-upsert endpoints require the platform `admin` role and must stay under the admin URL tier.
- Codeforces handle-account creation and identity migration require the platform `admin` role. Public Codeforces handle lookup by `studentIdentity` is a guest endpoint and must not parse JWTs.
- HTTP authorization follows [../docs/authorization.md](../docs/authorization.md): `/admin/**` is admin-only, `/player/**` is player/admin, and guest endpoints must not parse JWTs.
- `training-data-web` includes MySQL runtime driver support and applies `classpath:db/migration` scripts with Flyway.
- `training-data-web` must use the project file logging contract from [../docs/logging.md](../docs/logging.md).
- Submission warehouse table contracts, SQL task order, admin refresh semantics, and source-access notes live in [docs/ods-submission.md](docs/ods-submission.md).

Rules:

- Keep the current slice focused on Codeforces warehouse modeling, Codeforces handle-account mapping, OJ-specific batch upsert, recent-lookback submission collection, SQL task resources, and the current admin-triggered SQL task refresh path.
- Do not add persistent pipeline run state, cross-OJ DAG orchestration, or ADS physical tables until there is a concrete downstream query or product workflow. Codeforces has a disabled-by-default Spring scheduled trigger that calls the same admin collection use case; it must not grow into a pipeline scheduler.
- Keep each OJ as a vertical Maven module. Shared modules must not own OJ ingress/data organization.
- Keep OJ HTTP controllers thin; orchestration belongs in the OJ-owned app service.
- Codeforces handle-account identity migration only changes the `student_identity` key in `codeforces_handle_account`; it does not modify auth accounts and must not change the stored Codeforces handle.
- Do not add a unified `OdsSubmissionRecord`, `OdsSubmissionWriter`, or `SourcePlatform` for cross-OJ submission storage. Add OJ-specific records, parsers, writers, fixtures, SQL, and tests instead.
- Do not add ADS physical tables or cross-OJ DWD/DWM/DWS tables until there is a real downstream query or product workflow.
- Keep Codeforces DWD/DWM/DWS transforms as idempotent SQL resources. Do not add Java row-by-row transformation logic.
- Test external-source parsing and collection with local fixtures or fake clients. Do not make default tests depend on live Codeforces availability.
- When changing module boundaries or HTTP behavior, update [../docs/architecture.md](../docs/architecture.md), [../docs/api.md](../docs/api.md), [../docs/agent/context-map.md](../docs/agent/context-map.md), and [../docs/doc-sync-map.tsv](../docs/doc-sync-map.tsv) if routing rules change.
