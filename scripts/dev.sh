#!/usr/bin/env bash
# Author: huangbingrui.awa
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
ENV_FILE="${1:-${ROOT_DIR}/deploy/.env}"
TRAINING_DIR="${ROOT_DIR}/frontend"
BLOG_DIR="${ROOT_DIR}/platform-blog/upstream/nblog/blog-view"
training_pid=""
blog_pid=""

usage() {
  cat <<'EOF'
Usage:
  ./scripts/dev.sh [env-file]

Starts MySQL, Redis, and Blog API with Docker, then runs both Vue frontends
with Vite hot module replacement. Press Ctrl-C to stop both Vite servers.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

# shellcheck disable=SC2329 # Invoked indirectly by the signal/exit trap.
cleanup() {
  trap - EXIT INT TERM
  [[ -z "${blog_pid}" ]] || kill "${blog_pid}" 2>/dev/null || true
  [[ -z "${training_pid}" ]] || kill "${training_pid}" 2>/dev/null || true
  [[ -z "${blog_pid}" ]] || wait "${blog_pid}" 2>/dev/null || true
  [[ -z "${training_pid}" ]] || wait "${training_pid}" 2>/dev/null || true
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac

require_command docker
require_command curl
require_command node
require_command npm

if command -v pnpm >/dev/null 2>&1; then
  pnpm_command=(pnpm)
elif command -v corepack >/dev/null 2>&1; then
  pnpm_command=(corepack pnpm)
else
  echo "Missing pnpm: install pnpm 10.33.2 or provide Node corepack." >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing environment file: ${ENV_FILE}" >&2
  echo "Copy deploy/.env.example to deploy/.env and replace every change-me value." >&2
  exit 1
fi

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet

echo "Stopping the production Nginx frontend, if it is running ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" stop frontend >/dev/null

echo "Starting the development backend without forcing an image rebuild ..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d blog-db blog-redis blog-api

published_backend="$(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" port blog-api 8090 | sed -n '1p')"
published_backend="${published_backend##*:}"
if [[ "${published_backend}" != "8090" ]]; then
  echo "Developer mode requires BACKEND_PORT=8090 because both Vite proxies target localhost:8090." >&2
  exit 1
fi

echo "Waiting for Blog API ..."
for _ in {1..30}; do
  if curl -fsS http://localhost:8090/health >/dev/null; then
    break
  fi
  sleep 2
done
if ! curl -fsS http://localhost:8090/health >/dev/null; then
  echo "Blog API did not become healthy." >&2
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 blog-api >&2
  exit 1
fi

echo "Installing frontend dependencies ..."
(cd "${TRAINING_DIR}" && "${pnpm_command[@]}" install --frozen-lockfile)
(cd "${BLOG_DIR}" && npm install)

trap cleanup EXIT INT TERM

echo "Starting Training Vite server on http://localhost:5173 ..."
(cd "${TRAINING_DIR}" && exec "${pnpm_command[@]}" dev) &
training_pid=$!

echo "Starting Blog Vite gateway on http://localhost:4180 ..."
(cd "${BLOG_DIR}" && exec npm run dev) &
blog_pid=$!

sleep 2
if ! kill -0 "${training_pid}" 2>/dev/null || ! kill -0 "${blog_pid}" 2>/dev/null; then
  echo "A Vite server failed to start; check whether ports 4180 and 5173 are available." >&2
  exit 1
fi

echo
echo "Developer mode is ready:"
echo "  Blog:     http://localhost:4180/"
echo "  Training: http://localhost:4180/training/multiple"
echo "  API:      http://localhost:8090/health"
echo "Press Ctrl-C to stop both Vite servers. Docker backend services remain running."

while kill -0 "${training_pid}" 2>/dev/null && kill -0 "${blog_pid}" 2>/dev/null; do
  sleep 1
done

echo "A Vite server exited unexpectedly." >&2
exit 1
