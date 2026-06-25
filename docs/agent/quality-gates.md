# Quality Gates

Use the smallest check that proves the changed surface, then run broader checks when the blast radius crosses modules.

| Change type | Required check |
| --- | --- |
| Java code, POM, auth parsing, controller behavior | `mvn clean verify` and `./scripts/check-test-policy.sh` |
| Dockerfile or package/image behavior | `mvn clean package -DskipTests` |
| Compose or env example changes | `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` |
| Documentation sync rules | `./scripts/check-doc-sync.sh origin/main WORKTREE` |
| Docs-only changes | No Maven required unless examples/config changed |

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

The current allowlist is for DTO-only modules without behavior. New business logic should add focused tests instead of expanding the allowlist.
