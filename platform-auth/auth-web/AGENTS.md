# auth-web Agent Notes

`auth-web` is the runnable Spring Boot service for the auth slice.

Key files:

- `AuthWebApplication.java` - Spring Boot entrypoint rooted at `com.custacm.platform.auth`.
- `AuthController.java` - exposes `GET /api/auth/me`.
- `AuthModuleController.java` - exposes `GET /health` and `GET /module-info`.
- `AuthSecurityConfig.java` - stateless resource-server security; `/api/**` requires authentication.
- `src/main/resources/application.yml` - port and Keycloak JWT resource-server settings.

Rules:

- Do not issue tokens here; Keycloak owns token issuance.
- Keep public unauthenticated endpoints limited to documented operational endpoints unless explicitly changed.
- Any API path, response, or auth behavior change must update [../../../docs/api.md](../../../docs/api.md).
- Any logging change must follow [../../../docs/logging.md](../../../docs/logging.md).
- Add focused controller/security tests for new behavior.
