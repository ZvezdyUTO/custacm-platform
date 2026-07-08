# auth-web Testing

## How To Run

From the repository root:

```bash
mvn -pl platform-auth/auth-web test
```

For the full project gate:

```bash
mvn clean verify
```

## Test Framework

Tests use JUnit 5 and AssertJ through `spring-boot-starter-test`.

## Covered Scenarios

- `AuthWebIntegrationTest` - H2 + Flyway + RSA JWT integration coverage for login, ordinary versus remember-me token lifetime selection, `/api/auth/player/me`, guest endpoints ignoring bearer tokens, protected player routes, admin user operation responses, batch partial success/failure, generated passwords, deletion, unified role/password updates including `disable`, player password change including confirmation mismatch, player admin rejection, and admin self-downgrade protection.
- `AuthModuleControllerTest` - health and module-info controller metadata.

## Notes

The integration test starts a Spring context with H2, generated RSA keys, and a bootstrap admin. It does not require a live MySQL server.
