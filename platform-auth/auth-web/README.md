# auth-web

`auth-web` is the runnable Spring Boot service for the platform auth slice.

It owns public login, player self-service endpoints, admin user-management endpoints, datasource/runtime configuration, bootstrap admin wiring, and RSA JWT issuing/verification for the auth service itself. Public login accepts an optional `rememberMe` boolean: ordinary tokens default to 2 hours, while remembered sessions default to 30 days.

## Directory Layout

```text
auth-web/
  src/main/java/com/custacm/platform/auth/
  src/main/java/com/custacm/platform/auth/web/
  src/main/resources/
  src/test/java/com/custacm/platform/auth/web/
  AGENTS.md
  TESTING.md
```

## Dependency And Layer Rules

- May depend on `auth-domain`, `auth-app`, `auth-core`, and `auth-infra`.
- Controllers stay thin: validate HTTP request shape, extract current JWT user when needed, and call app services.
- Public endpoints must not parse JWTs. The user-list read endpoint is `/api/auth/users`; player/admin mutation endpoints stay under `/api/auth/player/**` or `/api/auth/admin/**`.
- Password hashing, persistence, token issuing, and bootstrap admin wiring are configured here but implemented in lower modules.
- Auth HTTP request/response records live in this module because no other backend module consumes them directly.

## File Responsibilities

- `AuthWebApplication.java` - Spring Boot entrypoint.
- `AuthController.java` - public login plus `/api/auth/player/**` current-user and own-password endpoints.
- `AdminUserController.java` - user-management endpoints; `GET /api/auth/users` is public read, while mutation endpoints remain admin-only and use operation-result responses.
- `AuthModuleController.java` - `/health` and `/module-info`.
- `AuthSecurityConfig.java` - configures the admin/player protected chain plus the guest public chain.
- `AuthApplicationConfig.java` - wires repository, password hasher, JWT issuer/decoder, services, and bootstrap admin.
- `AuthProperties.java` - typed runtime properties for JWT keys, ordinary/remember-me token TTLs, and bootstrap admin.
- `AuthExceptionHandler.java` - maps auth service exceptions to documented error responses.
- `UserResponseMapper.java` - maps account domain objects and admin operation results to HTTP DTOs.
- `*Request.java`, `*Response.java` - HTTP-local DTO records for auth endpoints.
- `application.yml` - service port, datasource, Flyway, JWT, and bootstrap admin defaults.
