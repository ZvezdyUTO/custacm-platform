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

OJ-specific controllers, handle-account endpoints, public warehouse query endpoints, and warehouse logic live in vertical OJ modules such as `training-data-codeforces`; this web module runs and secures them.

## Dependency And Layer Rules

- May depend on `auth-core` for JWT decoding and URL authorization helpers.
- May depend on OJ modules such as `training-data-codeforces`.
- Must not move OJ-specific parsing, records, writers, or SQL resources into this runtime module.
- Admin APIs must stay under `/api/training-data/admin/**`; guest endpoints must not parse JWTs.
- Public Codeforces handle lookup and DWD/DWM/DWS warehouse queries are guest endpoints; Codeforces handle creation and identity migration are admin endpoints.

## File Responsibilities

- `TrainingDataWebApplication.java` - Spring Boot entrypoint.
- `TrainingDataModuleController.java` - `/health` and `/module-info`, including the `codeforces-handle-account` feature flag.
- `TrainingDataSecurityConfig.java` - builds the admin protected chain and guest public chain using `PlatformSecurityConfig`.
- `TrainingDataJwtProperties.java` - typed RSA public-key settings for validating platform JWTs.
- `application.yml` - service port, datasource, Flyway, logging, and auth public-key defaults.
- `logback-spring.xml` - file logging configuration following the project logging contract.
- `src/test/java/com/custacm/platform/trainingdata/web/CodeforcesHandleAccountHttpIntegrationTest.java` - verifies Flyway, HTTP routing, admin writes, public lookup, and identity migration for Codeforces handle accounts.
- `src/test/java/com/custacm/platform/trainingdata/web/TrainingDataSecurityConfigTest.java` - verifies training-data admin endpoints require admin JWTs while guest endpoints, including Codeforces warehouse queries, ignore bearer tokens.
