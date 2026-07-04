# Agent Operating Map

This is the fast entry point for future agents. It records how to navigate the repository without rereading every file.

## Ground Rules

- Treat [../../AGENTS.md](../../AGENTS.md) as the hard rule source.
- Do not invent product behavior from directory names. If a module is a placeholder, keep it documented as a placeholder.
- Before editing a directory, read the nearest `AGENTS.md` in that directory or its parent.
- Keep `student_identity` as one immutable business identity string unless the user explicitly changes the decision.
- Keep Keycloak as the only login and token issuer unless the user explicitly changes the decision.

## Current Working Shape

- Root Maven reactor includes `platform-common`, `platform-auth`, and `platform-training-data`.
- `platform-auth/auth-web` is the first runnable backend implementation.
- `platform-training-data/training-data-web` is the second runnable backend implementation. It exposes an admin-only Codeforces ODS batch-upsert API and applies Codeforces ODS/DWD/DWM/DWS table migrations.
- `platform-common` currently contains empty shared Maven modules.
- `platform-blog`, `platform-editor`, `platform-article-storage`, and `frontend` are placeholders.
- Local deployment is under `deploy/` and currently starts Keycloak, Keycloak PostgreSQL, and `custacm-backend`.

## Agent Workflow

1. Identify the changed area with [context-map.md](context-map.md).
2. Read the nearest module `AGENTS.md`.
3. Read the relevant contract docs:
   - API changes: [../api.md](../api.md)
   - architecture/module changes: [../architecture.md](../architecture.md)
   - logging changes: [../logging.md](../logging.md)
   - deployment changes: [../../deploy/README.md](../../deploy/README.md), [../../deploy/UPDATE.md](../../deploy/UPDATE.md), [../server-deployment.md](../server-deployment.md)
4. Make the code/docs change.
5. Run the relevant checks from [quality-gates.md](quality-gates.md).
6. Update [../../CHANGELOG.md](../../CHANGELOG.md) using [changelog.md](changelog.md).
7. Run [../../scripts/check-doc-sync.sh](../../scripts/check-doc-sync.sh) before PR.

## When Unsure

Write uncertainty as `<!-- TODO: ... -->` in docs, or state it in the PR. Do not silently turn assumptions into project rules.
