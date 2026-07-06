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
    training-data-codeforces/
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

- stores raw Codeforces submissions in `ods_codeforces__submission`;
- stores cleaned Codeforces submission details in `dwd_codeforces__submission`;
- stores Codeforces handle/problem first accepted intermediate facts in `dwm_codeforces__handle_problem_first_accepted`;
- stores Codeforces handle/date/rating accepted summaries in `dws_codeforces__handle_daily_rating_accepted_summary`;
- stores platform `studentIdentity` to Codeforces handle bindings and automatic-collection flags in `codeforces_handle_account`;
- keeps Codeforces HTTP ingress, ingest application service, recent-lookback submission collector, in-process collection job service, collect batch type, ODS record, parser, writer, handle-account mapping, fixture, DDL, SQL task resources, SQL task manifest, Spring config, and tests in an independent OJ module;
- parses Codeforces fixture data into OJ-specific ODS records for repeatable tests;
- writes ODS rows through `CodeforcesOdsSubmissionWriter` and its JDBC implementation;
- exposes OJ-specific ODS ingest through each OJ module under `training-data-web`;
- uses platform RSA JWT resource-server validation for protected `/admin/**` and `/player/**` URL tiers, matching the auth module's converter.
- exposes OJ-specific ODS ingest under `/api/training-data/admin/**`, restricted to the platform `admin` role.
- exposes Codeforces recent-lookback submission collection under `/api/training-data/admin/codeforces/submissions:collect`, and browser-resumable in-process collection jobs under `/api/training-data/admin/codeforces/submissions:collect-batch-jobs`, restricted to the platform `admin` role.
- exposes Codeforces handle-account creation and identity/automatic-collection-flag updates under `/api/training-data/admin/codeforces/**`, restricted to the platform `admin` role.
- exposes Codeforces warehouse refresh under `/api/training-data/admin/codeforces/warehouse:refresh`, restricted to the platform `admin` role.
- exposes Codeforces student-data purge under `/api/training-data/admin/codeforces/users/{studentIdentity}/data`, restricted to the platform `admin` role; this deletes the current handle binding plus ODS/DWD/DWM/DWS rows for that handle, but not the auth account.
- exposes Codeforces handle lookup by `studentIdentity` under `/api/training-data/codeforces/**` as a guest endpoint that does not parse JWTs.
- exposes Codeforces DWD/DWM/DWS read-side query endpoints under `/api/training-data/codeforces/**` as guest endpoints that do not parse JWTs; DWS includes a `need_collect=true` automatic-collection user summary list sorted by total AC count, and DWD submission detail queries are backend-paginated, newest-first, and return exact total/page metadata.
- applies ODS/DWD/DWM/DWS and Codeforces handle-account table migrations from OJ modules through Flyway at `training-data-web` startup.

Current training-data module shape:

```text
platform-training-data/
  training-data-codeforces/
    app/
      account/
      collector/
      ingest/
      query/
      warehouse/
    collector/config/
    config/
    domain/
      collector/
      criteria/
      model/
      parser/
      repo/
      value/
    infra/
      collector/
      parser/
      repo/
    scheduler/
    web/
      account/
      collector/
      ingest/
      query/
      warehouse/
    src/main/resources/db/migration/
    src/main/resources/fixtures/codeforces/
    src/main/resources/sql/ods/
    src/main/resources/sql/dwd/
    src/main/resources/sql/dwm/
    src/main/resources/sql/dws/
    src/main/resources/sql/tasks/
  training-data-web/
```

The OJ boundary is vertical. Codeforces owns its entrance and data organization end to end:

```text
external source or fixture
 -> OJ HTTP ingress or Codeforces source client
 -> OJ ingest app service
 -> OJ parser/writer
 -> OJ ODS table
 -> OJ SQL task manifest and common SQL task runner
 -> OJ DWD/DWM/DWS tables
```

Codeforces also owns its current handle-account mapping because the only implemented binding is Codeforces-specific:

```text
studentIdentity
 -> Codeforces handle-account app service
 -> codeforces_handle_account
```

Admin updates for this mapping may change `codeforces_handle_account.student_identity` and `need_collect`; they do not update auth accounts and do not change the stored Codeforces handle.

Codeforces DWD/DWM/DWS transforms are SQL task resources. The current Java execution path is synchronous admin refresh: each request computes the batch's UTC+8 refresh interval from ODS submission times, reads the manifest, rebuilds and validates the DAG, then runs SQL files as set-based database work rather than row-by-row Java transformation. It supports manual resume with `startFromTaskId`, which executes the requested node and its downstream nodes only.

Codeforces recent-lookback submission collection is an OJ-owned source-ingestion use case. The admin HTTP entry accepts `studentIdentity` plus a positive lookback duration, resolves the identity to its bound Codeforces handle, and computes the right boundary from the service's current execution instant. The internal handle path pages Codeforces `user.status`, applies bounded connect/read timeouts and retry attempts to each source page request, reports per-handle status, and writes successful matches into ODS. For browser-driven batch collection, `training-data-codeforces` also exposes an in-process collection job service: admins start a job for multiple `studentIdentity` values, then poll job list/detail endpoints for `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` per-user status. These job snapshots survive frontend refresh and page switches but are not persisted across backend restarts and are not a general pipeline run state. The disabled-by-default Spring scheduled trigger calls the same app service daily at 12:00 by default with a rolling 120-hour lookback and collects handles whose `codeforces_handle_account.need_collect` flag is true, de-duplicated by handle. The public automatic-collection DWS summary query reads the same flag, reuses the personal DWS summary logic for each matching binding, and sorts by total accepted problem count. It is not the future persistent pipeline scheduler; after ODS write, the code currently leaves a TODO for the future scheduler/orchestrator handoff.

There is currently no persistent pipeline run state or ADS physical table. OJ-specific DWD/DWM/DWS tables should stay independent until a concrete cross-OJ product query needs a unified view or ADS table.

Current physical data layer:

```text
ODS: ods_codeforces__submission
DWD: dwd_codeforces__submission
DWM: dwm_codeforces__handle_problem_first_accepted
DWS: dws_codeforces__handle_daily_rating_accepted_summary
Codeforces handle account: codeforces_handle_account
ADS: not implemented yet
```

`training-data-web` owns the runtime datasource, MySQL JDBC driver, and Flyway auto-migration. OJ modules own the actual migration scripts under their own `src/main/resources/db/migration/` directories.

`training-data-web` uses the same file logging contract as other runnable Spring Boot services: `LOG_DIR/combined.log` and `LOG_DIR/error.log`.

### frontend

`frontend` is the first runnable frontend slice. It is a React/Vite/TypeScript single-page app for the training-team management workbench.

Current implementation:

- defaults to a focused training query workspace with single-user detail query and automatic-collection multi-user summary pages. The single-user page filters by `studentIdentity`, date range, and rating range, then shows newest-first Codeforces AC/submission activity, first-accepted, and rating-distribution results. The multi-user page reuses the same date/rating range filters, reads all handle bindings marked for automatic collection, and displays their DWS AC summaries as a total-accepted descending table. The left sidebar exposes icon-assisted function module entries, separated into available and unavailable groups, with only training-data enabled and blog/editor shown as unsupported. Query tabs are URL-addressable as `/query/multiple` and `/query/single`;
- separates admin-only operations into an admin workspace with left-sidebar pages for user information management, training-data collection, data maintenance, and operation records; the user information page shows all auth users sorted by the numeric `studentIdentity` prefix descending and expands existing-user edits inside that list, including Codeforces handle automatic-collection eligibility, the collection page lists only eligible handle accounts, can start one backend collection job for all listed students with a shared lookback window, can start collection per selected student row, polls a task list with expandable per-user details, and treats a blank lookback field as an unlimited collection window; the maintenance page owns ODS upload, warehouse refresh, and the high-risk user deletion action that clears Codeforces training data before deleting the auth account. Admin pages are URL-addressable as `/admin/users`, `/admin/collection`, `/admin/maintenance`, and `/admin/records`;
- logs in through `POST /api/auth/login`, stores the returned access token in browser localStorage, and uses it for admin auth/training-data calls;
- reads public `GET /api/auth/users`, `POST /api/auth/admin/users:batch-create`, `PATCH /api/auth/admin/users/{studentIdentity}`, `DELETE /api/auth/admin/users/{studentIdentity}`, `GET /api/auth/player/me`, Codeforces public single-handle/query endpoints including the automatic-collection accepted-summary list, and admin Codeforces handle/ODS/collector-job/warehouse/purge endpoints through frontend-local API clients;
- uses Vite dev proxy and the production Nginx frontend container to keep browser requests same-origin;
- uses the Compose `frontend-build` one-shot service to generate `frontend/dist`, while `custacm-frontend` runs a fixed Nginx image with bind-mounted static assets and proxy config;
- includes `scripts/seed-local-codeforces-data.sh` to prepare local fixture-backed data through real HTTP APIs;
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
POST /api/auth/admin/users
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
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
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
fixture-backed local workbench, run:

```bash
./scripts/seed-local-codeforces-data.sh
```
