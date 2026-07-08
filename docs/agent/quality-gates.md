# Quality Gates

Use the smallest check that proves the changed surface, then run broader checks when the blast radius crosses modules.

| Change type | Required check |
| --- | --- |
| Java code, POM, auth parsing, controller behavior | `mvn clean verify` and `./scripts/check-test-policy.sh` |
| Backend logging code or `logback-spring.xml` | `mvn clean verify`, `./scripts/check-test-policy.sh`, and `git diff --check` |
| Backend Dockerfile or package/image behavior | `mvn clean package -DskipTests` |
| Compose or env example changes | `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` |
| Auto-update classification changes | `./scripts/auto-update-main.sh classify <changed-file>...` with representative paths |
| Frontend runtime changes | `cd frontend && pnpm lint && pnpm test && pnpm typecheck && pnpm build`, `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`, and rendered browser smoke testing |
| Frontend deploy path changes | Run the Compose `frontend-build` service against a local env file, then verify `custacm-frontend` serves `/` and proxies health/API paths |
| Local sample-data seed script changes | Run `./scripts/seed-local-codeforces-data.sh` against local auth/training-data services when they are available |
| Documentation sync rules | `./scripts/check-doc-sync.sh origin/main WORKTREE` |
| Docs-only changes | No Maven required unless examples/config changed |

Training-data storage changes should include focused tests for fixture parsing, OJ-specific ODS idempotency, writer wiring, collection HTTP behavior, and DWD/DWM/DWS SQL task behavior when those layers change. OJ-specific tests belong in that OJ Maven module; shared `training-data-web` security tests belong in `training-data-web`. Codeforces external parsing and warehouse SQL checks must stay fixture-backed in default tests. The current JDBC tests use H2 in MySQL compatibility mode to keep default verification independent of local Docker availability.

## CI

GitHub Actions runs:

```bash
./scripts/check-doc-sync.sh
mvn clean verify
./scripts/check-test-policy.sh
```

The protected `main` branch requires the `verify` check to pass before ordinary PRs can merge.

## Test Policy

`check-test-policy.sh` runs after Maven. It requires Java modules with executable source to either:

- contain `*Test.java` files and generated Surefire plus JaCoCo reports, or
- be explicitly listed in [../test-policy-allowlist.tsv](../test-policy-allowlist.tsv).

The allowlist should stay narrow and only cover modules without executable behavior. New business logic should add focused tests instead of expanding the allowlist.
