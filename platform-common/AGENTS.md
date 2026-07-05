# platform-common Agent Notes

`platform-common` is a shared Maven area, not a business module and not a runnable service.

Current state:

- `common-core` contains the reusable SQL task DAG execution core. It reads a YAML manifest on every run, rebuilds an adjacency-list graph, validates DAG shape, and executes SQL task nodes with one transaction per node.
- `common-web` is an empty jar module.

Rules:

- Do not put business entities here.
- Add shared code only after a repeated concrete need exists.
- If executable code is added, add focused tests and update this file plus [../docs/architecture.md](../docs/architecture.md).
- Keep common code independent of concrete business modules.
