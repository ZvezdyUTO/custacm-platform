# training-data-common

This module contains reusable training-data application/domain/infra/web code that is shared by OJ vertical modules.

Current scope: OJ handle-account application services, per-OJ handle collection state tracking, per-OJ difficulty bucket policies, common DWD/DWM/DWS query services and contracts, student-data purge orchestration, reusable JDBC repositories for same-layer OJ warehouse tables, common same-layer DWD/DWM/DWS table DDL migrations, submission-collection orchestration, shared collection adapter failure handling, epoch-second window boundary normalization, required-text argument validation, OJ collector dispatch, generic warehouse refresh interval/service/handler contracts, OJ warehouse refresh dispatch, process-local collection-job orchestration, shared SQL task runner wiring, common Spring bean configuration, common HTTP controllers/DTOs, and scheduling helpers. It does not own OJ source clients, OJ ODS records, OJ payload parsers, OJ ODS writers, OJ-specific ODS table migrations, OJ-specific source parsing, OJ-specific refresh SQL, or OJ SQL task manifests.

## Layer Rules

- OJ modules provide source clients, source pagination adapters, OJ-specific timestamp extraction, source failure error-code implementations, ODS records/parsers/writers, and OJ-specific ODS purge adapters.
- This module owns OJ-generic app/domain/infra/web code that works against `ojName`, `studentIdentity`, handle maps, collection interfaces, and same-layer warehouse table contracts.
- This module may own reusable collection envelopes such as handle normalization, hashed handle logging, failed handle outcome construction, and collector batch id prefixing; it must not own OJ-specific source access, payload parsing, or ODS storage contracts.
- This module may own small non-business support helpers shared by training-data modules, such as required-text argument validation.
- This module may own common same-layer DWD/DWM/DWS physical table DDL migrations when a table follows the shared warehouse query contract.
- Common HTTP controllers must not depend on an OJ module directly; OJ modules provide app services and callbacks through common contracts.
- This module must not own OJ-specific payload schemas, OJ-specific ODS table contracts, OJ source clients, or OJ-specific source parsing.
- OJ warehouse refresh SQL, manifest resources, and JDBC interval derivation stay in the OJ module; this module owns the generic interval value/repository port, refresh service, SQL-task refresh handler, and dispatcher used by collection jobs and scheduled collection.

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/common/
  app/              # generic OJ app services and app-layer result records
    account/        # OJ handle-account use cases and app errors
    purge/          # student-scoped OJ training-data deletion use case
    query/          # DWD/DWM/DWS read use cases and query results
    warehouse/      # SQL-task-backed warehouse refresh use case shared by OJ modules
  collector/        # generic OJ submission collection orchestration
    dispatch/       # OJ collector registry and dispatcher contracts
    job/            # generic in-process collection job state and service
    result/         # generic collection result records and status enums
  config/           # common Spring bean wiring for OJ-generic repositories, services, dispatchers, jobs, and SQL tasks
  domain/           # OJ-generic domain models, criteria, repository ports, and values
    oj/             # OJ handle-account, DWD/DWM/DWS, purge, and table-selection contracts
  infra/            # OJ-generic infrastructure implementations
    oj/             # JDBC implementations for handle accounts and same-layer warehouse tables
  scheduler/        # generic scheduled-collection Spring wiring
  support/          # small non-business training-data support helpers
  web/              # OJ-generic HTTP controllers, request DTOs, response DTOs, and exception handlers
    account/        # OJ handle-account HTTP endpoints
    collector/      # recent-window collection and collection-job HTTP endpoints
    purge/          # student-data purge HTTP endpoint
    query/          # public same-layer warehouse query HTTP endpoints

src/main/resources/db/migration/
                    # common same-layer DWD/DWM/DWS Flyway migrations

src/test/java/com/custacm/platform/trainingdata/common/
  app/              # focused tests for generic app services
  collector/        # focused tests for collection orchestration helpers
    dispatch/       # focused tests for OJ collector dispatch
    job/            # focused tests for collection job helpers
  domain/           # domain model and criteria tests
  infra/            # JDBC repository tests
  scheduler/        # focused tests for scheduled-collection wiring
  support/          # focused tests for shared support helpers
  web/              # focused tests for common HTTP controllers and response mapping

src/test/resources/db/migration/
                    # H2-compatible migration copies used by common JDBC tests
```

## Main File Responsibilities

| File | Responsibility |
| --- | --- |
| `pom.xml` | Declares the shared training-data jar module and its `common-core`, Jackson, Spring context/web/JDBC/transaction, SLF4J, H2 test, and Spring test dependencies. |
| `src/main/java/com/custacm/platform/trainingdata/common/config/CommonTrainingDataConfig.java` | Registers OJ-generic Spring beans: shared SQL task runner, handle-account/query/purge repositories and services, collection dispatcher, warehouse refresh dispatcher, single-thread collection job executor, and process-local collection job service. |
| `src/main/java/com/custacm/platform/trainingdata/common/support/Texts.java` | Shared required-text argument validator for training-data modules; returns trimmed text and supports caller-provided runtime exception factories. |
| `src/main/resources/db/migration/V020__create_atcoder_warehouse_tables.sql` | Creates AtCoder same-layer DWD/DWM/DWS tables that match the common warehouse query contract; it does not define any AtCoder cleaning or refresh SQL. |
| `src/main/resources/db/migration/V021__add_oj_handle_account_collection_states.sql` | Adds nullable `oj_handle_account.collection_states_json`, a per-OJ JSON map for history-left-edge and collector execution-time state. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/account/OjHandleAccountException.java` | App-layer OJ handle-account exception with stable error codes for invalid requests, missing accounts, duplicate identities, and duplicate handles. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/account/OjHandleAccountService.java` | OJ handle-account use cases: create `studentIdentity + handles` maps, list all bindings, resolve one requested OJ handle through the repository, update `studentIdentity`, merge optional new handles, update the automatic-collection flag, and mark per-OJ handle collection state after successful collection. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/purge/OjStudentDataPurgeException.java` | App-layer exception for invalid generic OJ student-data purge requests. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/purge/OjStudentDataPurgeService.java` | Generic student-data purge use case: normalizes `studentIdentity` and required `ojName`, resolves that OJ handle, runs the OJ-specific ODS purge adapter plus common DWD/DWM/DWS purge in one transaction, and keeps handle-account bindings. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/OjAcceptedSummaryQueryService.java` | Generic DWS read use case: resolves `studentIdentity + ojName` to a handle, applies rating/date filtering and rating-bucket aggregation, and returns the app-layer summary report. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/OjFirstAcceptedProblemQueryService.java` | Generic DWM read use cases: resolves personal `studentIdentity + ojName` queries to handle criteria, counts and pages first-accepted rows, maps problem-query handles back to identities for the requested OJ, and returns personal or problem first-accepted reports. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/OjSubmissionQueryService.java` | Generic DWD read use cases: resolves personal `studentIdentity + ojName` queries to handle criteria, counts and pages submission rows, maps problem-query handles back to identities for the requested OJ, and returns submission reports. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/warehouse/OjWarehouseRefreshService.java` | Generic warehouse refresh use case: normalizes `batchId` and optional resume task id, asks the OJ-provided interval repository for the effective UTC+8 date interval, then executes the configured SQL task manifest with shared parameters. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjAcceptedSummaryReport.java` | App-layer DWS report for one identity and resolved handle: rating totals and overall total. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjHandleFirstAcceptedProblemReport.java` | App-layer DWM personal report with identity, resolved handle, total accepted problem count, exact pagination metadata, and first-accepted problem items. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjHandleSubmissionReport.java` | App-layer DWD personal report with identity, resolved handle, pagination metadata, and submission detail items. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjProblemFirstAcceptedHandleReport.java` | App-layer DWM problem report with accepted handle count, exact pagination metadata, and identity/handle first-accepted rows. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjProblemSubmissionReport.java` | App-layer DWD problem report with requested problem key, pagination metadata, and submission detail items. |
| `src/main/java/com/custacm/platform/trainingdata/common/app/query/result/OjSubmissionItem.java` | App-layer DWD submission item with identity, handle, problem metadata, verdict, resource usage, source URL, and UTC+8 submitted time. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjHandleAccountCollectionHandleResolver.java` | Shared collection handle resolver backed by OJ handle accounts: resolves one `studentIdentity + ojName` handle, lists `needCollect=true` handles for scheduled collection, and marks successful per-OJ handle collection states. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjSubmissionCollectionService.java` | Generic recent-window collection orchestration: computes `[now - lookback, now)`, resolves configured or single-student handles through a resolver, prevents overlapping runs in the same JVM, aggregates per-handle outcomes, delegates OJ-specific writes, and writes successful handle collection states after the run is safe to record. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjSubmissionCollectionAdapter.java` | Adapter contract implemented by OJ modules for handle collection and ODS batch writes. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/AbstractOjSubmissionCollectionAdapter.java` | Base class for OJ-specific collection adapters: validates handles, wraps per-handle runtime failures, logs hashed handles with stable error codes, builds success/failure outcomes, and provides collector batch id prefixes. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjCollectionSourceFailure.java` | Contract for OJ source exceptions that expose a stable collector error code without making common code depend on OJ-specific exception classes. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjCollectionHandleResolver.java` | Adapter contract for resolving `studentIdentity + ojName` to a handle, listing auto-collection handles for one OJ, and optionally marking successful handle collection state. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjCollectionRequestExecutor.java` | Shared source-request retry and inter-request rate-limiting helper. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjEpochSeconds.java` | Shared conversion from `Instant` window boundaries to integer epoch-second bounds for source APIs whose submission timestamps have second precision. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/OjSubmissionWindowFilter.java` | Shared timestamp-window filtering for source pages; also reports the page maximum epoch second and the newest-first older-page stop signal used by Codeforces-style pagination. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/dispatch/OjRecentSubmissionCollector.java` | OJ-facing recent-submission collector facade contract used by the dispatcher, HTTP controller, scheduler, and job service without depending on OJ modules. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/dispatch/OjSubmissionCollectionDispatcher.java` | Selects a registered OJ collector by normalized `ojName`, applies the configured default OJ for blank requests, and reports unsupported OJ collection requests as invalid. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/config/OjCollectorSchedulingProperties.java` | Typed OJ collector properties: batch-job per-identity interval plus scheduled-collection `ojName`, enablement, cron, zone, and rolling lookback duration. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobService.java` | Generic in-process batch collection job service: starts one active collection job at a time, waits the configured interval between identities, optionally calls an OJ-provided refresh handler, exposes job snapshots, and retains recent completed jobs in memory for frontend polling. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshHandler.java` | Optional warehouse refresh handler contract used by collection jobs and scheduled collection after a successful ODS batch. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/SqlTaskOjWarehouseRefreshHandler.java` | Generic SQL-task-backed warehouse refresh handler: exposes one configured OJ name and maps SQL task run status into collection-job refresh status. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcher.java` | Selects a warehouse refresh handler by normalized OJ name and reports unsupported refresh requests as failed job refresh results. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobSnapshot.java` | Immutable job-level snapshot returned to HTTP adapters: job id, aggregate status/counts, batch ids, timestamps, message, and per-identity item snapshots. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobItem.java` | Per-identity collection job item: pending/running/success/failure state, resolved handle, ODS batch/write counts, source counts, message, and optional refresh result. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobStatus.java` | Generic job aggregate status enum: `RUNNING`, `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILED`. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobItemStatus.java` | Generic per-identity job status enum: `PENDING`, `RUNNING`, `SUCCESS`, or `FAILED`. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobRefreshResult.java` | Generic optional refresh result returned by an OJ refresh handler. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobRefreshStatus.java` | Generic optional refresh status enum: not requested, no batch, success, or failed. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjSubmissionCollectionResult.java` | Generic collection-run result: window, aggregate counts, optional batch metadata, message, and per-handle results. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjSubmissionCollectionStatus.java` | Generic collection-run status enum: `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`, or `SKIPPED`. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjSubmissionCollectionHandleResult.java` | Generic per-handle collection outcome with fetched/matched counts and sanitized failure details. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjSubmissionCollectionHandleStatus.java` | Generic per-handle status enum: `SUCCESS` or `FAILED`. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjSubmissionCollectionWriteResult.java` | Generic ODS write metadata returned by an OJ adapter. |
| `src/main/java/com/custacm/platform/trainingdata/common/collector/result/OjHandleCollectionOutcome.java` | Generic adapter return value for one handle: per-handle result, matched raw submissions, and whether the OJ adapter proved that the historical left edge has been reached. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjAcceptedSummaryCriteria.java` | DWS query criteria: handle, optional UTC+8 accepted date range, and optional difficulty/rating bounds. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjHandleFirstAcceptedProblemCriteria.java` | DWM handle query criteria: handle, optional UTC+8 first-accepted time range, optional difficulty/rating bounds, and pagination. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjHandleSubmissionCriteria.java` | DWD handle query criteria: handle, optional UTC+8 submitted time range, optional difficulty/rating bounds, and pagination. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjProblemFirstAcceptedHandleCriteria.java` | DWM problem query criteria: problem key, optional UTC+8 first-accepted time range across handles, and pagination. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjProblemSubmissionCriteria.java` | DWD problem query criteria: problem key, optional UTC+8 submitted time range across handles, and pagination. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjDailyRatingAcceptedSummary.java` | DWS read model for one handle/date row with fixed difficulty/rating accepted-problem counts. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjFirstAcceptedProblem.java` | DWM read model for one handle's first accepted submission for one problem. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjHandleAccount.java` | OJ handle-account model keyed by platform `studentIdentity`, with uppercase OJ-name to handle map, automatic-collection flag, per-OJ collection states, and audit timestamps. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjHandleCollectionState.java` | Per-OJ handle collection state: whether collection reached the historical left edge and the collector execution instant for the last successful handle collection. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjStudentDataPurgeResult.java` | Generic deletion-count result for student-scoped OJ purge, including optional requested OJ, handles, per-OJ deletion counts, aggregate counts, and total rows. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjSubmission.java` | DWD read model for one cleaned same-layer OJ submission row. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjWarehouseRefreshInterval.java` | Common inclusive UTC+8 date interval passed into OJ DWD/DWM/DWS SQL refresh tasks. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjAcceptedSummaryRepository.java` | Repository port for DWS accepted-summary queries. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjFirstAcceptedProblemRepository.java` | Repository port for DWM first-accepted counts and paged queries by handle or problem key. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjHandleAccountRepository.java` | Repository port for OJ handle-account full-list, identity lookup, OJ-handle lookup, inserts, identity/automatic-collection updates, and per-OJ collection-state persistence. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjOdsDataPurgeRepository.java` | OJ-specific ODS purge port; each implementation declares its OJ name and deletes that OJ's ODS rows by handle. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjSubmissionRepository.java` | Repository port for DWD submission counts and paged queries by handle or problem key. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjWarehouseDataPurgeRepository.java` | Repository port for deleting same-layer DWD/DWM/DWS rows by requested OJ name and handle. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/repo/OjWarehouseRefreshIntervalRepository.java` | Repository port implemented by each OJ module to derive the effective refresh interval for one ODS batch with OJ-specific SQL. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/value/OjNames.java` | Uppercase OJ-name constants and normalization for handle-map keys such as `CODEFORCES` and `ATCODER`. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/value/OjDifficultyBucket.java` | One rated difficulty bucket with a stable DWS key and inclusive numeric bounds. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/value/OjDifficultyBucketPolicy.java` | Per-OJ bucket policy that maps optional numeric rating filters to rated DWS bucket keys and controls when `UNRATED` is included. |
| `src/main/java/com/custacm/platform/trainingdata/common/domain/oj/value/OjDifficultyBucketPolicies.java` | Default difficulty bucket registry: Codeforces exact 100-point rating keys, AtCoder Problems range buckets, and the shared `UNRATED` key. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/account/JdbcOjHandleAccountRepository.java` | JDBC implementation for `oj_handle_account`; stores `handles_json` and `collection_states_json`, lists accounts in stable identity order, finds accounts by identity or OJ handle, inserts bindings, updates identity/handles/`need_collect`, and updates per-OJ collection states independently. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjAcceptedSummaryRepository.java` | JDBC implementation for DWS summary queries; selects same-layer DWS tables by normalized OJ name, builds optional predicates, and maps rows. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjFirstAcceptedProblemRepository.java` | JDBC implementation for DWM first-accepted counts and paged queries; selects same-layer DWM tables by normalized OJ name, builds optional predicates, and maps rows. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjSubmissionRepository.java` | JDBC implementation for DWD submission counts and paged queries; selects same-layer DWD tables by normalized OJ name, builds optional predicates, and maps rows. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/warehouse/JdbcOjWarehouseDataPurgeRepository.java` | Transactional JDBC adapter that deletes all DWD, DWM, and DWS rows for one requested OJ handle. |
| `src/main/java/com/custacm/platform/trainingdata/common/infra/oj/repo/warehouse/OjWarehouseTableNames.java` | Builds whitelisted same-layer DWD/DWM/DWS table names from normalized OJ names. |
| `src/main/java/com/custacm/platform/trainingdata/common/scheduler/OjScheduledSubmissionCollectionService.java` | Minimal collection service contract used by common HTTP and scheduled collection wiring so common code does not depend on any OJ module. |
| `src/main/java/com/custacm/platform/trainingdata/common/scheduler/OjCollectorSchedulingConfig.java` | Spring scheduling adapter; registers enabled OJ schedule entries from configuration, invokes the configured OJ collection service by cron and `ojName`, refreshes the selected OJ warehouse through the shared refresh dispatcher when the collection returns a batch, and logs scheduled boundary failures with a stable error code. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/OjHandleAccountController.java` | HTTP endpoints for admin create, public full-list, and admin identity/handles/automatic-collection-flag updates for OJ handle accounts. Public GET returns a `studentIdentity -> account` map because the handle set is small. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/OjHandleAccountExceptionHandler.java` | Maps OJ handle-account business errors to JSON error responses with `400`, `404`, or `409`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/request/ChangeOjHandleIdentityRequest.java` | HTTP request DTO for migrating an OJ handle account from one `studentIdentity` to another, optionally merging new handles, and optionally changing whether it needs automatic collection. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/request/CreateOjHandleAccountRequest.java` | HTTP request DTO for creating a `studentIdentity + handles` binding, where `handles` is an uppercase OJ-name to handle map. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/response/OjHandleAccountErrorResponse.java` | HTTP error response DTO for OJ handle-account business failures. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/response/OjHandleAccountResponse.java` | HTTP response DTO for OJ handle-account data: `studentIdentity`, uppercase `handles` map, `needCollect`, and uppercase per-OJ `collectionStates` map. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/account/response/OjHandleCollectionStateResponse.java` | HTTP response DTO for one OJ handle collection state: `historyStartReached` and `lastCollectedAt`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/OjSubmissionCollectionController.java` | Admin HTTP endpoints for synchronous one-identity recent-window collection plus in-process batch collection jobs: start job and list retained jobs. Existing routes keep the `/codeforces` path while request bodies can pass `ojName`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/OjSubmissionCollectionExceptionHandler.java` | Maps admin submission collection invalid requests to `400` JSON responses and logs stable request error codes. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/request/OjSubmissionCollectionRequest.java` | HTTP request DTO for admin recent-lookback collection: required `studentIdentity`, positive `lookbackHours`, and optional `ojName`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/request/OjSubmissionCollectionJobStartRequest.java` | HTTP request DTO for starting a batch collection job: required `studentIdentities`, positive `lookbackHours`, optional `refreshWarehouse`, and optional `ojName`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/response/OjSubmissionCollectionHandleResponse.java` | HTTP response DTO for one handle's collection outcome. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/response/OjSubmissionCollectionJobItemResponse.java` | HTTP response DTO for one identity inside a collection job, including OJ name, collection status, batch/write counts, message, and refresh result. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/response/OjSubmissionCollectionJobResponse.java` | HTTP response DTO for collection job snapshots returned by start/list endpoints. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/collector/response/OjSubmissionCollectionResponse.java` | HTTP response DTO for admin recent-lookback collection OJ name, aggregate status, computed window, ODS write metadata, and per-handle outcomes. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/purge/OjStudentDataPurgeController.java` | Admin HTTP endpoint for `DELETE /api/training-data/admin/students/{studentIdentity}/oj-data`; requires `ojName` and returns aggregate plus per-OJ deletion counts. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/purge/OjStudentDataPurgeExceptionHandler.java` | Maps generic student-data purge request errors to JSON `400` responses. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/purge/response/OjStudentDataPurgeErrorResponse.java` | HTTP error response DTO for student-data purge request failures. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/purge/response/OjStudentDataPurgeResponse.java` | HTTP response DTO for student-data purge aggregate and per-OJ deletion counts. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/OjWarehouseQueryController.java` | Public guest HTTP endpoints for DWS accepted summary, DWD submission reports, and DWM first-accepted reports. Existing routes keep the `/codeforces` path while single-user/problem queries accept `ojName`. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjAcceptedSummaryResponse.java` | HTTP response DTO for public DWS accepted-summary reports. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjProblemFirstAcceptedHandleReportResponse.java` | HTTP response DTO for public DWM problem first-accepted handle reports with exact pagination metadata. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjProblemSubmissionReportResponse.java` | HTTP response DTO for public DWD problem submission reports with exact pagination metadata. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjStudentFirstAcceptedProblemReportResponse.java` | HTTP response DTO for public DWM student first-accepted problem reports with exact pagination metadata. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjStudentSubmissionReportResponse.java` | HTTP response DTO for public DWD student submission reports with exact pagination metadata. |
| `src/main/java/com/custacm/platform/trainingdata/common/web/query/response/OjSubmissionItemResponse.java` | Shared HTTP response DTO for DWD submission detail items. |

## Test File Responsibilities

| File | Responsibility |
| --- | --- |
| `src/test/java/com/custacm/platform/trainingdata/common/app/account/OjHandleAccountServiceTest.java` | Verifies OJ handle-account creation, duplicate checks, repository-backed handle lookup, full-list reads, identity migration, handle merging, `needCollect` updates, collection-state marking through the dedicated repository method, and state reset when an OJ handle changes. |
| `src/test/java/com/custacm/platform/trainingdata/common/app/purge/OjStudentDataPurgeServiceTest.java` | Verifies student-data purge normalization, requested-OJ deletion, missing bindings, and invalid-request errors. |
| `src/test/java/com/custacm/platform/trainingdata/common/app/query/OjAcceptedSummaryQueryServiceTest.java` | Verifies DWS query service handle resolution, rating filtering, aggregation, and unbound identity handling. |
| `src/test/java/com/custacm/platform/trainingdata/common/app/query/OjWarehouseQueryServiceTest.java` | Verifies DWD/DWM query services resolve identities, delegate criteria, count/page rows, map handles back to identities, reject unbound handles, and return app-layer reports. |
| `src/test/java/com/custacm/platform/trainingdata/common/app/warehouse/OjWarehouseRefreshServiceTest.java` | Verifies generic warehouse refresh manifest selection, trimmed request parameters, UTC+8 date SQL parameters, blank batch rejection, and OJ-specific missing-interval message handling. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/AbstractOjSubmissionCollectionAdapterTest.java` | Verifies shared adapter handle trimming, success aggregation, source error-code passthrough, generic failure fallback, blank-handle rejection, and collector batch prefix formatting. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/OjSubmissionCollectionServiceTest.java` | Verifies common handle normalization, window computation, aggregation, batch write delegation, successful handle collection-state marking, single-student handle resolution, and in-JVM overlap skipping. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/OjCollectionRequestExecutorTest.java` | Verifies retry exhaustion and rate-limiting sleeps between source requests. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/OjEpochSecondsTest.java` | Verifies exact and sub-second `Instant` boundary conversion to source epoch seconds. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/OjSubmissionWindowFilterTest.java` | Verifies generic timestamp-window filtering, sub-second window bounds, page maximum epoch seconds, older-page detection, and invalid page rejection. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/dispatch/OjSubmissionCollectionDispatcherTest.java` | Verifies blank requests use the configured default OJ, explicit OJ names dispatch to the matching collector, and unsupported OJs are rejected. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/job/OjSubmissionCollectionJobServiceTest.java` | Verifies in-process collection job start/list/get behavior, active-job reuse, configured inter-identity waiting, aggregate counts, per-identity item mapping, optional refresh aggregation, and missing-job handling. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcherTest.java` | Verifies warehouse refresh dispatch by normalized OJ name, unsupported-OJ failure results, duplicate-handler rejection, and invalid handler names. |
| `src/test/java/com/custacm/platform/trainingdata/common/collector/job/SqlTaskOjWarehouseRefreshHandlerTest.java` | Verifies SQL-task refresh handler OJ-name normalization, success/failure status mapping, and delegated validation. |
| `src/test/java/com/custacm/platform/trainingdata/common/support/TextsTest.java` | Verifies shared required-text validation, trimming, default exceptions, and caller-provided exception factories. |
| `src/test/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjAcceptedSummaryCriteriaTest.java` | Verifies DWS criteria field pass-through and default helper behavior. |
| `src/test/java/com/custacm/platform/trainingdata/common/domain/oj/criteria/OjWarehouseCriteriaTest.java` | Verifies DWD/DWM criteria field pass-through, pagination fields, and default helper behavior. |
| `src/test/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjHandleAccountTest.java` | Verifies OJ handle-account required fields, uppercase OJ-name normalization, explicit `needCollect`, collection-state defaults, and timestamp pass-through. |
| `src/test/java/com/custacm/platform/trainingdata/common/domain/oj/model/OjWarehouseRefreshIntervalTest.java` | Verifies common refresh interval required dates and inclusive date-order validation. |
| `src/test/java/com/custacm/platform/trainingdata/common/infra/oj/repo/account/JdbcOjHandleAccountRepositoryTest.java` | Verifies `oj_handle_account` insert, full-list reads from `handles_json` and `collection_states_json`, OJ-handle lookup, migration defaults, identity plus `need_collect` updates, and standalone collection-state updates. |
| `src/test/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjAcceptedSummaryRepositoryTest.java` | Verifies DWS JDBC query predicates, rating filtering, row mapping, and requested-OJ table selection. |
| `src/test/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjFirstAcceptedProblemRepositoryTest.java` | Verifies DWM JDBC handle/problem counts, paged queries, time filters, rating filters, row mapping, and requested-OJ table selection. |
| `src/test/java/com/custacm/platform/trainingdata/common/infra/oj/repo/query/JdbcOjSubmissionRepositoryTest.java` | Verifies DWD JDBC handle/problem counts, paged queries, time filters, rating filters, row mapping, and requested-OJ table selection. |
| `src/test/java/com/custacm/platform/trainingdata/common/infra/oj/repo/warehouse/JdbcOjWarehouseDataPurgeRepositoryTest.java` | Verifies transactional OJ-name and handle-based DWD/DWM/DWS deletion without touching other handles. |
| `src/test/java/com/custacm/platform/trainingdata/common/scheduler/OjCollectorSchedulingConfigTest.java` | Verifies enabled OJ collector schedule entries are registered from configuration with the configured `ojName`, scheduled batches trigger warehouse refresh, no-batch runs skip refresh, and disabled entries do not create tasks. |
| `src/test/java/com/custacm/platform/trainingdata/common/web/account/OjHandleAccountControllerTest.java` | Verifies OJ handle-account controller request normalization, full-list response mapping, invalid-request handling, response mapping, and business error-to-HTTP-status mapping. |
| `src/test/java/com/custacm/platform/trainingdata/common/web/collector/OjSubmissionCollectionControllerTest.java` | Verifies admin collection controller request delegation, optional OJ-name propagation, job start/list response mapping, empty body rejection, and invalid-request error body. |
| `src/test/java/com/custacm/platform/trainingdata/common/web/query/OjWarehouseQueryControllerTest.java` | Verifies public warehouse query controller OJ-name pass-through, parameter parsing, non-exposure of the automatic-summary list route, submission and first-accepted pagination defaults and validation, service delegation, response mapping, and invalid-request handling. |
| `src/test/java/com/custacm/platform/trainingdata/common/web/purge/OjStudentDataPurgeControllerTest.java` | Verifies student-data purge controller response mapping and purge error response mapping. |
