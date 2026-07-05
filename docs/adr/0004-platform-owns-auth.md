# ADR 0004: Platform Owns Auth

## Status

Accepted.

## Decision

`platform-auth/auth-web` owns login, password hashing, user management, and token issuance.

The platform stores local accounts in MySQL. Passwords are hashed with BCrypt. `auth-web` signs access tokens with an RSA private key, and other backend services validate those tokens with the matching public key.

JWT business claims are intentionally small:

- `sub`: the immutable `studentIdentity` business user ID.
- `role`: one stored account role string, currently `admin`, `player`, or `disable`.

`disable` is stored as an account role and cannot authenticate. Unauthenticated visitors have no stored account role and no JWT role value. `disable` is not emitted in JWTs.

There is no public registration flow. Admins create users directly or through batch creation. Admins may manage other accounts, but cannot downgrade, disable, or delete themselves. Players can change only their own passwords.

HTTP authorization is URL-tiered across runnable services:

- `/admin/**` endpoints require `admin`.
- `/player/**` endpoints require `player` or `admin`.
- Other endpoints are guest endpoints; they do not require or parse JWTs.

`admin` includes `player` capability in Spring Security authorities.

## Consequences

- Keycloak, Keycloak realm import, and Keycloak-specific JWT claim parsing are removed from the platform runtime.
- `studentIdentity` remains the only business user ID and must not be split into student-number/name fields without a new decision.
- Other runnable backend services should use `auth-core` platform JWT helpers and the platform RSA public key for token validation.
- Runnable backend services should use `auth-core` shared URL authorization helpers so guest, player, and admin tiers behave consistently.
- Auth code that changes JWT claim names, roles, or login/account semantics must update `platform-auth/*/AGENTS.md`, `docs/architecture.md`, `docs/api.md`, and relevant tests.
