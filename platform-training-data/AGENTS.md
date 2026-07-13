# platform-training-data Agent Notes

- This Maven parent owns OJ source collection, ODS storage, DWD/DWM/DWS refresh/query, scheduling, in-process jobs and purge libraries. It has no runnable application.
- Blog API is the only Spring Boot runtime and owns security, DataSource/Flyway, logging, usernames, account/OJ handle management and all HTTP adapters.
- `username` is the training business identity. Production reads and writes normalized `training_member` and `oj_handle_binding` rows introduced by V034; the legacy `oj_handle_account` table is read only during migration and retained for one migration window.
- Never edit an applied migration to drop the legacy table. Add a later migration only after the normalized schema has been verified in production.
- OJ handle changes are complete-set replacement after Blog API purges data for every removed or changed OJ. Training code must not expose a shortcut that overwrites a bound handle without purge.
- Collection state is only per-user, per-OJ `lastCollectedAt`. Missing state means full history; later runs begin at `lastCollectedAt - lookback`; only successful completion advances the cursor.
- Multi-user accepted summaries must use the repository batch method and a single handle-set SQL query. Do not fall back to looping over the single-user repository method.
- Submission schedules and AtCoder problem metadata bootstrap/schedule are disabled by default. Manual jobs remain available; automatic work needs explicit operator configuration.
- OJ modules own their source clients, payload parsers, ODS models/writers/migrations, purge adapters, cleaning SQL, manifests and focused tests.
- Do not add auth, JWT, account HTTP, public training controllers, cross-OJ physical ODS tables or persistent pipeline job state here without a product decision.
- DWD/DWM/DWS refresh remains idempotent SQL. External-source tests use fixtures, fakes or local servers.
- Before backend log changes read `docs/logging.md`; update the nearest README and documents selected by `docs/doc-sync-map.tsv`.
- Verify Java changes from the repository root with `mvn clean test`; do not weaken or delete tests to make changes pass.
