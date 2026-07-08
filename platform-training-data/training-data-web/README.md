# training-data-web

`training-data-web` is the runnable Spring Boot service for the current training-data slice.

It owns the HTTP runtime, datasource, Flyway migration execution, module health/info endpoints, logging runtime configuration, and platform URL authorization for training-data APIs.

## Directory Layout

```text
training-data-web/
  src/main/java/com/custacm/platform/trainingdata/
  src/main/java/com/custacm/platform/trainingdata/web/
  src/main/resources/
  src/test/java/com/custacm/platform/trainingdata/web/
```

Common handle-account endpoints, admin submission collection endpoints, admin collection-job status endpoints, admin student-data purge endpoints, public warehouse query endpoints, and OJ-generic Spring wiring live in `training-data-common`; OJ-specific source/ODS/warehouse adapters and Spring wiring live in vertical OJ modules such as `training-data-codeforces` and `training-data-atcoder`. This web module runs and secures them through Spring component scanning.

## Dependency And Layer Rules

- May depend on `auth-core` for JWT decoding and URL authorization helpers.
- May depend on OJ modules such as `training-data-codeforces` and `training-data-atcoder`.
- Must not move OJ-specific parsing, records, writers, or SQL resources into this runtime module.
- Admin APIs must stay under `/api/training-data/admin/**`; guest endpoints must not parse JWTs.
- The public full OJ handle account map endpoint, including per-OJ collection states, and DWD/DWM/DWS warehouse queries are guest endpoints; the query routes keep the `/api/training-data/codeforces/**` path and accept `ojName` as the OJ pass-through parameter. OJ handle creation, identity migration, submission collection, collection-job start/list, and student-data purge are admin endpoints.

## File Responsibilities

- `TrainingDataWebApplication.java` - Spring Boot entrypoint.
- `TrainingDataModuleController.java` - `/health` and `/module-info`, including Codeforces collector/warehouse feature flags plus AtCoder ODS, submission collector, problem-list/problem-model collector, warehouse-table, and warehouse-refresh feature flags.
- `TrainingDataSecurityConfig.java` - builds the admin protected chain and guest public chain using `PlatformSecurityConfig`.
- `TrainingDataJwtProperties.java` - typed RSA public-key settings for validating platform JWTs.
- `application.yml` - service port, datasource, Flyway, logging, auth public-key defaults, common collection-job per-identity interval, enabled-by-default Codeforces/AtCoder OJ collector schedule entries that refresh the selected OJ warehouse after a batch, Codeforces source timeout/retry/interval defaults, and AtCoder Kenkoooo source/problem metadata startup bootstrap and every-three-days scheduler defaults.
- `logback-spring.xml` - file logging configuration following the project logging contract.
- `src/test/java/com/custacm/platform/trainingdata/web/OjHandleAccountHttpIntegrationTest.java` - verifies Flyway, HTTP routing, admin writes, public full-map list with collection states, identity migration, and admin student-data purge by resolved OJ handle while keeping OJ handle accounts.
- `src/test/java/com/custacm/platform/trainingdata/web/CodeforcesSubmissionCollectionHttpIntegrationTest.java` - verifies admin recent-lookback submission collection by `studentIdentity`, identity-to-handle resolution, mocked Codeforces source access, ODS writes, and Codeforces collection-state persistence.
- `src/test/java/com/custacm/platform/trainingdata/web/AtcoderCollectionHttpIntegrationTest.java` - verifies admin AtCoder recent-lookback submission collection through the common route with `ojName=ATCODER`, mocked Kenkoooo source access, AtCoder collection-state persistence, and collection-job-triggered AtCoder warehouse refresh.
- `src/test/java/com/custacm/platform/trainingdata/web/TrainingDataSecurityConfigTest.java` - verifies training-data admin endpoints, including Codeforces collection-job start/list paths, require admin JWTs while guest endpoints, including public warehouse queries, ignore bearer tokens.
