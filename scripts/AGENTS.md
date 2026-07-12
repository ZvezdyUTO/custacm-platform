# Scripts Agent Notes

- Keep scripts compatible with their declared shell and never commit real secrets.
- There are exactly two startup entrypoints: `dev.sh` keeps the Docker database/Redis/API running without forcing backend rebuilds and runs both Vue frontends through Vite/HMR; `deploy.sh` builds and starts the complete stable four-service Compose stack and checks all public paths.
- `dev.sh` stops the production Nginx frontend while it owns ports 4180/5173 in the foreground; Ctrl-C stops both Vite processes but leaves backend containers running. `deploy.sh` switches back to the Nginx production frontend.
- Do not add module-only deployment scripts, a wrapper under `deploy/`, a third startup mode, or scripts that pull Git implicitly.
- `check-doc-sync.sh` and `check-test-policy.sh` enforce documentation and Java test/report policy.
- Deployment changes require synchronized `deploy/README.md`, `deploy/UPDATE.md` and `docs/server-deployment.md` updates.
