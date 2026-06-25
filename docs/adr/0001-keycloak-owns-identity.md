# ADR 0001: Keycloak Owns Identity

## Status

Accepted.

## Decision

Keycloak is the only login and token issuer. Platform backend code validates Keycloak JWTs and uses the `student_identity` claim as the single business user ID exposed as `studentIdentity`.

## Consequences

- Do not add local password login, demo tokens, or self-issued JWTs.
- Do not split `student_identity` into student-number/name fields unless the product decision changes explicitly.
- Platform business responses use one `role` string, currently `admin` or `student`.
- Auth code that changes JWT parsing must update `platform-auth/auth-core/AGENTS.md`, `platform-auth/auth-core/TESTING.md`, and the relevant API/architecture docs.
