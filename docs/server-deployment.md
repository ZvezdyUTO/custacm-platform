# Server Deployment

This project uses a single-server deployment model for the current phase.

## Server Layout

Use this directory layout on the server:

```text
/opt/custacm-platform/                  application repository
/opt/custacm-platform/deploy/.env       server-only environment file
/opt/custacm-platform/deploy/secrets/   server-only JWT key files
/opt/custacm-platform/logs/             backend log files
/opt/custacm-tools/local-logs-mcp-server/  pinned MCP log reader
```

The application logs stay on the server local disk and are not committed to Git.

## First Deploy

Prepare the repository:

```bash
sudo mkdir -p /opt/custacm-platform
sudo chown -R "$USER":"$USER" /opt/custacm-platform
git clone <repo-url> /opt/custacm-platform
cd /opt/custacm-platform
```

Prepare the tools directory used by the remote log MCP:

```bash
sudo mkdir -p /opt/custacm-tools
sudo chown -R "$USER":"$USER" /opt/custacm-tools
```

Prepare environment variables:

```bash
cp deploy/.env.example deploy/.env
mkdir -p deploy/secrets
openssl genrsa -out deploy/secrets/auth-private-key.pem 2048
openssl rsa -in deploy/secrets/auth-private-key.pem -pubout -out deploy/secrets/auth-public-key.pem
vim deploy/.env
```

At minimum, change:

```env
AUTH_DB_VOLUME_NAME=custacm-platform_auth-db-data
TRAINING_DATA_DB_VOLUME_NAME=custacm-platform_training-data-db-data
AUTH_DB_PASSWORD=change-me-auth-db-password
AUTH_DB_ROOT_PASSWORD=change-me-auth-root-password
TRAINING_DATA_DB_PASSWORD=change-me-training-data-db-password
TRAINING_DATA_DB_ROOT_PASSWORD=change-me-training-data-root-password
AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY=root
AUTH_BOOTSTRAP_ADMIN_PASSWORD=change-me-root-password
```

The two `*_VOLUME_NAME` values are the persistent MySQL Docker volume names. Keep them stable across deploys.
Changing them makes Compose mount a different database volume, which can look like the database was reset.

The auth service also reads `AUTH_JWT_ACCESS_TOKEN_TTL` for ordinary logins and
`AUTH_JWT_REMEMBER_ME_ACCESS_TOKEN_TTL` for remember-me logins. The defaults in
`deploy/.env.example` are `2h` and `30d`.

Deploy:

```bash
./scripts/server-deploy.sh
```

## Daily Deploy

On the server:

```bash
cd /opt/custacm-platform
./scripts/server-deploy.sh
```

The script fetches `origin/main`, fast-forwards the worktree, creates `logs/`, builds the Docker images, starts Compose, and checks `/health`.
It also runs the one-shot `frontend-build` service before `docker compose up`
so the Nginx frontend container can serve the latest `frontend/dist` without a
custom frontend image rebuild.

Daily deploys and container restarts preserve MySQL data because `auth-db` and `training-data-db`
store `/var/lib/mysql` in the named volumes from `deploy/.env`. Do not use
`docker compose down --volumes`, `docker volume rm`, or `docker volume prune` unless you explicitly intend to wipe local data.

For local testing without a remote:

```bash
SERVER_DEPLOY_SKIP_GIT_PULL=1 ./scripts/server-deploy.sh
```

## Install Remote Log MCP

Install the pinned MCP log reader:

```bash
cd /opt/custacm-platform
./scripts/install-log-mcp-server.sh
```

The script installs `mariosss/local-logs-mcp-server` at commit:

```text
63f25778260ec0bcc362be41396073f6e58fc190
```

The server needs Node.js 18+ for the MCP process.

## Local AI Client Configuration

Configure the local AI client to start the MCP process over SSH:

```json
{
  "mcpServers": {
    "custacm-prod-logs": {
      "command": "ssh",
      "args": [
        "custacm-server",
        "cd /opt/custacm-tools/local-logs-mcp-server && LOGS_DIR=/opt/custacm-platform/logs LOG_EXTENSIONS=.log,.txt node local-logs-mcp-server.js"
      ]
    }
  }
}
```

The SSH host alias `custacm-server` should be configured in the local `~/.ssh/config`.

Useful agent prompts:

```text
List available server log files.
Show the last 100 lines from combined.log.
Search combined.log for traceId=xxx.
Search combined.log for errorCode=AUTH_TOKEN_INVALID.
Show recent entries from error.log.
```

## Verification

After deployment:

```bash
curl -fsS http://localhost:3000/
curl -fsS http://localhost:8081/health
curl -fsS http://localhost:8082/health
test -f logs/combined.log
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

The front-end container serves the built React workbench and proxies same-origin
`/api/auth/**`, `/api/training-data/**`, health, and module-info requests to the
backend services.
For frontend-only changes after a deploy, use:

```bash
./scripts/update-module.sh frontend
```

That command rebuilds `frontend/dist`, keeps the fixed Nginx container, and
reloads Nginx instead of rebuilding backend images.

To create sample users and start a Codeforces collection job through real HTTP
APIs after the first deploy:

```bash
./scripts/seed-local-codeforces-data.sh
```

The log MCP should be able to list `combined.log` and `error.log` after the backend services have written log entries.
