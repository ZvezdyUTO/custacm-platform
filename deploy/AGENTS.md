# Deploy Agent Notes

This directory owns local/server deployment configuration.

- `deploy/.env` is local-only and must not be committed.
- `deploy/.env.example` is the public template; keep secrets as `change-me-*` placeholders.
- `docker-compose.yml` currently starts `auth-db`, `custacm-backend`, `training-data-db`, `custacm-training-data-web`, and `custacm-frontend`; it also defines the one-shot `frontend-build` service for generating `frontend/dist`.
- MySQL data must persist through service/container recreation. Keep auth/training-data database volumes explicitly named through `AUTH_DB_VOLUME_NAME` and `TRAINING_DATA_DB_VOLUME_NAME`, and do not introduce deployment scripts that run `docker compose down --volumes` unless the user explicitly asks to wipe data.
- `custacm-backend` runs `platform-auth/auth-web`.
- `custacm-training-data-web` runs `platform-training-data/training-data-web`.
- `custacm-frontend` is a fixed Nginx container that serves mounted `frontend/dist` and proxies same-origin API requests to backend services.
- JWT private/public key PEM files are local secrets and must not be committed.

When deployment behavior changes, update:

- [README.md](README.md)
- [UPDATE.md](UPDATE.md)
- [../docs/server-deployment.md](../docs/server-deployment.md)
- [../docs/agent/context-map.md](../docs/agent/context-map.md) if services or directories change

Run:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```
