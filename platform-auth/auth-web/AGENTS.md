# auth-web Agent Notes

`auth-web` is the runnable Spring Boot service for the auth slice.

Key files:

- `AuthWebApplication.java` - Spring Boot entrypoint rooted at `com.custacm.platform.auth`.
- `AuthController.java` - exposes public login plus player self-service endpoints under `/api/auth/player/**`.
- `AuthModuleController.java` - exposes `GET /health` and `GET /module-info`.
- `AuthSecurityConfig.java` - stateless platform JWT security using shared admin/player protected chains and a guest chain.
- `AuthApplicationConfig.java` - wires JDBC repository, BCrypt, RSA JWT issuer/decoder, services, and bootstrap admin.
- `AdminUserController.java` - admin user-management HTTP API.
- `*Request.java`, `*Response.java` - HTTP-local request/response DTOs for auth endpoints.
- `src/main/resources/application.yml` - port, datasource, Flyway, RSA JWT, and bootstrap admin settings.

Rules:

- `auth-web` issues platform JWTs. Do not reintroduce Keycloak, demo tokens, or public registration unless the identity decision changes explicitly.
- `POST /api/auth/login` accepts a `rememberMe` boolean; keep ordinary token TTL defaulted to 2 hours and remember-me token TTL defaulted to 30 days unless the product decision changes.
- Other backend services should depend on `auth-core` for platform JWT parsing and current-user extraction; they should not depend on auth HTTP DTOs.
- Keep URL authorization aligned with [../../../docs/authorization.md](../../../docs/authorization.md): `/api/auth/admin/**` is admin-only, `/api/auth/player/**` is player/admin, and guest endpoints must not parse JWTs.
- Any API path, response, or auth behavior change must update [../../../docs/api.md](../../../docs/api.md).
- Any logging change must follow [../../../docs/logging.md](../../../docs/logging.md).
- Add focused controller/security tests for new behavior.
