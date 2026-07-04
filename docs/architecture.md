# custacm-platform Architecture

## Phase

The current phase creates a small, evolvable backend skeleton. It should not lock in the final product model yet.

The first runnable slice is Keycloak-backed platform auth. The second runnable slice is the training-data ODS warehouse model. Other product areas are represented by directories only and should be expanded later, one module at a time.

## Module Map

```text
custacm-platform/
  platform-common/
    common-core/
    common-web/

  platform-auth/
    auth-core/
    auth-interface/
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

It currently contains only general shared library modules.

Current and expected split:

- `common-core`: base types, base exceptions, error codes, general utilities.
- `common-web`: HTTP response helpers, exception handling, request context helpers.

Do not put business concepts such as `User`, `Article`, `TrainingDataset`, or editor documents in common modules.

### platform-auth

`platform-auth` is the first runnable module and owns the platform auth boundary.

Current implementation:

- keeps Keycloak JWT parsing and current-user extraction in `auth-core`;
- validates Keycloak-issued JWTs;
- extracts immutable student identity and one platform role from JWT claims;
- exposes the current-user endpoint.

Keycloak is the source of truth for login, registration, password reset, sessions, and token issuance. Do not add local password login, demo tokens, or self-issued JWTs unless the identity decision changes explicitly.

The only platform roles are:

```text
admin
student
```

Business APIs expose a single `role` string. If Keycloak ever puts both platform roles in a token, `admin` wins.

The platform student identity is one immutable string:

```text
student_identity = fixed-length student number + real name
example: 112487张三
```

This value is stored as a Keycloak user attribute named `student_identity` and emitted into JWTs as the `student_identity` claim.

Do not split this identity into separate `student_no` and `real_name` fields in the platform model unless the product decision changes explicitly.

`studentIdentity` is the only user ID in platform business code. Other modules should store and reference this value directly when they need to associate data with a student.

Current auth module shape:

```text
platform-auth/
  auth-core/
  auth-interface/
  auth-web/
```

Add `auth-domain`, `auth-app`, or `auth-infra` only when the platform needs business-owned auth or identity data beyond the immutable `student_identity` claim.

### platform-training-data

`platform-training-data` owns the first training-data warehouse slice.

Current implementation:

- stores raw Codeforces submissions in `ods_codeforces__submission`;
- stores cleaned Codeforces submission details in `dwd_codeforces__submission`;
- stores Codeforces handle/problem first accepted intermediate facts in `dwm_codeforces__handle_problem_first_accepted`;
- stores Codeforces handle/date daily accepted summaries with fixed rating count columns in `dws_codeforces__handle_daily_rating_accepted_summary`;
- keeps Codeforces HTTP ingress, ingest application service, collect batch type, ODS record, parser, writer, DWD/DWM/DWS query repositories/services, fixture, DDL, SQL task resources, Spring config, and tests in an independent OJ module;
- parses Codeforces fixture data into OJ-specific ODS records for repeatable tests;
- writes ODS rows through `CodeforcesOdsSubmissionWriter` and its JDBC implementation;
- reads Codeforces DWD submission rows, DWM first-accepted problem rows, and DWS daily rating accepted summary rows through OJ-owned repositories and JDBC implementations;
- uses UTC+8 (`Asia/Shanghai`) as the canonical warehouse `datetime` / date-grain time zone for Codeforces derived tables and read-side query boundaries;
- exposes OJ-specific ODS ingest through each OJ module under `training-data-web`;
- uses Keycloak JWT resource-server validation for `/api/**`, matching the auth module's converter.
- restricts OJ-specific ODS ingest to the platform `admin` role.
- applies ODS/DWD/DWM/DWS table migrations from OJ modules through Flyway at `training-data-web` startup.

Current training-data module shape:

```text
platform-training-data/
  training-data-codeforces/
    src/main/java/com/custacm/platform/trainingdata/codeforces/
      app/
        result/
        service/
      config/
      domain/
        criteria/
        model/
        parser/
        repo/
        value/
      infra/
        parser/
        repo/
      web/
        controller/
        response/
    src/main/resources/db/migration/
    src/main/resources/fixtures/codeforces/
    src/main/resources/sql/dwd/
    src/main/resources/sql/dwm/
    src/main/resources/sql/dws/
    src/main/resources/sql/ods/
  training-data-web/
```

`app` owns use-case orchestration and result types. `domain` owns business data models, value objects, repository contracts, and validated query criteria. `infra` owns source parsing and JDBC implementations. `web` owns HTTP controllers and HTTP response DTOs. `config` wires the OJ module into Spring.

The OJ boundary is vertical. Codeforces owns its entrance and data organization end to end:

```text
external source or fixture
 -> OJ HTTP ingress
 -> OJ ingest app service
 -> OJ parser/writer
 -> OJ ODS table
 -> OJ SQL task resources
 -> OJ DWD/DWM/DWS tables
```

Codeforces DWD/DWM/DWS transforms are SQL task resources. Java scheduling/execution is not implemented yet; later Java code should trigger these SQL files rather than performing row-by-row transformation.

Codeforces read-side query code may read existing warehouse tables directly through OJ-owned repositories. Current DWD/DWM/DWS query support is internal Java capability only; it is not yet exposed as an HTTP API.

Training-data warehouse `datetime` and date-grain fields use UTC+8 (`Asia/Shanghai`) as the canonical local time zone. Codeforces epoch seconds are shifted to UTC+8 when building DWD `submitted_at_utc_plus8` / `submitted_date_utc_plus8`; DWM and DWS inherit the same day boundary. The default `training-data-web` datasource URL sets `serverTimezone=Asia/Shanghai`.

There is currently no shared DAG/task orchestration layer. Do not reintroduce pipeline run state, scheduler, or generic task executors until the data model has a real downstream workflow. OJ-specific DWD/DWM/DWS tables should stay independent until a concrete cross-OJ product query needs a unified view or ADS table.

Current physical data layer:

```text
ODS: ods_codeforces__submission
DWD: dwd_codeforces__submission
DWM: dwm_codeforces__handle_problem_first_accepted
DWS: dws_codeforces__handle_daily_rating_accepted_summary
ADS: not implemented yet
```

`training-data-web` owns the runtime datasource, MySQL JDBC driver, and Flyway auto-migration. OJ modules own the actual migration scripts under their own `src/main/resources/db/migration/` directories.

`training-data-web` uses the same file logging contract as other runnable Spring Boot services: `LOG_DIR/combined.log` and `LOG_DIR/error.log`.

### Placeholder Modules

These directories exist to preserve product boundaries:

- `platform-blog`: future blog/content module.
- `platform-editor`: future external editor integration.
- `platform-article-storage`: future article storage module.
- `frontend`: future frontend implementation.
- `deploy`: future Docker/K8s deployment configuration.

Do not add them to the Maven reactor until their first runnable slice is being implemented.

## Dependency Direction

Within a business module, prefer this shape:

```text
web -> app -> domain
web -> infra -> domain
app -> interface
web -> interface
```

Rules:

- `domain` must not depend on `app`, `infra`, or `web`.
- `interface` must not depend on `app`, `infra`, or `web`.
- `app` orchestrates use cases and should avoid direct infrastructure details.
- `infra` implements repositories and remote clients.
- `web` owns Spring Boot startup and HTTP controllers.

`platform-auth` is currently a Keycloak adapter module and therefore intentionally has only `auth-core`, `auth-interface`, and `auth-web`. `platform-training-data` uses vertical OJ modules because OJ data warehouses must own their own ingress and data organization.

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
GET  /api/auth/me
```

`GET /api/auth/me` requires a Keycloak bearer token.

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
POST /api/training-data/ods/codeforces/submissions:batch-upsert
```

Training-data `/api/**` endpoints require a Keycloak bearer token. ODS batch-upsert endpoints require the platform `admin` role.

Current response shape:

```json
{
  "studentIdentity": "112487张三",
  "role": "student"
}
```

For local deployment, use:

```bash
cp deploy/.env.example deploy/.env
./scripts/deploy.sh
```
