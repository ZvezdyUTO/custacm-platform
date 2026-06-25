# Docs Agent Notes

This directory owns repository documentation.

Before editing docs:

- Keep facts aligned with code and config that actually exist.
- Prefer links to existing source documents over duplicated long explanations.
- Update [doc-sync-map.tsv](doc-sync-map.tsv) when a new code area or module doc is added.
- Keep [agent/context-map.md](agent/context-map.md) current when top-level directories, modules, or runnable services change.
- Do not document future product behavior as implemented behavior.

Docs-only changes do not require Maven verification, but they should pass:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
```
