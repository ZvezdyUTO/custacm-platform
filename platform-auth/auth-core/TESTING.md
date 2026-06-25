# auth-core Testing

## How To Run

From the repository root:

```bash
mvn -pl platform-auth/auth-core test
```

For the full project gate:

```bash
mvn clean verify
```

## Test Framework

Tests use JUnit 5 and AssertJ through `spring-boot-starter-test`.

## Covered Scenarios

- `CurrentUserExtractorTest` - extracts `student_identity` and role; rejects missing `student_identity`.
- `KeycloakRolesTest` - reads realm/client roles, chooses `admin` before `student`, rejects tokens without a platform role.
- `KeycloakJwtAuthoritiesConverterTest` - converts the platform role into a Spring `ROLE_*` authority.

## Notes

JWTs in tests are constructed directly with Spring Security's `Jwt` type; no live Keycloak server is required.
