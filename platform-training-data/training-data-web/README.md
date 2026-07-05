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

OJ-specific controllers, handle-account endpoints, admin submission collection endpoints, admin warehouse refresh endpoints, public warehouse query endpoints, and warehouse logic live in vertical OJ modules such as `training-data-codeforces`; this web module runs and secures them.

## Dependency And Layer Rules

- May depend on `auth-core` for JWT decoding and URL authorization helpers.
- May depend on OJ modules such as `training-data-codeforces`.
- Must not move OJ-specific parsing, records, writers, or SQL resources into this runtime module.
- Admin APIs must stay under `/api/training-data/admin/**`; guest endpoints must not parse JWTs.
- Public Codeforces handle lookup and DWD/DWM/DWS warehouse queries are guest endpoints; Codeforces handle creation, identity migration, submission collection, and warehouse refresh are admin endpoints.

## File Responsibilities

- `TrainingDataWebApplication.java` - Spring Boot entrypoint.
- `TrainingDataModuleController.java` - `/health` and `/module-info`, including `codeforces-handle-account`, `codeforces-warehouse-refresh`, and `codeforces-submission-collector` feature flags.
- `TrainingDataSecurityConfig.java` - builds the admin protected chain and guest public chain using `PlatformSecurityConfig`.
- `TrainingDataJwtProperties.java` - typed RSA public-key settings for validating platform JWTs.
- `application.yml` - service port, datasource, Flyway, logging, auth public-key defaults, disabled-by-default Codeforces collector schedule entries, and Codeforces source timeout/retry defaults.
- `logback-spring.xml` - file logging configuration following the project logging contract.
- `src/test/java/com/custacm/platform/trainingdata/web/CodeforcesHandleAccountHttpIntegrationTest.java` - verifies Flyway, HTTP routing, admin writes, public lookup, and identity migration for Codeforces handle accounts.
- `src/test/java/com/custacm/platform/trainingdata/web/CodeforcesSubmissionCollectionHttpIntegrationTest.java` - verifies admin recent-lookback submission collection by `studentIdentity`, identity-to-handle resolution, mocked Codeforces source access, and ODS writes.
- `src/test/java/com/custacm/platform/trainingdata/web/CodeforcesWarehouseRefreshHttpIntegrationTest.java` - verifies admin ODS ingest followed by synchronous SQL task DAG refresh, manual resume from a task id, and invalid resume-node handling.
- `src/test/java/com/custacm/platform/trainingdata/web/TrainingDataSecurityConfigTest.java` - verifies training-data admin endpoints require admin JWTs while guest endpoints, including Codeforces warehouse queries, ignore bearer tokens.
