# platform-common Agent Notes

- `platform-common` is a shared Maven area, not a business module or runnable service.
- Do not place business entities, module-specific tables or transport contracts here.
- Add shared code only for a demonstrated repeated need, and keep it independent of concrete business modules.
- `common-core` owns the reusable SQL task DAG executor; callers own business manifests, SQL, HTTP validation and security.
- New or materially changed executable logic requires focused tests and an updated nearest README.
- Before backend log changes read [logging.md](../docs/logging.md). Verify Java changes from the repository root with `mvn clean test`.
- Update [architecture.md](../docs/architecture.md) only when the shared-module boundary changes.
