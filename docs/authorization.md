# HTTP Authorization Rules

This document defines the URL-level authorization convention for every runnable backend service.

## Three Access Tiers

Every HTTP endpoint must belong to exactly one tier:

| Tier | URL convention | JWT handling | Access |
| --- | --- | --- | --- |
| Guest | outside `/admin/**` and `/player/**` | no JWT is required or parsed | anyone |
| Player | `/player/**` under the module API prefix | platform JWT required | `player` or `admin` |
| Admin | `/admin/**` under the module API prefix | platform JWT required | `admin` only |

Operational endpoints such as `/health` and `/module-info` are guest endpoints.

## URL Shape

New module APIs should use this shape:

```text
/api/<module>/admin/**   -> admin-only
/api/<module>/player/**  -> logged-in player/admin
/api/<module>/**         -> guest public API
```

Examples:

```text
POST  /api/auth/login
GET   /api/auth/player/me
PATCH /api/auth/player/me/password
GET   /api/auth/users
POST  /api/auth/admin/users
PATCH /api/auth/admin/users/{studentIdentity}

POST  /api/training-data/admin/ods/codeforces/submissions:batch-upsert
POST  /api/training-data/admin/codeforces/submissions:collect
POST  /api/training-data/admin/codeforces/submissions:collect-batch-jobs
GET   /api/training-data/admin/codeforces/submissions/collect-batch-jobs
GET   /api/training-data/admin/codeforces/submissions/collect-batch-jobs/{jobId}
POST  /api/training-data/admin/codeforces/handles
PATCH /api/training-data/admin/codeforces/handles:change-identity
DELETE /api/training-data/admin/codeforces/users/{studentIdentity}/data
POST  /api/training-data/admin/codeforces/warehouse:refresh
GET   /api/training-data/codeforces/handles?studentIdentity=230511213黄炳睿
GET   /api/training-data/codeforces/accepted-summary?studentIdentity=230511213黄炳睿
GET   /api/training-data/codeforces/accepted-summary/auto-collect-users
GET   /api/training-data/codeforces/submissions/by-student?studentIdentity=230511213黄炳睿
GET   /api/training-data/codeforces/submissions/by-problem?problemKey=2237:G
GET   /api/training-data/codeforces/first-accepted/by-student?studentIdentity=230511213黄炳睿
GET   /api/training-data/codeforces/first-accepted/by-problem?problemKey=2237:G
```

If an endpoint needs the current user identity, it is not a guest endpoint. Put it under `/player/**` or `/admin/**`.

## Guest Endpoints

Guest endpoints are public and must not depend on login state.

Rules:

- Do not declare `@AuthenticationPrincipal Jwt` on a guest endpoint.
- Do not read `SecurityContextHolder` or parse the `Authorization` header in guest handlers.
- A request with an `Authorization` header must still be treated as a guest request.
- If the behavior changes based on the caller identity, move the endpoint to `/player/**` or `/admin/**`.

The shared Spring Security setup implements this with a separate guest filter chain that does not enable OAuth2 Resource Server. As a result, guest endpoints ignore bearer tokens instead of validating or rejecting them.

## Player And Admin Endpoints

Protected endpoints use platform JWTs issued by `auth-web`.

JWT business claims:

```text
sub   = studentIdentity
role  = admin | player
```

Stored `disable` account roles are not emitted in JWTs. Unauthenticated public access has no role value and is represented by the absence of a current user.

The platform authenticated role model is a capability subset:

```text
admin includes player
player includes authenticated player access
```

In Spring Security authorities:

```text
role=admin  -> ROLE_admin, ROLE_player
role=player -> ROLE_player
```

That means `/player/**` endpoints only need `hasRole("player")`; admins pass because the shared converter gives them `ROLE_player`.

## Shared Spring Security Setup

Runnable `*-web` modules should use `auth-core`'s shared helper instead of hand-writing JWT security rules:

```java
@Bean
@Order(PlatformSecurityConfig.PROTECTED_CHAIN_ORDER)
SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
    return PlatformSecurityConfig.statelessJwt(http, jwtDecoder)
            .admin("/api/example/admin/**")
            .player("/api/example/player/**")
            .build();
}

@Bean
@Order(PlatformSecurityConfig.GUEST_CHAIN_ORDER)
SecurityFilterChain guestSecurityFilterChain(HttpSecurity http) throws Exception {
    return PlatformSecurityConfig.guest(http);
}
```

Each service still owns its own `JwtDecoder`, normally built from the platform RSA public key with `PlatformJwtDecoders` and `PemRsaKeys`.

## Business Permission Checks

URL authorization is only the first boundary.

Controller files and URL tiers decide who may call an endpoint. App services should still enforce business rules such as:

- users can only modify their own data;
- admins cannot downgrade, disable, or delete themselves;
- module-specific ownership checks.
