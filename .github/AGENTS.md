# GitHub Automation Agent Notes

This directory owns PR templates, CODEOWNERS, and GitHub Actions.

- Keep the required branch protection check name aligned with the workflow job name `verify`.
- If CI commands change, update [../docs/agent/quality-gates.md](../docs/agent/quality-gates.md).
- If PR workflow expectations change, update [../CONTRIBUTING.md](../CONTRIBUTING.md) and [../docs/agent/doc-sync.md](../docs/agent/doc-sync.md).
- PR templates and automation notes must reflect that project-owner PRs need no extra review confirmation, while PRs from anyone else require explicit project-owner approval before merge.
- Keep workflow steps deterministic and free of repository secrets unless the feature explicitly needs them.
