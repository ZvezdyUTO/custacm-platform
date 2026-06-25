# custacm-platform Architecture

## Phase

The current phase creates a small, evolvable backend skeleton. It should not lock in the final product model yet.

The first runnable slice is Keycloak-backed platform auth. Other product areas are represented by directories only and should be expanded later, one module at a time.

## Module Map

```text
custacm-platform/
  platform-common/
    common-core/
    common-web/

  platform-auth/
    auth-core/
    auth-interface/
    auth-web/

  platform-training-data/
  platform-blog/
  platform-editor/
  platform-article-storage/

  frontend/
  deploy/
```

## Current Module Responsibilities

### platform-common

`platform-common` is a shared library area, not a service and not a container.

It currently contains only general shared library modules.

Current and expected split:

- `common-core`: base types, base exceptions, error codes, general utilities.
- `common-web`: HTTP response helpers, exception handling, request context helpers.

Do not put business concepts such as `User`, `Article`, `TrainingDataset`, or editor documents in common modules.

### platform-auth

`platform-auth` is the first runnable module and owns the platform auth boundary.

Current implementation:

- keeps Keycloak JWT parsing and current-user extraction in `auth-core`;
- validates Keycloak-issued JWTs;
- extracts immutable student identity and one platform role from JWT claims;
- exposes the current-user endpoint.

Keycloak is the source of truth for login, registration, password reset, sessions, and token issuance. Do not add local password login, demo tokens, or self-issued JWTs unless the identity decision changes explicitly.

The only platform roles are:

```text
admin
student
```

Business APIs expose a single `role` string. If Keycloak ever puts both platform roles in a token, `admin` wins.

The platform student identity is one immutable string:

```text
student_identity = fixed-length student number + real name
example: 112487张三
```

This value is stored as a Keycloak user attribute named `student_identity` and emitted into JWTs as the `student_identity` claim.

Do not split this identity into separate `student_no` and `real_name` fields in the platform model unless the product decision changes explicitly.

`studentIdentity` is the only user ID in platform business code. Other modules should store and reference this value directly when they need to associate data with a student.

Current auth module shape:

```text
platform-auth/
  auth-core/
  auth-interface/
  auth-web/
```

Add `auth-domain`, `auth-app`, or `auth-infra` only when the platform needs business-owned auth or identity data beyond the immutable `student_identity` claim.

### Placeholder Modules

These directories exist to preserve product boundaries:

- `platform-training-data`: future training data API and worker area.
- `platform-blog`: future blog/content module.
- `platform-editor`: future external editor integration.
- `platform-article-storage`: future article storage module.
- `frontend`: future frontend implementation.
- `deploy`: future Docker/K8s deployment configuration.

Do not add them to the Maven reactor until their first runnable slice is being implemented.

## Dependency Direction

Within a business module, prefer this shape:

```text
web -> app -> domain
web -> infra -> domain
app -> interface
web -> interface
```

Rules:

- `domain` must not depend on `app`, `infra`, or `web`.
- `interface` must not depend on `app`, `infra`, or `web`.
- `app` orchestrates use cases and should avoid direct infrastructure details.
- `infra` implements repositories and remote clients.
- `web` owns Spring Boot startup and HTTP controllers.

`platform-auth` is currently a Keycloak adapter module and therefore intentionally has only `auth-core`, `auth-interface`, and `auth-web`.

## Cross-Module Calls

Each module exposes capabilities through its own `*-web` HTTP API.

If one module needs another module, the caller should define and use a local client/adapter. The target base URL must come from configuration, not hard-coded code.

Example future shape:

```text
content-app
  -> AuthClient
  -> content-infra HTTP adapter
  -> auth-web HTTP API
```

A gateway may be added later as a frontend-facing entrypoint. Internal service calls do not need to go through the gateway by default.

## Spring Boot Startup

Each runnable web service needs its own Spring Boot application class.

For package scanning, place the application class at the module package root. Example:

```text
com.custacm.platform.auth.AuthWebApplication
```

This allows Spring to scan:

```text
com.custacm.platform.auth
com.custacm.platform.auth.web
```

## Verification

Current verification commands:

```bash
mvn clean verify
```

`mvn clean verify` runs unit tests and JaCoCo coverage checks. Code-bearing modules should keep line coverage at or above `70%`; placeholder-only modules do not need tests until they contain executable code.

Run the packaging check when build artifacts or Docker image behavior changes:

```bash
mvn clean package -DskipTests
```

Run `auth-web` locally:

```bash
java -jar platform-auth/auth-web/target/auth-web-0.1.0-SNAPSHOT.jar
```

Default port:

```text
8081
```

Basic endpoints:

```text
GET  /health
GET  /module-info
GET  /api/auth/me
```

`GET /api/auth/me` requires a Keycloak bearer token.

Current response shape:

```json
{
  "studentIdentity": "112487张三",
  "role": "student"
}
```

For local deployment, use:

```bash
cp deploy/.env.example deploy/.env
./scripts/deploy.sh
```
