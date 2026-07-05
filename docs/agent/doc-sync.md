# Documentation Sync

The repository is agent-developed, so every meaningful code or configuration change must update the docs that future agents will read.

## Enforcement

[../../scripts/check-doc-sync.sh](../../scripts/check-doc-sync.sh) checks changed files against [../doc-sync-map.tsv](../doc-sync-map.tsv).

Run locally:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
```

The GitHub workflow runs the same check on PRs.

## How The Map Works

Each non-comment row in [../doc-sync-map.tsv](../doc-sync-map.tsv) has three tab-separated columns:

```text
changed-path-pattern    required-docs-csv    reason
```

If a changed file matches the pattern, at least one listed required doc must also change in the same diff. This is intentionally conservative: it forces agents to either update the docs or make a deliberate docs-only/no-runtime change.

## Required Agent Behavior

- API behavior changed: update [../api.md](../api.md) and the relevant module `AGENTS.md`.
- HTTP authorization behavior changed: update [../authorization.md](../authorization.md), [../api.md](../api.md), and the relevant module `AGENTS.md`.
- Module boundary changed: update [../architecture.md](../architecture.md), [context-map.md](context-map.md), and module `AGENTS.md`.
- Runnable service status changed: update [README.md](../../README.md), [../architecture.md](../architecture.md), [context-map.md](context-map.md), and [README.md](README.md).
- Tests changed materially: update the module `TESTING.md`.
- Deployment or scripts changed: update `deploy/` docs, [../server-deployment.md](../server-deployment.md), or [../../scripts/AGENTS.md](../../scripts/AGENTS.md).
- Auto-update path classification changed: update [../../deploy/UPDATE.md](../../deploy/UPDATE.md), [quality-gates.md](quality-gates.md), and [../../scripts/AGENTS.md](../../scripts/AGENTS.md).
- Logging changed: read and update [../logging.md](../logging.md) if the logging contract changes.
- PR outcome changed: update [../../CHANGELOG.md](../../CHANGELOG.md) using [changelog.md](changelog.md).
- New module slice added: add a module `AGENTS.md`, add tests or explain why not, update [context-map.md](context-map.md), [../architecture.md](../architecture.md), and [../doc-sync-map.tsv](../doc-sync-map.tsv).

## When A Code Change Needs No Doc Change

Prefer updating docs anyway. If a change is truly internal and no documented behavior or decision changes, mention that in the PR. If the sync check blocks a legitimate change, update [../doc-sync-map.tsv](../doc-sync-map.tsv) with a narrower rule instead of bypassing the check silently.
