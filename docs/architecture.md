# custacm-platform Architecture

## Phase

The current phase creates a small, evolvable backend skeleton plus the first runnable frontend workbench slice. It should not lock in the final product model yet.

The first runnable backend slice is platform-owned auth. The second runnable backend slice is the training-data ODS warehouse model. The first runnable frontend slice is a React/Vite training-team admin dashboard that logs in through `auth-web` and reads real `auth-web` / `training-data-web` HTTP APIs. Other product areas are represented by directories only and should be expanded later, one module at a time.

## Module Map

```text
custacm-platform/
  platform-common/
    common-core/
    common-web/

  platform-auth/
    auth-domain/
    auth-app/
    auth-core/
    auth-infra/
    auth-web/

  platform-training-data/
    training-data-common/
    training-data-codeforces/
    training-data-atcoder/
    training-data-web/

  platform-blog/
  platform-editor/
  platform-article-storage/

  frontend/
  deploy/
```

For agent navigation, keep the directory-level map in `docs/agent/context-map.md` synchronized with this architecture document.

## Current Module Responsibilities

### platform-common

`platform-common` is a shared library area, not a service and not a container.

It currently contains general shared library modules and the reusable SQL task execution core.

Current and expected split:

- `common-core`: reusable backend primitives, including the SQL task DAG runner that reads a YAML manifest on every run, rebuilds an adjacency-list graph, validates DAG shape, and executes SQL nodes with one transaction per node.
- `common-web`: HTTP response helpers, exception handling, request context helpers.

Do not put business concepts such as `User`, `Article`, `TrainingDataset`, or editor documents in common modules.

### platform-auth

`platform-auth` is the first runnable module and owns the platform auth boundary.

Current implementation:

- stores local accounts in MySQL through `auth-infra`;
- hashes passwords with BCrypt;
- signs access tokens with an RSA private key in `auth-web`;
- issues ordinary login access tokens with a default 2-hour lifetime, and `rememberMe` login access tokens with a default 30-day lifetime;
- validates platform JWTs with the matching RSA public key;
- keeps platform JWT parsing, role-to-authority conversion, current-user extraction, and shared URL security setup in `auth-core`;
- exposes login, current-user, own-password-change, and admin user-management endpoints.

There is no public registration flow. Admins create users directly or through batch creation. The first admin may be bootstrapped from environment variables at startup.

The platform roles are:

```text
admin
player
disable
```

Business APIs expose a single `role` string when a user is authenticated. Stored login accounts can be `admin`, `player`, or `disable`; `disable` accounts cannot authenticate. Unauthenticated visitors have no `role` value and are handled only through public endpoints. JWTs only emit authenticatable roles: `admin` or `player`.

HTTP authorization follows [authorization.md](authorization.md):

```text
/admin/**   -> admin-only
/player/**  -> player or admin
other paths  -> guest/public and do not parse JWTs
```

`admin` includes `player` capability in Spring Security authorities, so admin JWTs can call `/player/**` endpoints.

The platform student identity is one immutable string:

```text
studentIdentity = fixed-length student number + real name
example: 230511213黄炳睿
```

Do not split this identity into separate `student_no` and `real_name` fields in the platform model unless the product decision changes explicitly.

`studentIdentity` is the only user ID in platform business code. Other modules should store and reference this value directly when they need to associate data with a user. JWTs carry this value in the standard `sub` claim and carry the single role in `role`.

Current auth module shape:

```text
platform-auth/
  auth-domain/
  auth-app/
  auth-core/
  auth-infra/
  auth-web/
```

`auth-domain` owns account entities, account roles, and repository contracts. `auth-app` owns login, failed-login retry cooldown, admin user-management use cases, generated-password handling, and app-layer result models. `auth-infra` owns JDBC persistence, BCrypt, RSA JWT issuing, and Flyway migrations. `auth-web` owns HTTP controllers and HTTP-local request/response DTOs, including admin operation responses that can return a one-time plaintext password for newly created or reset accounts. `auth-core` remains the shared platform JWT parsing, authority conversion, public-key decoder, and URL authorization library for runnable services. Other runnable services should use `auth-core` to validate JWTs and extract `studentIdentity` plus `role`; they do not depend on auth HTTP DTOs.

### platform-training-data

`platform-training-data` owns the first training-data warehouse slice.

Current implementation:

- stores raw Codeforces submissions in `ods_codeforces__submission`, attributed to the collected `user.status` handle for team submissions;
- stores raw AtCoder submissions from Kenkoooo in `ods_atcoder__submission`;
- stores raw AtCoder problem-list items from Kenkoooo `resources/problems.json` in `ods_atcoder__problem`;
- stores raw AtCoder problem-model items from Kenkoooo `resources/problem-models.json` in `ods_atcoder__problem_model`;
- stores cleaned Codeforces submission details in `dwd_codeforces__submission` at `submission + handle` grain;
- stores Codeforces handle/problem first accepted intermediate facts in `dwm_codeforces__handle_problem_first_accepted`;
- stores Codeforces handle/date/rating accepted summaries in `dws_codeforces__handle_daily_rating_accepted_summary`;
- stores cleaned AtCoder submission details in `dwd_atcoder__submission`;
- stores AtCoder handle/problem first accepted intermediate facts in `dwm_atcoder__handle_problem_first_accepted`;
- stores AtCoder handle/date/difficulty accepted summaries in `dws_atcoder__handle_daily_rating_accepted_summary`;
- stores platform `studentIdentity` to OJ handle bindings in `oj_handle_account`, where `handles_json` is an uppercase OJ-name to handle map, `need_collect` is the current automatic-collection eligibility flag, and `collection_states_json` is an uppercase OJ-name to `{historyStartReached,lastCollectedAt}` map;
- keeps reusable OJ app/domain/infra code in `training-data-common`: handle-account services/repositories, per-OJ difficulty bucket policies, DWD/DWM/DWS query services/repositories, common same-layer DWD/DWM/DWS table DDL migrations, student-data purge orchestration, submission-collection orchestration, OJ collector dispatch, generic warehouse refresh interval/service/handler contracts, OJ warehouse refresh dispatch, in-process job service, scheduling helpers, lookback window computation, handle resolver/adapter contracts, in-JVM overlap skipping, source request retry/rate limiting, sorted-page window filtering, aggregate result models, scheduled-collection properties/config, required-text argument validation, and tests;
- keeps Codeforces ingest application service, Codeforces collection adapter/facade, collect batch type, ODS record, parser, writer, Codeforces ODS purge adapter, refresh interval JDBC adapter, fixture, historical DDL/Flyway migrations, SQL task resources, SQL task manifest, Spring config, and tests in an independent OJ module;
- keeps AtCoder Kenkoooo source client, ODS ingest application service, recent-window submission collection adapter/facade, startup/low-frequency problem metadata collection service, collect batch type, submission/problem/problem-model ODS records, parser, writers, AtCoder ODS purge adapter, AtCoder refresh interval JDBC adapter, ODS landing table migrations, DWD/DWM/DWS SQL task resources, Spring config, and tests in an independent OJ module;
- parses Codeforces fixture data into OJ-specific ODS records for repeatable tests;
- writes Codeforces ODS rows through `CodeforcesOdsSubmissionWriter` and AtCoder ODS rows through `AtcoderOdsSubmissionWriter` / `AtcoderOdsProblemWriter` / `AtcoderOdsProblemModelWriter` JDBC implementations;
- uses platform RSA JWT resource-server validation for protected `/admin/**` and `/player/**` URL tiers, matching the auth module's converter.
- exposes Codeforces-compatible recent-lookback submission collection under `/api/training-data/admin/codeforces/submissions:collect`, and browser-resumable in-process collection jobs under `/api/training-data/admin/codeforces/submissions:collect-batch-jobs`, restricted to the platform `admin` role; both accept optional `ojName`, where omitted uses the dispatcher default `CODEFORCES` and explicit `ATCODER` selects the Kenkoooo AtCoder collector.
- exposes OJ handle-account creation and identity/automatic-collection-flag updates under `/api/training-data/admin/oj-handles/**`, restricted to the platform `admin` role.
- exposes student-data purge under `/api/training-data/admin/students/{studentIdentity}/oj-data`, restricted to the platform `admin` role; `ojName` is a required query parameter and selects one bound OJ. The use case runs in one transaction, deletes DWD/DWM/DWS through the common OJ warehouse purge contract, deletes ODS through the selected OJ-specific purge adapter, and keeps handle-account and auth accounts.
- exposes the full OJ handle account map, including per-OJ collection states, under `/api/training-data/oj-handles` as a guest endpoint that does not parse JWTs.
- exposes public DWD/DWM/DWS read-side query endpoints under `/api/training-data/codeforces/**` as guest endpoints that do not parse JWTs; single-user/problem queries accept `ojName` as the OJ pass-through parameter, app services resolve the requested OJ handle from `handles_json`, JDBC selects the same-layer table by the normalized OJ name, and DWD submission detail plus DWM first-accepted detail queries are backend-paginated, newest-first, and return exact total/page metadata.
- applies ODS/DWD/DWM/DWS and OJ handle-account table migrations from OJ modules and the common module through Flyway at `training-data-web` startup.

Current training-data module shape:

```text
platform-training-data/
  training-data-common/
    app/
      account/
      purge/
      query/
      warehouse/
    collector/
      config/
      dispatch/
      job/
      result/
    domain/
      oj/
    infra/
      oj/
    scheduler/
    support/
    web/
      account/
      collector/
      purge/
      query/
    src/main/resources/db/migration/
  training-data-codeforces/
    app/
    config/
    domain/
    infra/
    src/main/resources/db/migration/
    src/main/resources/fixtures/codeforces/
    src/main/resources/sql/ods/
    src/main/resources/sql/dwd/
    src/main/resources/sql/dwm/
    src/main/resources/sql/dws/
    src/main/resources/sql/tasks/
  training-data-atcoder/
    app/
    config/
    domain/
    infra/
    web/
    src/main/resources/db/migration/
    src/main/resources/sql/ods/
    src/main/resources/sql/dwd/
    src/main/resources/sql/dwm/
    src/main/resources/sql/dws/
    src/main/resources/sql/tasks/
  training-data-web/
```

The OJ boundary is vertical. Codeforces and AtCoder own their OJ-specific ODS contracts, source clients, parsers/writers, source paging rules, refresh interval SQL, and warehouse refresh resources/manifests. Shared OJ app/domain/infra/web contracts, Spring bean wiring, collector dispatch, generic warehouse refresh interval/service/handler contracts, warehouse refresh dispatch, shared SQL task runner bean, and common same-layer DWD/DWM/DWS table DDL come from `training-data-common`:

```text
external source or fixture
 -> OJ source client or local fixture path
 -> OJ ingest app service
 -> OJ parser/writer
 -> OJ ODS table
 -> OJ SQL task manifest and common SQL task runner
 -> OJ DWD/DWM/DWS tables
```

`training-data-common` hosts the OJ handle-account app/domain/infra/web implementation; Codeforces supplies the historical Flyway migration that creates the base physical table, and common supplies later generic handle-account extensions such as collection states:

```text
studentIdentity
 -> OJ handle-account app service
 -> oj_handle_account.handles_json
 -> CODEFORCES / ATCODER handle values
```

Admin updates for this mapping may change `oj_handle_account.student_identity`, `need_collect`, and the stored handle map through merge semantics; they do not update auth accounts. Existing per-OJ collection state is kept when that OJ's handle value is unchanged and reset when the handle value is replaced.

Codeforces and AtCoder DWD/DWM/DWS transforms are SQL task resources. The current Java execution path is internal to collection jobs and enabled scheduled collection: common refresh code asks the OJ-provided interval repository for the batch's effective UTC+8 refresh interval, reads the OJ manifest, rebuilds and validates the DAG, then runs SQL files as set-based database work rather than row-by-row Java transformation. Codeforces derives the interval from ODS submission creation times; AtCoder also folds in existing DWM first-accepted dates touched by accepted submissions in the batch.

Recent-lookback submission collection is an OJ-owned source-ingestion use case behind common HTTP routes and a common dispatcher. The admin HTTP entry accepts `studentIdentity` plus a positive lookback duration, resolves the identity to its bound OJ handle from `handles_json`, and computes the right boundary from the service's current execution instant. The Codeforces internal handle path pages `user.status`; the AtCoder path pages Kenkoooo `user/submissions` with `from_second`. Both apply bounded connect/read timeouts and retry attempts to each source page request, report per-handle status, write successful matches into ODS, and let common code persist successful per-OJ handle collection state. `lastCollectedAt` is the collector execution instant rather than the latest submission time. `historyStartReached` is set only when the OJ adapter can prove the historical left edge was reached: Codeforces reaches the source's final page, while AtCoder starts from `from_second=0`. For browser-driven batch collection, `training-data-common` owns the HTTP controller, in-process collection job service, shared job executor, generic SQL-task warehouse refresh handler, and warehouse-refresh dispatcher bean, while OJ modules supply collection services, interval repositories, SQL resources, and manifests. Codeforces and AtCoder both wire the common refresh handler with their OJ name and manifest. Admins start a job for multiple `studentIdentity` values, then poll the job list endpoint for `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` per-user status; the per-identity wait is configured by `platform.training-data.collector.job-item-interval`. These job snapshots survive frontend refresh and page switches but are not persisted across backend restarts and are not a general pipeline run state. The enabled-by-default Spring scheduled trigger is wired by `training-data-common` and driven by `platform.training-data.collector.schedules`; each entry carries an `ojName`, calls the same app service at its cron, collects handles whose `oj_handle_account.need_collect` flag is true and whose handle map contains that OJ name, de-duplicated by handle, and calls the same warehouse refresh dispatcher bean when the collection result has a `batchId`. The default config includes enabled Codeforces and AtCoder recent-submission schedules. AtCoder problem metadata refresh is separate from submission polling: startup bootstrap pulls `problems.json` and `problem-models.json` when either metadata ODS table is empty, and the every-three-days scheduler stays enabled by default under `platform.training-data.atcoder.problem-list-collector`. Automatic collection eligibility is used by scheduled collection only; there is no public automatic-summary query endpoint.

There is currently no persistent pipeline run state or ADS physical table. OJ-specific DWD/DWM/DWS tables stay as same-layer independent tables with a shared query contract until a concrete cross-OJ product query needs a unified view or ADS table.

Current physical data layer:

```text
ODS: ods_codeforces__submission
ODS: ods_atcoder__submission
ODS: ods_atcoder__problem
ODS: ods_atcoder__problem_model
DWD: dwd_codeforces__submission
DWM: dwm_codeforces__handle_problem_first_accepted
DWS: dws_codeforces__handle_daily_rating_accepted_summary
DWD: dwd_atcoder__submission
DWM: dwm_atcoder__handle_problem_first_accepted
DWS: dws_atcoder__handle_daily_rating_accepted_summary
OJ handle account: oj_handle_account
ADS: not implemented yet
```

`training-data-web` owns the runtime datasource, MySQL JDBC driver, and Flyway auto-migration. OJ modules own OJ-specific ODS and historical OJ migration scripts under their own `src/main/resources/db/migration/` directories; `training-data-common` owns common same-layer DWD/DWM/DWS table DDL migrations under its own migration directory.

`training-data-web` uses the same file logging contract as other runnable Spring Boot services: `LOG_DIR/combined.log` and `LOG_DIR/error.log`.

### frontend

`frontend` is the first runnable frontend slice. It is a React/Vite/TypeScript single-page app for the training-team management workbench.

Current implementation:

- defaults to a focused training query workspace with multi-user, single-user detail, and problem-level query pages. Query controls default to a start date seven days before the current day, apply automatically when OJ, student, problem key, date range, or rating range changes, and refreshing the page loads the default multi-user summary. The single-user page filters by `studentIdentity`, OJ name, date range, and rating range, then shows newest-first Codeforces/AtCoder AC/submission activity, backend-paginated first-accepted results, and rating-distribution results. The problem page filters by problem key, OJ name, and range, then shows backend-paginated submissions and first-accepted handles. The multi-user page does not call a backend automatic-summary endpoint; it queries the existing public per-student accepted-summary endpoint for users with automatic collection enabled and a current-OJ handle, then shows the results by accepted problem count. The left sidebar exposes icon-assisted function module entries, separated into available and unavailable groups, with only training-data enabled and blog/editor shown as unsupported. Query tabs are URL-addressable as `/query/multiple`, `/query/single`, and `/query/problem`;
- separates admin-only operations into an admin workspace with left-sidebar pages for user creation, user modification, training-data collection, and operation records; the user creation page owns text import, editable create rows, auth batch creation, and optional Codeforces/AtCoder OJ handle binding. The user modification page shows all auth users sorted by the numeric `studentIdentity` prefix descending and expands existing-user edits inside that list, including Codeforces/AtCoder OJ handle binding, automatic-collection eligibility, per-OJ history-start coverage status, last collected time from `collectionStates`, and confirmed full user deletion. The collection page lists eligible handle accounts by selected OJ, can start one backend collection job for all listed students with a shared lookback window, can start collection per selected student row, polls a task list with expandable per-user details, and treats a blank lookback field as an unlimited collection window. Admin pages are URL-addressable as `/admin/user-create`, `/admin/user-edit`, `/admin/collection`, and `/admin/records`;
- logs in through `POST /api/auth/login`, stores the returned access token in browser localStorage, and uses it for admin auth/training-data calls;
- reads public `GET /api/auth/users`, `GET /api/auth/player/me`, `PATCH /api/auth/player/me/password`, `POST /api/auth/admin/users:batch-create`, `PATCH /api/auth/admin/users/{studentIdentity}`, `DELETE /api/auth/admin/users/{studentIdentity}`, module-info endpoints, public full OJ handle account map endpoint including collection states, public warehouse query endpoints that pass `ojName`, and admin OJ handle create/update/identity-migration, collector-job and purge endpoints through frontend-local API clients;
- uses Vite dev proxy and the production Nginx frontend container to keep browser requests same-origin;
- uses the Compose `frontend-build` one-shot service to generate `frontend/dist`, while `custacm-frontend` runs a fixed Nginx image with bind-mounted static assets and proxy config;
- includes `scripts/seed-local-codeforces-data.sh` to create sample users, bind OJ handles, and start Codeforces collection through real HTTP APIs;
- keeps `studentIdentity` as one immutable string in UI data and filtering;
- provides local frontend verification scripts for lint, unit tests, typecheck, and production build.

It should not move auth ownership, password handling, token issuance, or training-data domain rules into the frontend.

### Placeholder Modules

These directories exist to preserve product boundaries:

- `platform-blog`: future blog/content module.
- `platform-editor`: future external editor integration.
- `platform-article-storage`: future article storage module.

Do not add them to the Maven reactor until their first runnable slice is being implemented.

### deploy

`deploy` is the current Docker Compose deployment entry. It starts auth MySQL, `auth-web`, training-data MySQL, `training-data-web`, and the frontend Nginx static/proxy container for the local/single-server phase. Frontend static assets are generated by the one-shot `frontend-build` service and served from the `frontend/dist` bind mount, so frontend-only updates do not require rebuilding a frontend image.

## Dependency Direction

Within a business module, prefer this shape:

```text
web -> app -> domain
web -> infra -> domain
```

Rules:

- `domain` must not depend on `app`, `infra`, or `web`.
- `app` orchestrates use cases and should avoid direct infrastructure details.
- `infra` implements repositories and remote clients.
- `web` owns Spring Boot startup and HTTP controllers.

`platform-auth` now follows the domain/app/infra/web split because it owns account and credential data. `platform-training-data` uses vertical OJ modules because OJ data warehouses must own their own ingress and data organization.

## Cross-Module Calls

Each module exposes capabilities through its own `*-web` HTTP API.

If one module needs another module, the caller should define and use a local client/adapter. The target base URL must come from configuration, not hard-coded code.

Example future shape:

```text
content-app
  -> AuthClient
  -> content-infra HTTP adapter
  -> auth-web HTTP API
```

A gateway may be added later as a frontend-facing entrypoint. Internal service calls do not need to go through the gateway by default.

## Spring Boot Startup

Each runnable web service needs its own Spring Boot application class.

For package scanning, place the application class at the module package root. Example:

```text
com.custacm.platform.auth.AuthWebApplication
```

This allows Spring to scan:

```text
com.custacm.platform.auth
com.custacm.platform.auth.web
```

## Verification

Current verification commands:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
mvn clean verify
./scripts/check-test-policy.sh
```

`check-doc-sync.sh` verifies that code/config changes include the matching documentation updates. `mvn clean verify` runs unit tests and JaCoCo coverage checks. `check-test-policy.sh` verifies that Java modules with executable source have tests and generated test/coverage reports unless explicitly allowlisted. Code-bearing modules should keep line coverage at or above `70%`; placeholder-only modules do not need tests until they contain executable code.

Run the packaging check when build artifacts or Docker image behavior changes:

```bash
mvn clean package -DskipTests
```

Run `auth-web` locally:

```bash
java -jar platform-auth/auth-web/target/auth-web-0.1.0-SNAPSHOT.jar
```

Default port:

```text
8081
```

Basic endpoints:

```text
GET  /health
GET  /module-info
POST /api/auth/login
GET  /api/auth/player/me
PATCH /api/auth/player/me/password
POST /api/auth/admin/users:batch-create
GET  /api/auth/users
PATCH /api/auth/admin/users/{studentIdentity}
```

`/api/auth/player/**` and `/api/auth/admin/**` require a platform bearer token issued by `auth-web`. Other auth endpoints are guest endpoints unless documented otherwise.

Run `training-data-web` locally:

```bash
java -jar platform-training-data/training-data-web/target/training-data-web-0.1.0-SNAPSHOT.jar
```

Default port:

```text
8082
```

Basic endpoints:

```text
GET  /health
GET  /module-info
POST /api/training-data/admin/codeforces/submissions:collect
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
GET  /api/training-data/admin/codeforces/submissions/collect-batch-jobs
DELETE /api/training-data/admin/students/{studentIdentity}/oj-data
```

Training-data `/admin/**` endpoints require a platform bearer token with the platform `admin` role. Guest endpoints do not parse JWTs.

Current response shape:

```json
{
  "studentIdentity": "230511213黄炳睿",
  "role": "player"
}
```

For local deployment, use:

```bash
cp deploy/.env.example deploy/.env
./scripts/deploy.sh
```

The Compose stack exposes the frontend at `http://localhost:3000/`, auth at
`http://localhost:8081/`, and training data at `http://localhost:8082/`. For a
sample local workbench, run:

```bash
./scripts/seed-local-codeforces-data.sh
```
