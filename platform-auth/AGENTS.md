# platform-auth Agent Notes

`platform-auth` is the first runnable backend slice. It adapts Keycloak JWTs into platform business identity.

Current Maven modules:

- `auth-core` - JWT claim parsing, role extraction, current-user helpers.
- `auth-interface` - cross-module DTOs/contracts.
- `auth-web` - Spring Boot HTTP entrypoint and controllers.

Rules:

- Keycloak remains the login and token issuer.
- Do not add local password login, registration, reset password, demo-token, or self-issued JWT flows.
- Business identity is `studentIdentity`, sourced from JWT claim `student_identity`.
- Business role is one string: `admin` or `student`.
- If both roles are present, `admin` wins.

Before changing this module, read the child module `AGENTS.md` file for the exact layer you are editing.
