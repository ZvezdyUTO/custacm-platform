#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
LOG_DIR="${ROOT_DIR}/logs"

build_frontend_assets() {
  local host_uid host_gid
  host_uid="$(id -u 2>/dev/null || true)"
  host_gid="$(id -g 2>/dev/null || true)"

  echo "Building frontend static assets ..."
  HOST_UID="${host_uid}" HOST_GID="${host_gid}" \
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" run --rm frontend-build
}

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  echo "Copy deploy/.env.example to deploy/.env and update passwords before deploying." >&2
  exit 1
fi

mkdir -p "${LOG_DIR}"
chmod 0777 "${LOG_DIR}"

build_frontend_assets

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build
