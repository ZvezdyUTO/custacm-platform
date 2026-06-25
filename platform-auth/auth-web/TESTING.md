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

- `AuthControllerTest` - `GET /api/auth/me` controller logic returns `studentIdentity` and `role` from a constructed JWT.
- `AuthModuleControllerTest` - health and module-info controller metadata.

## Notes

Current tests instantiate controllers directly; they do not start a full Spring context or a live Keycloak server.
