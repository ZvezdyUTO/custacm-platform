# platform-training-data

`platform-training-data` is an in-process training-data library subsystem used by Blog API. It has no Spring Boot entrypoint, independent HTTP service, authentication or account-management surface.

## Modules

| Module | Responsibility |
| --- | --- |
| `training-data-common` | OJ-neutral identity contracts, query facade, collection orchestration, schedules, jobs, warehouse refresh and purge repositories. |
| `training-data-codeforces` | Codeforces source access, payload parsing, ODS persistence and OJ-specific warehouse adapters/SQL. |
| `training-data-atcoder` | AtCoder source and metadata access, payload parsing, ODS persistence and OJ-specific warehouse adapters/SQL. |

## Dependency And Layer Rules

- Blog API owns users, passwords, roles, OJ-handle administration and every HTTP controller.
- `username` is the business identity. Common owns cross-OJ contracts; each OJ module owns its external payloads, ODS schema and cleaning SQL.
- OJ modules depend on `training-data-common` and `platform-common/common-core`; common does not depend on concrete OJ modules or Spring MVC.
- Handle replacement is allowed only after Blog API purges data for each removed or changed OJ.
- V034 created the normalized `training_member` and `oj_handle_binding` schema. Do not edit V034; removal of the remaining legacy table requires a new migration after production verification.
- Automatic submission collection and AtCoder metadata work are disabled by default.
- HTTP contracts are documented in [docs/api.md](../docs/api.md), authorization in [docs/authorization.md](../docs/authorization.md), and runtime topology in [docs/architecture.md](../docs/architecture.md).

## Directory Layout

```text
platform-training-data/
  pom.xml
  training-data-common/
  training-data-codeforces/
  training-data-atcoder/
```

## Key Entries

| Path | Responsibility |
| --- | --- |
| `pom.xml` | Training multi-module Maven parent. |
| `training-data-common/README.md` | Shared contracts, normalized identity storage and orchestration boundaries. |
| `training-data-codeforces/README.md` | Codeforces source, ODS and warehouse boundaries. |
| `training-data-atcoder/README.md` | AtCoder source, metadata, ODS and warehouse boundaries. |
| `training-data-common/src/main/java/.../app/query/OjWarehouseQueryFacade.java` | Transport-neutral query entrypoint used by Blog API. |
| `training-data-common/src/main/java/.../app/account/OjHandleAccountService.java` | Normalized handle validation and replacement after purge. |
| `training-data-common/src/main/resources/db/migration/V034__normalize_oj_handle_accounts.sql` | Applied normalized identity migration; never edit in place. |

## Verification

Run from the repository root:

```bash
mvn clean test
```
