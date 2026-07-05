# Documentation Index

This repository is expected to be developed by agents. Documentation is part of the engineering surface, not an afterthought.

## Read Order For Agents

1. [../AGENTS.md](../AGENTS.md) - hard project rules.
2. [agent/README.md](agent/README.md) - agent operating map.
3. [agent/context-map.md](agent/context-map.md) - directory and module map.
4. The nearest module `AGENTS.md` before editing a module.
5. [agent/doc-sync.md](agent/doc-sync.md) and [doc-sync-map.tsv](doc-sync-map.tsv) before opening a PR.

## Document Layers

| Layer | Files | Purpose |
| --- | --- | --- |
| Root rules | [../AGENTS.md](../AGENTS.md), [../CONTRIBUTING.md](../CONTRIBUTING.md) | Mandatory rules for agents and contributors. |
| Planning | [../TODO.md](../TODO.md), [../CHANGELOG.md](../CHANGELOG.md) | Human-readable todo list and MR outcome history. |
| Agent map | [agent/README.md](agent/README.md), [agent/context-map.md](agent/context-map.md), [agent/doc-sync.md](agent/doc-sync.md), [agent/changelog.md](agent/changelog.md), [agent/quality-gates.md](agent/quality-gates.md) | Fast context, update rules, changelog format, and verification commands. |
| Architecture | [architecture.md](architecture.md), [authorization.md](authorization.md), [adr/](adr/) | Current architecture, HTTP authorization tiers, and durable decisions. |
| API | [api.md](api.md) | Implemented HTTP contracts for auth and training-data services only. |
| Operations | [logging.md](logging.md), [server-deployment.md](server-deployment.md), [../deploy/README.md](../deploy/README.md), [../deploy/UPDATE.md](../deploy/UPDATE.md) | Runtime, logging, deployment, and update workflows. |
| Module docs | `*/README.md`, `*/AGENTS.md`, `*/TESTING.md`, `platform-training-data/docs/*.md` | Local module context, warehouse contracts, fixtures, and test coverage notes. |

Training-data module details start at [../platform-training-data/README.md](../platform-training-data/README.md). Codeforces warehouse table grain, app query semantics, source collection, SQL task resources, and admin refresh semantics are maintained in [../platform-training-data/docs/ods-submission.md](../platform-training-data/docs/ods-submission.md).

## Update Rule

When changing code, scripts, CI, deployment configuration, or module boundaries, update the matching docs in [doc-sync-map.tsv](doc-sync-map.tsv). The CI job runs [../scripts/check-doc-sync.sh](../scripts/check-doc-sync.sh) to enforce this for PRs.

Docs-only changes should still keep links and facts consistent, but they do not require Maven verification unless they change executable examples or configuration.

Current identity and login decisions are recorded in [adr/0004-platform-owns-auth.md](adr/0004-platform-owns-auth.md).
