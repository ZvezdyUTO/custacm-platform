#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"

case "${1:-}" in
  list|--list|-l)
    echo "blog-api    complete backend monolith"
    exit 0
    ;;
  blog-api|blog|backend)
    ;;
  *)
    echo "Usage: ./scripts/update-module.sh blog-api" >&2
    exit 1
    ;;
esac

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build blog-api

health_url="http://localhost:${BACKEND_PORT:-8090}/health"
for _ in {1..30}; do
  if curl -fsS "${health_url}" >/dev/null; then
    echo "Updated blog-api successfully."
    exit 0
  fi
  sleep 2
done

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 blog-api >&2
exit 1
