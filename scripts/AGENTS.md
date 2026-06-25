# Scripts Agent Notes

This directory owns repository operation scripts.

- Keep scripts compatible with the target shell already used in this repo.
- Do not add scripts that require real secrets in committed files.
- If a script changes deployment behavior, update [../deploy/UPDATE.md](../deploy/UPDATE.md) and [../docs/server-deployment.md](../docs/server-deployment.md).
- If doc-sync behavior changes, update [../docs/agent/doc-sync.md](../docs/agent/doc-sync.md) and [../docs/doc-sync-map.tsv](../docs/doc-sync-map.tsv).
- If test-policy behavior changes, update [../docs/agent/quality-gates.md](../docs/agent/quality-gates.md) and [../docs/test-policy-allowlist.tsv](../docs/test-policy-allowlist.tsv) when needed.

Current scripts:

- `deploy.sh` - local Compose up/build using `deploy/.env`.
- `server-deploy.sh` - server fast-forward deploy flow.
- `update-module.sh` - single business container rebuild/restart.
- `auto-update-main.sh` - conservative fast-forward auto update.
- `install-log-mcp-server.sh` - installs the pinned read-only log MCP helper.
- `check-doc-sync.sh` - verifies code/config changes include matching doc updates.
- `check-test-policy.sh` - verifies Java modules have tests and generated test/coverage reports unless allowlisted.
