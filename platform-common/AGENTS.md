# platform-common Agent Notes

`platform-common` is a shared Maven area, not a business module and not a runnable service.

Current state:

- `common-core` is an empty jar module.
- `common-web` is an empty jar module.

Rules:

- Do not put business entities here.
- Add shared code only after a repeated concrete need exists.
- If executable code is added, add focused tests and update this file plus [../docs/architecture.md](../docs/architecture.md).
- Keep common code independent of concrete business modules.
