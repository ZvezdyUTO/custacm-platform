# platform-training-data Agent Notes

- This Maven parent provides OJ collection, ODS storage, warehouse refresh/query, scheduling, in-process jobs and purge libraries. It has no runnable application.
- Blog API is the only Spring Boot runtime and owns HTTP, authentication, users, OJ-handle administration, `DataSource`, Flyway and runtime logging.
- `username` is the training identity. Production repositories use `training_member` and `oj_handle_binding` created by V034.
- Never edit applied migration V034. The legacy `oj_handle_account` table still exists in the schema; removing it requires a later migration after explicit production verification.
- Handle updates are complete-set replacement. Blog API must purge every removed or changed OJ before replacing its binding; do not add a bypass.
- Collection progress is per user and OJ. Only a successful collection advances `lastCollectedAt`; missing state means full history.
- Multi-user summaries must use the batch repository path and one handle-set query, not a loop over the single-user query.
- OJ modules own source clients, payloads, ODS models/writers, migrations, purge adapters and warehouse SQL. Do not add auth, public controllers, cross-OJ ODS tables or persistent job state here without a product decision.
- Submission schedules and AtCoder metadata bootstrap/schedules remain disabled by default and require explicit operator configuration.
- External-source tests use fixtures, fakes or local servers, never live external services.
- Before backend log changes read [logging.md](../docs/logging.md). Verify Java changes from the repository root with `mvn clean test`.
- Update the nearest README and the relevant architecture, API or authorization document when a public contract or module boundary changes.
