# ADR 0003: Agent Documentation Governance

## Status

Accepted.

## Decision

Documentation is part of the agent development contract. Agents should navigate through root `AGENTS.md`, `docs/`, and local module `AGENTS.md` files. Changes to code, CI, scripts, deployment, or module boundaries must update matching docs according to `docs/doc-sync-map.tsv`.

## Consequences

- CI runs `scripts/check-doc-sync.sh` before Maven verification.
- Module-level `AGENTS.md` files should stay short and factual.
- If the sync map blocks a legitimate change, narrow or extend the map instead of bypassing it.
- Unknowns must stay explicit as TODOs rather than being turned into guessed facts.
