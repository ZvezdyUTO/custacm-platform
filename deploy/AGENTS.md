# Deploy Agent Notes

This directory owns local/server deployment configuration.

- `deploy/.env` is local-only and must not be committed.
- `deploy/.env.example` is the public template; keep secrets as `change-me-*` placeholders.
- `docker-compose.yml` currently starts `keycloak-db`, `keycloak`, and `custacm-backend`.
- `custacm-backend` currently runs `platform-auth/auth-web`.
- Keycloak realm import is in `keycloak/custacm-realm.json`.

When deployment behavior changes, update:

- [README.md](README.md)
- [UPDATE.md](UPDATE.md)
- [../docs/server-deployment.md](../docs/server-deployment.md)
- [../docs/agent/context-map.md](../docs/agent/context-map.md) if services or directories change

Run:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```
