# 0005 Blog API owns integrated identity

Status: Accepted; supersedes ADR 0004 for the current runtime.

## Decision

NBlog Blog API is the only Spring Boot backend and owns BCrypt credentials, user management, OJ handles, and HS512 JWT issuance. `username` is the JWT `sub` and training business identity. Roles are exactly `ROLE_admin` and `ROLE_player`; guest is unauthenticated.

Training-data modules are in-process libraries for collection, ODS/DWD/DWM/DWS processing, query, scheduling, and purge. They depend on `TrainingUserDirectory` and do not own auth or a web runtime.

Protected requests reload the current user/role from MySQL after token verification. Username changes and deletion therefore invalidate old tokens on their next use.

## Consequences

- The former `platform-auth` and `training-data-web` modules are removed.
- Training queries require player/admin and live below `/player/training-data/**`.
- Account, handle, and training administration live below `/admin/**`.
- The unified database uses username/handle cascade foreign keys while user deletion retains articles/comments with nullable authors.
