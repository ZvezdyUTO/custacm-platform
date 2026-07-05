# platform-auth Agent Notes

`platform-auth` is the first runnable backend slice. It owns local accounts, password hashing, user management, RSA JWT issuance, and platform JWT parsing.

Current Maven modules:

- `auth-domain` - account domain model, account roles, and repository contracts.
- `auth-app` - login, password change, failed-login retry cooldown, admin user-management use cases, and ports.
- `auth-core` - platform JWT claim parsing, role extraction, current-user helpers, URL authorization helpers, and RSA public-key decoder helpers.
- `auth-infra` - JDBC user repository, BCrypt password hashing, RSA JWT issuer, and Flyway migration.
- `auth-web` - Spring Boot HTTP entrypoint, controllers, and auth HTTP DTOs.

Rules:

- Do not reintroduce Keycloak, demo-token, in-memory login, or public registration unless the identity decision changes explicitly.
- Business identity is `studentIdentity`, sourced from JWT subject `sub`.
- Business role is one string when authenticated. Stored account roles are `admin`, `player`, or `disable`; `disable` accounts cannot authenticate. Unauthenticated access has no account role and no JWT role value.
- HTTP authorization uses the shared tiers in [../docs/authorization.md](../docs/authorization.md): `/admin/**` requires admin, `/player/**` requires player or admin, and guest endpoints must not parse JWTs.
- Passwords must be stored as hashes; never persist or return plaintext passwords.
- JWTs are signed by `auth-web` with an RSA private key and verified by services with the matching public key.

Before changing this module, read the child module `AGENTS.md` file for the exact layer you are editing.
