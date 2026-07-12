# Agent Instructions

## Project Goal

This repository is the skeleton for `custacm-platform`, a training-team integrated platform.

Current phase: operate the integrated Blog API backend and the Vue 3 Blog / Vue 3 Training frontend gateway.

## Current Scope

- `platform-blog/upstream/nblog/blog-api` is the only runnable Spring Boot backend.
- Blog API owns BCrypt passwords, user management, HS512 JWT issuance, roles, and OJ handles.
- `username` is the JWT subject and the only training business identity.
- Stored roles are exactly `ROLE_admin` and `ROLE_player`; guest is implicit unauthenticated access.
- `platform-training-data` retains Codeforces/AtCoder collection, ODS/DWD/DWM/DWS processing, query, scheduling, and purge libraries.
- `platform-blog/upstream/nblog/blog-view` is the Vue 3 + Vite public Blog at `/`.
- `frontend` is the Vue 3 Training application at `/training/**`; its admin pages cover user management, training-data management, and homepage image management.
- One Nginx `frontend` service serves both static applications and proxies browser `/api/**` requests to Blog API.
- The supported UI scope is 1280–2560 px desktop, with primary acceptance at 1440×900 and 1920×1080; mobile is outside the current phase.

## Architecture Rules

- Blog API owns the single HTTP layer and composes training application services in-process.
- Do not put business entities in `platform-common`.
- Do not reintroduce demo-token or in-memory login flows unless the user explicitly changes the identity decision.
- Passwords, account management, handles, and token issuance belong to Blog API; there is no public registration flow.
- Other business modules reference users by `username`.
- HTTP APIs must follow `docs/authorization.md`: `/admin/**` is admin-only, `/player/**` is player-or-admin, public GET and OPTIONS are anonymous, and the only anonymous business writes are `POST /login` and `POST /admin/login`.
- The two Vue 3 applications share `custacm.accessToken` and `custacm.user` for login continuity. Public Blog requests must not globally attach JWTs; protected requests attach Bearer tokens explicitly.
- Keep Vue Blog routing under `/`, Vue Training routing under `/training/**`, and browser API routing under `/api/**`; do not couple the two frontend routers.
- Keep training domain/app/infra boundaries clear and keep `top.naccl` as NBlog's package root.

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
mvn clean test
```

`mvn clean test` compiles the reactor and runs all existing unit tests. Historical code is not required to backfill unit tests or meet a repository-wide coverage threshold.

Test standard:

- New or materially changed business logic, JWT/security parsing, HTTP controller behavior, and client/adapters should add focused unit tests in the same change.
- Existing tests must continue to pass; do not delete, skip, or weaken tests merely to make a change pass.
- Historical code without tests does not need unrelated test backfills.
- JaCoCo reports may be inspected locally as a diagnostic aid, but coverage percentage is not an MR gate.

If packaging or Docker image behavior changes, also run:

```bash
mvn clean package -DskipTests
```

For deployment configuration changes, run the relevant config checks, for example:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

For local deployment, use the Compose stack under `deploy/`, which starts exactly `blog-db`, `blog-redis`, `blog-api`, and the shared-Nginx `frontend` service.

For docs-only changes, Maven verification is not required.
