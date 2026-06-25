# auth-core Agent Notes

`auth-core` contains Keycloak JWT parsing and current-user extraction helpers.

Key files:

- `CurrentUser.java` - immutable platform current-user record.
- `CurrentUserExtractor.java` - reads `student_identity` and platform role from a `Jwt`.
- `KeycloakRoles.java` - reads realm/client roles and chooses `admin` before `student`.
- `KeycloakJwtAuthoritiesConverter.java` - converts the platform role to a Spring `ROLE_*` authority.
- `KeycloakJwtAuthenticationConverters.java` - builds the Spring JWT authentication converter and sets principal claim `student_identity`.

Rules:

- Never log plaintext `student_identity`, tokens, or Authorization headers.
- Keep accepted platform roles aligned with [../../../docs/api.md](../../../docs/api.md).
- Add or update tests in `src/test` for parsing, role precedence, and rejection behavior.
- If JWT claim names or role semantics change, update [../../../docs/architecture.md](../../../docs/architecture.md), [../../../docs/api.md](../../../docs/api.md), and [TESTING.md](TESTING.md).
