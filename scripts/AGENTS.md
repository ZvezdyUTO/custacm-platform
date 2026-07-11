# Scripts Agent Notes

- Keep scripts compatible with their declared shell and never commit real secrets.
- `deploy.sh` delegates to `deploy/deploy.sh`, which builds and starts the four-service `blog-db` + `blog-redis` + `blog-api` + `frontend` Compose stack.
- `update-module.sh` intentionally rebuilds only the unique `blog-api` backend; there is no frontend-specific update script.
- To rebuild the shared Nginx that serves Vue Blog `/` and React Training `/training/**`, use `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build frontend`.
- `auto-update-main.sh` currently classifies root Docker/deployment/script changes as a full deploy and Blog/training/common changes as `blog-api`; it does not classify standalone `frontend/*` changes for deployment. Do not rely on it for frontend-only updates.
- `server-deploy.sh` remains a manual fast-forward wrapper for the full four-service Compose stack; its automated health loop currently checks Blog API only.
- `check-doc-sync.sh` and `check-test-policy.sh` enforce documentation and Java test/report policy.
- Deployment changes require synchronized `deploy/UPDATE.md` and `docs/server-deployment.md` updates.
