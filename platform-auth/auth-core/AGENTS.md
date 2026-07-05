# auth-core Agent Notes

`auth-core` contains platform JWT parsing, current-user extraction, and shared URL authorization helpers.

Key files:

- `CurrentUser.java` - immutable platform current-user record.
- `CurrentUserExtractor.java` - reads JWT subject `sub` and platform `role` from a `Jwt`.
- `PlatformRoles.java` - validates token roles.
- `PlatformJwtAuthoritiesConverter.java` - converts the platform role to Spring `ROLE_*` authorities; `admin` also receives `ROLE_player`.
- `PlatformJwtAuthenticationConverters.java` - builds the Spring JWT authentication converter and sets principal claim `sub`.
- `PemRsaKeys.java` and `PlatformJwtDecoders.java` - load RSA public keys and build decoders for platform JWTs.
- `PlatformSecurityConfig.java` - builds shared stateless Spring Security filter chains for protected admin/player paths and public guest paths.

Rules:

- Never log tokens, Authorization headers, or JWT signing keys.
- Keep accepted platform roles aligned with [../../../docs/api.md](../../../docs/api.md).
- Keep URL authorization behavior aligned with [../../../docs/authorization.md](../../../docs/authorization.md).
- Add or update tests in `src/test` for parsing, role validation, and rejection behavior.
- If JWT claim names, role semantics, or shared URL authorization behavior change, update [../../../docs/authorization.md](../../../docs/authorization.md), [../../../docs/architecture.md](../../../docs/architecture.md), [../../../docs/api.md](../../../docs/api.md), and [TESTING.md](TESTING.md).
