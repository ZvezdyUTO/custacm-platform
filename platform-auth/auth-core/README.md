# auth-core

`auth-core` is the shared authentication library for runnable backend services.

It does not own accounts, passwords, persistence, or token issuance. It owns the common runtime rules for consuming platform JWTs and applying platform URL authorization tiers.

## Directory Layout

```text
auth-core/
  src/main/java/com/custacm/platform/auth/core/
  src/test/java/com/custacm/platform/auth/core/
  AGENTS.md
  TESTING.md
```

## Dependency And Layer Rules

- May depend on Spring Security JWT/resource-server APIs.
- Must not depend on `auth-app`, `auth-domain`, `auth-infra`, or `auth-web`.
- Must not contain account persistence, password hashing, login, or admin user-management use cases.
- Shared URL authorization behavior must stay aligned with `docs/authorization.md`.

## File Responsibilities

- `CurrentUser.java` - immutable current-user value with `studentIdentity` and role.
- `CurrentUserExtractor.java` - extracts `sub` and `role` from a decoded Spring `Jwt`.
- `PlatformRoles.java` - validates token roles; accepts `admin` and `player`, rejects non-token roles such as `guest` and stored `disable`.
- `PlatformJwtAuthoritiesConverter.java` - converts platform roles to Spring authorities; `admin` also receives `ROLE_player`.
- `PlatformJwtAuthenticationConverters.java` - builds the JWT authentication converter and uses `sub` as the principal name.
- `PlatformSecurityConfig.java` - builds shared stateless security filter chains for admin/player protected paths and guest public paths.
- `PemRsaKeys.java` - loads RSA public/private keys from inline PEM or file paths.
- `PlatformJwtDecoders.java` - builds RSA public-key JWT decoders.
