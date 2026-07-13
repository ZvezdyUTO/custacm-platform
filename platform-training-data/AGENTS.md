# platform-training-data Agent Notes

- This Maven parent owns OJ collection, ODS storage, warehouse refresh/query, scheduling, and purge libraries.
- Blog API is the only Spring Boot runtime and owns security, DataSource/Flyway, logging, usernames, and HTTP adapters.
- `username` is the training business identity; `oj_handle_account` stores handle maps and collection state.
- Collection state is only the per-user, per-OJ `lastCollectedAt` successful window end. Missing state means full-history collection; later runs start at `lastCollectedAt - lookback`. Do not reintroduce a separate full-history completion flag.
- OJ modules own their source clients, payload parsers, ODS models/writers/migrations, purge adapters, cleaning SQL, manifests, and focused tests.
- Do not add auth, JWT, account management, public training controllers, cross-OJ physical ODS tables, or persistent pipeline state here without a new product decision.
- DWD/DWM/DWS refresh remains idempotent SQL. External-source tests use fixtures or fake clients.
- Before log changes read `docs/logging.md`; update the nearest README and synced architecture/API docs.
- Verify Java changes with root `mvn clean verify` and `./scripts/check-test-policy.sh`.
