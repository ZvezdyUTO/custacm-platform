#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
ENV_FILE="${1:-${ROOT_DIR}/deploy/.env}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/deploy.sh [env-file]

Builds, starts, and verifies the complete four-service stack:
  blog-db, blog-redis, blog-api, frontend
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

check_url() {
  local url="$1"
  local name="$2"

  echo "Checking ${name}: ${url}"
  for _ in {1..30}; do
    if curl -fkLsS "${url}" >/dev/null; then
      return 0
    fi
    sleep 2
  done

  echo "Health check failed for ${name}." >&2
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps >&2
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 blog-api frontend >&2
  exit 1
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac

require_command docker
require_command curl

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing environment file: ${ENV_FILE}" >&2
  echo "Copy deploy/.env.example to deploy/.env and replace every change-me value." >&2
  exit 1
fi

mkdir -p "${ROOT_DIR}/logs" "${ROOT_DIR}/uploads"

echo "Validating Docker Compose configuration ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet

echo "Building and starting the complete Docker Compose stack ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build --remove-orphans

backend_port="$(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" port blog-api 8090 | sed -n '1p')"
frontend_port="$(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" port frontend 80 | sed -n '1p')"
backend_port="${backend_port##*:}"
frontend_port="${frontend_port##*:}"

if [[ -z "${backend_port}" || -z "${frontend_port}" ]]; then
  echo "Cannot resolve published backend or frontend port from Docker Compose." >&2
  exit 1
fi

tls_enabled="$(awk -F= '$1 == "TLS_ENABLED" {print tolower($2); exit}' "${ENV_FILE}")"
frontend_scheme="http"
if [[ "${tls_enabled}" == "true" ]]; then
  frontend_scheme="https"
  frontend_port="$(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" port frontend 443 | sed -n '1p')"
  frontend_port="${frontend_port##*:}"
fi

if [[ -z "${backend_port}" || -z "${frontend_port}" ]]; then
  echo "Cannot resolve published backend or frontend port from Docker Compose." >&2
  exit 1
fi

check_url "http://localhost:${backend_port}/health" "blog-api"
check_url "${frontend_scheme}://localhost:${frontend_port}/" "Vue Blog"
check_url "${frontend_scheme}://localhost:${frontend_port}/training/multiple" "Vue Training"
check_url "${frontend_scheme}://localhost:${frontend_port}/api/health" "gateway API"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps
echo "Complete custacm-platform stack is ready."
