# blog-api

`blog-api` is the project's only runnable Spring Boot backend. It runs Java 21 and Spring Boot 3.5, keeps NBlog's `top.naccl` package root, and composes the training-data modules as in-process libraries.

Browsers reach it through Nginx `/api/**`; direct backend routes do not include the `/api` prefix.

## Responsibilities

- Blog articles, comments, profiles, competitions, homepage content and locally managed images.
- BCrypt login, HS512 JWT issuance, roles, users and OJ-handle administration.
- Public, player and admin HTTP adapters, including training query and collection-job adapters.
- One shared MySQL `DataSource`, transaction manager and Flyway history for Blog and training schemas.
- Redis-backed runtime controls and caches, with security-sensitive controls failing closed.

Detailed endpoints and payloads belong to [the API contract](../../../../docs/api.md). Route tiers and resource ownership belong to [the authorization contract](../../../../docs/authorization.md).

## Dependency And Layer Rules

- `blog-api` may depend on training application contracts and OJ implementations; training modules must not depend on `top.naccl`.
- HTTP binding, authentication, authorization, account management and OJ-handle management stay in this module.
- `username` is the JWT subject and training identity. Stored roles are exactly `ROLE_admin` and `ROLE_player`.
- HS512 signing configuration is validated when the application context starts: the secret is mandatory, must not be a placeholder and must contain at least 64 UTF-8 bytes.
- Authorization parsing accepts only the `Bearer <JWT>` scheme (case-insensitive scheme); raw or malformed token headers are rejected on protected routes.
- Controllers return real HTTP 4xx/5xx statuses for failures; `ControllerExceptionHandler` owns the stable error envelope instead of controllers returning a failed envelope with HTTP 200.
- Category and tag names, article-tag pairs and taxonomy references rely on database constraints. Services translate duplicate-key, stale-reference write and referenced-delete failures to HTTP 409; controllers do not perform check-then-write or count-then-delete queries.
- Player writes derive ownership from the authenticated identity. Administrator capabilities do not allow a player endpoint to impersonate another user.
- OJ-handle removal or replacement is coordinated by the user service only after the affected OJ data is purged.
- The fixed `root` administrator remains protected from deletion, rename, demotion and training-member configuration.
- Database migrations are append-only once applied. Blog API loads its own migrations and the migration resources supplied by training dependencies.
- Retired NBlog features and remote-upload channels must not be reintroduced without an explicit product decision.
- Before changing backend logs, follow [docs/logging.md](../../../../docs/logging.md).

## Directory Layout

```text
src/main/java/top/naccl/
  config/       security, JWT, infrastructure and training-module assembly
  controller/   public, player and admin HTTP adapters
  mapper/       MyBatis mapper interfaces
  model/        request and response models
  repository/   JDBC-backed aggregate repositories
  service/      Blog, identity and asset use cases
src/main/resources/
  mapper/       MyBatis XML
  db/migration/ Blog schema migrations
src/test/java/top/naccl/
```

## Key Entries

| Path | Responsibility |
| --- | --- |
| `pom.xml` | Backend dependencies, tests and Spring Boot packaging. |
| `src/main/java/top/naccl/BlogApiApplication.java` | Application entrypoint. |
| `src/main/java/top/naccl/config/SecurityConfig.java` and `JwtFilter.java` | URL authorization tiers and current-database-user JWT authentication. |
| `src/main/java/top/naccl/util/JwtUtils.java` | Immutable HS512 token signing/parsing and startup-time secret validation. |
| `src/main/java/top/naccl/handler/ControllerExceptionHandler.java` | HTTP status and stable `errorCode` mapping for request, domain and infrastructure failures. |
| `src/main/java/top/naccl/controller/support/PageRequestValidator.java` | Shared bounded controller pagination validation. |
| `src/main/java/top/naccl/model/vo/PageCommentPage.java` | Bounded comment-page result, including reply truncation state. |
| `src/main/java/top/naccl/config/TrainingDataModuleConfiguration.java` | In-process training module assembly. |
| `src/main/java/top/naccl/controller/` | Anonymous Blog read adapters. |
| `src/main/java/top/naccl/controller/player/` | Authenticated self-service and training query adapters. |
| `src/main/java/top/naccl/controller/admin/` | Administrator management adapters. |
| `src/main/java/top/naccl/service/impl/AdminUserService.java` | Atomic user updates, protected-admin rules and purge-before-handle-replacement orchestration. |
| `src/main/java/top/naccl/service/CompetitionService.java` | Competition aggregate validation, projection and lifecycle. |
| `src/main/java/top/naccl/service/ArticleRecycleBinService.java` | Article retention, restore and expiry cleanup transaction. |
| `src/main/java/top/naccl/service/ImageAssetService.java` | Managed image ownership, binding and lifecycle. |
| `src/main/resources/application.properties` | Database, Redis, JWT, uploads, cache and opt-in collection configuration. |
| `src/main/resources/db/migration/` | Append-only Blog schema history. |
| `src/main/resources/mapper/CommentMapper.xml` | Root-comment paging and capped current-page recursive reply traversal. |
| `src/test/java/top/naccl/` | Security, controller, service, migration and integration-focused tests. |

This table is a navigation map, not an exhaustive inventory. Use `rg --files` and code references for the current file set.

## Verification

Run Java verification from the repository root:

```bash
mvn clean test
```

If packaging or container-image behavior changes, also run:

```bash
mvn clean package -DskipTests
```
