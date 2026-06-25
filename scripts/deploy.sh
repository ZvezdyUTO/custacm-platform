#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
LOG_DIR="${ROOT_DIR}/logs"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  echo "Copy deploy/.env.example to deploy/.env and update passwords before deploying." >&2
  exit 1
fi

mkdir -p "${LOG_DIR}"
chmod 0777 "${LOG_DIR}"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build
