# Agent Instructions

## Project Goal

This repository is the skeleton for `custacm-platform`, a training-team integrated platform.

Current phase: build an evolvable backend framework, not the full product.

## Current Scope

- Keycloak is the only login and token issuer for this project.
- `platform-auth` is the only module with a runnable backend implementation.
- Student identity is a single immutable string in the format `fixed-length student number + real name`, for example `112487张三`.
- The Keycloak user attribute and JWT claim name for this value is `student_identity`.
- `studentIdentity` is the only user ID used by platform business code.
- `platform-auth` currently validates Keycloak JWTs and exposes `student_identity` plus one `role`. It does not implement local password login.
- The only platform roles are `admin` and `student`; business responses use a single role string, not a role list.
- `platform-auth/auth-core` contains Keycloak JWT parsing and current-user extraction helpers.
- `platform-training-data`, `platform-blog`, `platform-editor`, `platform-article-storage`, `frontend`, and `deploy` are placeholders.
- Do not implement all placeholder modules at once. Add one runnable slice at a time.

## Architecture Rules

- Business modules expose functionality upward through their own `*-web` HTTP layer.
- Other modules should call those HTTP APIs through local client/adapters when needed.
- Do not put business entities in `platform-common`.
- Do not reintroduce demo-token, in-memory login, or self-issued JWT login flows unless the user explicitly changes the identity decision.
- Passwords, login sessions, registration, reset password, and token issuance belong to Keycloak.
- Do not split `student_identity` into separate student-number/name fields unless explicitly requested. The project decision is to treat it as one immutable business identity string.
- Other business modules should reference users by `studentIdentity`.
- Keep module boundaries clear:
  - `*-domain`: entities, domain types, repository interfaces, domain services.
  - `*-interface`: cross-module DTOs, request/response contracts, client contracts.
  - `*-app`: application services and use-case orchestration.
  - `*-infra`: repository implementations, memory/database adapters, remote clients.
  - `*-web`: Spring Boot entrypoint and controllers.
- The current package root is `com.custacm.platform`.

## Logging Rules

- Before adding or changing backend logs, read `docs/logging.md`.
- Use Spring Boot's default SLF4J/Logback stack; do not introduce a custom logging system or heavy log platform in the current phase.
- Error logs must include a stable `errorCode`.
- After request tracing is implemented, request logs must carry `traceId` through MDC; business code must not generate trace IDs manually.
- Never log plaintext `student_identity`, passwords, tokens, cookies, Authorization headers, Keycloak secrets, or full personal sensitive data.

## Git Rules

- Do not commit unless the user explicitly asks.
- Do not push unless the user explicitly asks.
- If the user says to push, treat that as permission to commit and push.
- MR titles and descriptions must be written in Chinese.

## Verification

Use this check after Java changes:

```bash
mvn clean verify
```

`mvn clean verify` runs unit tests and JaCoCo coverage checks.

Coverage standard:

- Code-bearing Maven modules must keep JaCoCo line coverage at or above `70%`.
- Spring Boot startup classes and configuration classes are excluded from the coverage gate.
- New business logic, JWT/security parsing, HTTP controller behavior, and client/adapters should add focused unit tests in the same change.
- Placeholder-only modules and empty modules do not need tests until they contain executable code.

If packaging or Docker image behavior changes, also run:

```bash
mvn clean package -DskipTests
```

For deployment configuration changes, run the relevant config checks, for example:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

For local deployment, use the Compose stack under `deploy/`, which starts Keycloak and the backend.

For docs-only changes, Maven verification is not required.
