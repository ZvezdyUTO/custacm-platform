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
  auth-web             platform-auth/auth-web -> custacm-backend
  training-data-web    platform-training-data/training-data-web -> custacm-training-data-web
  frontend             frontend static build -> custacm-frontend

Run `git pull origin main` before this script when updating from GitHub.
Backend Docker images build Maven artifacts inside Docker. Frontend updates use
the frontend-build service to refresh frontend/dist, then reload Nginx.
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
    MODULE_KIND="backend"
    HEALTH_PORT_VAR="BACKEND_PORT"
    HEALTH_DEFAULT="8081"
    HEALTH_PATH="/health"
    ;;
  training-data-web|training-data|custacm-training-data-web)
    MODULE_PATH="platform-training-data/training-data-web"
    SERVICE_NAME="custacm-training-data-web"
    MODULE_KIND="backend"
    HEALTH_PORT_VAR="TRAINING_DATA_PORT"
    HEALTH_DEFAULT="8082"
    HEALTH_PATH="/health"
    ;;
  frontend|front|custacm-frontend)
    MODULE_PATH="frontend"
    SERVICE_NAME="custacm-frontend"
    MODULE_KIND="frontend"
    HEALTH_PORT_VAR="FRONTEND_PORT"
    HEALTH_DEFAULT="3000"
    HEALTH_PATH="/"
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

HEALTH_PORT="${!HEALTH_PORT_VAR:-${HEALTH_DEFAULT}}"
HEALTH_URL="http://localhost:${HEALTH_PORT}${HEALTH_PATH}"

mkdir -p "${LOG_DIR}"
chmod 0777 "${LOG_DIR}"

if [[ "${MODULE_KIND}" == "frontend" ]]; then
  host_uid="$(id -u 2>/dev/null || true)"
  host_gid="$(id -g 2>/dev/null || true)"

  echo "Building frontend static assets ..."
  HOST_UID="${host_uid}" HOST_GID="${host_gid}" \
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" run --rm frontend-build

  echo "Ensuring Docker service ${SERVICE_NAME} is running ..."
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --no-deps "${SERVICE_NAME}"

  echo "Reloading Nginx in ${SERVICE_NAME} ..."
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T "${SERVICE_NAME}" nginx -s reload
else
  echo "Building Docker service ${SERVICE_NAME} ..."
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" build "${SERVICE_NAME}"

  echo "Restarting Docker service ${SERVICE_NAME} ..."
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --no-deps "${SERVICE_NAME}"
fi

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
