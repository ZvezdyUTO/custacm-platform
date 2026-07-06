# Scripts Agent Notes

This directory owns repository operation scripts.

- Keep scripts compatible with the target shell already used in this repo.
- Do not add scripts that require real secrets in committed files.
- If a script changes deployment behavior, update [../deploy/UPDATE.md](../deploy/UPDATE.md) and [../docs/server-deployment.md](../docs/server-deployment.md).
- If doc-sync behavior changes, update [../docs/agent/doc-sync.md](../docs/agent/doc-sync.md) and [../docs/doc-sync-map.tsv](../docs/doc-sync-map.tsv).
- If test-policy behavior changes, update [../docs/agent/quality-gates.md](../docs/agent/quality-gates.md) and [../docs/test-policy-allowlist.tsv](../docs/test-policy-allowlist.tsv) when needed.

Current scripts:

- `deploy.sh` - local Compose deploy using `deploy/.env`, including the frontend static build step.
- `server-deploy.sh` - server fast-forward deploy flow, including the frontend static build step.
- `update-module.sh` - single module update for `auth-web`, `training-data-web`, or `frontend`; frontend updates rebuild `frontend/dist` and reload Nginx instead of rebuilding a frontend image.
- `auto-update-main.sh` - conservative fast-forward auto update with path classification for auth, training-data, and frontend runtime containers.
- `seed-local-codeforces-data.sh` - local fixture seeding through real auth/training-data HTTP APIs without printing tokens or passwords.
- `install-log-mcp-server.sh` - installs the pinned read-only log MCP helper.
- `check-doc-sync.sh` - verifies code/config changes include matching doc updates.
- `check-test-policy.sh` - verifies Java modules have tests and generated test/coverage reports unless allowlisted.

`auto-update-main.sh` classifies changes under `platform-auth/auth-domain`, `auth-app`, `auth-core`, `auth-infra`, and `auth-web` as `auth-web` module updates; `platform-training-data/training-data-codeforces` and `training-data-web` as `training-data-web`; and `frontend/*` as `frontend`.
