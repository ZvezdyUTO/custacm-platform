# GitHub Automation Agent Notes

This directory owns PR templates, CODEOWNERS, and GitHub Actions.

- Keep the required branch protection check name aligned with the workflow job name `verify`.
- If CI commands change, update [../docs/agent/quality-gates.md](../docs/agent/quality-gates.md).
- If PR workflow expectations change, update [../CONTRIBUTING.md](../CONTRIBUTING.md) and [../docs/agent/doc-sync.md](../docs/agent/doc-sync.md).
- Keep workflow steps deterministic and free of repository secrets unless the feature explicitly needs them.
