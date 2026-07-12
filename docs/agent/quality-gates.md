# Quality Gates

Use the smallest check that proves the changed surface, then run broader checks when the blast radius crosses modules.

| Change type | Required check |
| --- | --- |
| Java code, POM, auth parsing, controller behavior | `mvn clean test` |
| Backend logging code or `logback-spring.xml` | `mvn clean test` and `git diff --check` |
| Backend Dockerfile or package/image behavior | `mvn clean package -DskipTests` |
| Compose or env example changes | `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` |
| Frontend runtime changes | `cd frontend && pnpm lint && pnpm test && pnpm typecheck && pnpm build`, `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`, and rendered browser smoke testing |
| Deployment entrypoint changes | Run `./scripts/dev.sh <safe-local-env-file>` and verify Vite 4180/5173 plus API 8090; then run `./scripts/deploy.sh <safe-local-env-file>` and verify all four Compose services and production URLs |
| Local sample-data seed script changes | Run `./scripts/seed-local-codeforces-data.sh` against local auth/training-data services when they are available |
| Documentation sync rules | `./scripts/check-doc-sync.sh origin/main WORKTREE` |
| Docs-only changes | No Maven required unless examples/config changed |

Training-data storage changes should include focused tests for fixture parsing, OJ-specific ODS idempotency, writer wiring, Blog API collection/security behavior, and DWD/DWM/DWS SQL task behavior. OJ-specific tests belong in that OJ Maven module; integrated HTTP/security tests belong in Blog API. Codeforces external parsing and warehouse SQL checks must stay fixture-backed in default tests. JDBC unit tests use H2 in MySQL compatibility mode; final Flyway/foreign-key integration should also be exercised against local MySQL when available.

## CI

GitHub Actions runs:

```bash
./scripts/check-doc-sync.sh
mvn clean test
```

The protected `main` branch requires the `verify` check to pass before ordinary PRs can merge.

## Test Policy

The required `verify` check compiles the Maven reactor and runs all tests already present in the repository. It does not enforce a JaCoCo percentage or require historical code to backfill tests. New or materially changed behavior should include focused tests in the same change, and existing tests must not be deleted, skipped, or weakened merely to pass CI. JaCoCo reports remain optional local diagnostics rather than an MR gate.
