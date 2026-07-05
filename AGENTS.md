# Agent Instructions

## Project Goal

This repository is the skeleton for `custacm-platform`, a training-team integrated platform.

Current phase: build an evolvable backend framework, not the full product.

## Current Scope

- `platform-auth/auth-web` owns login, password hashing, user management, and JWT token issuance for this project.
- `platform-auth/auth-web` is the first runnable backend implementation.
- `platform-training-data/training-data-web` is the second runnable backend implementation. It exposes admin-only OJ-specific ODS batch-upsert APIs and applies training-data SQL migrations with Flyway.
- Student identity is a single immutable string in the format `fixed-length student number + real name`, for example `112487张三`.
- `studentIdentity` is the only user ID used by platform business code.
- `platform-auth` stores local accounts in MySQL, hashes passwords with BCrypt, signs JWTs with an RSA private key, and exposes `studentIdentity` plus one `role`.
- Stored account roles are `admin`, `player`, and `disable`; `disable` accounts cannot authenticate. JWT roles are only `admin` or `player`. `guest` is implicit unauthenticated access; business responses use a single role string, not a role list.
- `platform-auth/auth-core` contains platform JWT parsing and current-user extraction helpers.
- `platform-blog`, `platform-editor`, `platform-article-storage`, `frontend`, and `deploy` are placeholders.
- Do not implement all placeholder modules at once. Add one runnable slice at a time.

## Architecture Rules

- Business modules expose functionality upward through their own `*-web` HTTP layer.
- Other modules should call those HTTP APIs through local client/adapters when needed.
- Do not put business entities in `platform-common`.
- Do not reintroduce demo-token or in-memory login flows unless the user explicitly changes the identity decision.
- Passwords, account management, and token issuance belong to `platform-auth`; there is no public registration flow.
- Do not split `studentIdentity` into separate student-number/name fields unless explicitly requested. The project decision is to treat it as one immutable business identity string.
- Other business modules should reference users by `studentIdentity`.
- HTTP APIs must follow the URL authorization tiers in `docs/authorization.md`: `/admin/**` is admin-only, `/player/**` is player-or-admin, and guest endpoints are public and must not parse or depend on JWTs.
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
- Never log passwords, tokens, cookies, Authorization headers, JWT signing keys, database passwords, or full personal sensitive data.

## Documentation Rules

- Treat `docs/README.md` as the documentation index and `docs/agent/README.md` as the fast context entry for future agents.
- Before editing a module, read the nearest module `AGENTS.md`.
- Every non-placeholder module with source code must have a module-level `README.md` for humans and agents. It must include the module responsibility, directory layout, dependency/layer rules, and a file-level responsibility list. When generating, moving, deleting, or materially changing module files, create or update that module README in the same change.
- Before opening an MR, update `CHANGELOG.md` using `docs/agent/changelog.md`; it is written by agents but must read naturally for humans.
- When changing code, scripts, CI, deployment configuration, or module boundaries, update the matching docs listed in `docs/doc-sync-map.tsv`.
- Keep `docs/agent/context-map.md` current when top-level directories, runnable services, or module responsibilities change.
- If a fact cannot be proven from current files, write it as a TODO instead of guessing.
- Run `./scripts/check-doc-sync.sh origin/main WORKTREE` before opening a PR when local refs are available.

## Git Rules

- Do not commit unless the user explicitly asks.
- Do not push unless the user explicitly asks.
- If the user says to push, treat that as permission to commit and push.
- PRs/MRs opened by anyone other than the project owner must not be merged until the project owner explicitly confirms approval.
- PRs/MRs opened by the project owner do not need an additional review confirmation; the owner's explicit merge instruction is enough.
- MR titles and descriptions must be written in Chinese.

## Verification

Use this check after Java changes:

```bash
mvn clean verify
./scripts/check-test-policy.sh
```

`mvn clean verify` runs unit tests and JaCoCo coverage checks.
`check-test-policy.sh` verifies that Java modules with executable source have tests and generated Surefire/JaCoCo reports unless explicitly allowlisted.

Coverage standard:

- Code-bearing Maven modules must keep JaCoCo line coverage at or above `70%`.
- Spring Boot startup classes and configuration classes are excluded from the coverage gate.
- New business logic, JWT/security parsing, HTTP controller behavior, and client/adapters should add focused unit tests in the same change.
- Placeholder-only modules and empty modules do not need tests until they contain executable code.
- DTO-only modules without behavior may be listed in `docs/test-policy-allowlist.tsv`; do not use the allowlist for business logic.

If packaging or Docker image behavior changes, also run:

```bash
mvn clean package -DskipTests
```

For deployment configuration changes, run the relevant config checks, for example:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

For local deployment, use the Compose stack under `deploy/`, which starts auth MySQL and the backend.

For docs-only changes, Maven verification is not required.
