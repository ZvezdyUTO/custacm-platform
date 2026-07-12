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
| Project entry | [../README.md](../README.md) | Human-facing overview of the training workspace and Blog platform for team knowledge sharing and visitor exchange; maintenance links stay collapsed. |
| Root rules | [../AGENTS.md](../AGENTS.md), [../CONTRIBUTING.md](../CONTRIBUTING.md) | Mandatory rules for agents and contributors. |
| Planning | [../TODO.md](../TODO.md), [../CHANGELOG.md](../CHANGELOG.md) | Human-readable todo list and MR outcome history. |
| Agent map | [agent/README.md](agent/README.md), [agent/context-map.md](agent/context-map.md), [agent/doc-sync.md](agent/doc-sync.md), [agent/changelog.md](agent/changelog.md), [agent/quality-gates.md](agent/quality-gates.md) | Fast context, update rules, changelog format, and verification commands. |
| Architecture | [architecture.md](architecture.md), [authorization.md](authorization.md), [adr/](adr/) | Current architecture, HTTP authorization tiers, and durable decisions. |
| API | [api.md](api.md) | Implemented Blog API, user-management, and training-data HTTP contracts. |
| Operations | [logging.md](logging.md), [server-deployment.md](server-deployment.md), [../deploy/README.md](../deploy/README.md), [../deploy/UPDATE.md](../deploy/UPDATE.md) | Runtime, logging, deployment, and update workflows. |
| Module docs | `*/README.md`, `*/AGENTS.md`, `*/TESTING.md` | Local module boundaries, file responsibilities, and test notes. |

The monolith boundary starts at [../platform-blog/README.md](../platform-blog/README.md); training internals start at [../platform-training-data/README.md](../platform-training-data/README.md). HTTP contracts remain in [api.md](api.md).

## Update Rule

When changing code, scripts, CI, deployment configuration, or module boundaries, update the matching docs in [doc-sync-map.tsv](doc-sync-map.tsv). The CI job runs [../scripts/check-doc-sync.sh](../scripts/check-doc-sync.sh) to enforce this for PRs.

Docs-only changes should still keep links and facts consistent, but they do not require Maven verification unless they change executable examples or configuration.

The Java test policy and the exact GitHub gate command are defined in [agent/quality-gates.md](agent/quality-gates.md): CI runs existing tests, while focused tests are expected for new or materially changed behavior without forcing historical coverage backfills.

The current integrated identity and admin-surface decisions, including homepage image management, are documented in [architecture.md](architecture.md); older auth ADRs are historical context.
