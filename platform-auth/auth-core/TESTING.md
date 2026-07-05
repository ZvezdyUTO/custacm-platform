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

- `CurrentUserExtractorTest` - extracts JWT `sub` and `role`; rejects missing values.
- `PlatformRolesTest` - validates `admin` / `player` token roles and rejects non-token roles such as `guest` and stored `disable`.
- `PlatformJwtAuthoritiesConverterTest` - converts platform roles into Spring authorities, including `admin` inheriting `ROLE_player`.

## Notes

JWTs in tests are constructed directly with Spring Security's `Jwt` type; no live auth server is required.
