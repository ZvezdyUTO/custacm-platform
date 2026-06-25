#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
LOG_DIR="${ROOT_DIR}/logs"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/update-module.sh <module>
  ./scripts/update-module.sh list

Current modules:
  auth-web    platform-auth/auth-web -> custacm-backend

Run `git pull origin main` before this script when updating from GitHub.
The Dockerfile builds Maven artifacts inside Docker, so the host only needs Docker.
EOF
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  echo "Copy deploy/.env.example to deploy/.env and update passwords before deploying." >&2
  exit 1
fi

MODULE_NAME="$1"

case "${MODULE_NAME}" in
  list|--list|-l)
    usage
    exit 0
    ;;
  auth-web|auth|custacm-backend)
    MODULE_PATH="platform-auth/auth-web"
    SERVICE_NAME="custacm-backend"
    HEALTH_PORT_VAR="BACKEND_PORT"
    HEALTH_PATH="/health"
    ;;
  *)
    echo "Unknown module: ${MODULE_NAME}" >&2
    usage >&2
    exit 1
    ;;
esac

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

HEALTH_PORT="${!HEALTH_PORT_VAR:-8081}"
HEALTH_URL="http://localhost:${HEALTH_PORT}${HEALTH_PATH}"

mkdir -p "${LOG_DIR}"
chmod 0777 "${LOG_DIR}"

echo "Building Docker service ${SERVICE_NAME} ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" build "${SERVICE_NAME}"

echo "Restarting Docker service ${SERVICE_NAME} ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --no-deps "${SERVICE_NAME}"

echo "Checking ${HEALTH_URL} ..."
for _ in {1..30}; do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    echo "Updated ${MODULE_NAME} (${SERVICE_NAME}) successfully."
    exit 0
  fi
  sleep 2
done

echo "Health check failed for ${SERVICE_NAME}. Recent logs:" >&2
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 "${SERVICE_NAME}" >&2
exit 1
