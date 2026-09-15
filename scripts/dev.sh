#!/usr/bin/env bash
# Author: huangbingrui.awa
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-${ROOT_DIR}/deploy/.env}"
container_id=""
logs_pid=""

usage() {
  cat <<'EOF'
Usage:
  ./scripts/dev.sh [env-file]

Starts the local Docker backend and one frontend development container.
Both Vue applications use Vite hot module replacement through localhost:4180.
Source is mounted read-only; dependencies live in separate cached volumes.
Set DEV_FRONTEND_PORT in the env file to change the local gateway port.
Press Ctrl-C to stop only the frontend container; the backend stays running.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

# shellcheck disable=SC2329 # Invoked indirectly by the exit trap.
cleanup() {
  trap - EXIT INT TERM
  [[ -z "${logs_pid}" ]] || kill "${logs_pid}" 2>/dev/null || true
  [[ -z "${logs_pid}" ]] || wait "${logs_pid}" 2>/dev/null || true
  if [[ -n "${container_id}" ]] && docker inspect "${container_id}" >/dev/null 2>&1; then
    docker stop "${container_id}" >/dev/null
  fi
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac
if (( $# > 1 )); then
  usage >&2
  exit 1
fi

require_command docker
require_command curl
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing environment file: ${ENV_FILE}" >&2
  echo "Copy deploy/.env.example to deploy/.env and replace every change-me value." >&2
  exit 1
fi

# Developer mode operates on this machine, never an SSH/TCP Docker server.
docker_endpoint="${DOCKER_HOST:-$(docker context inspect --format '{{.Endpoints.docker.Host}}')}"
case "${docker_endpoint}" in
  unix://*|npipe://*) ;;
  *) echo "Developer mode requires a local Docker context." >&2; exit 1 ;;
esac
docker info >/dev/null
compose=(docker compose --ansi never --progress plain --env-file "${ENV_FILE}" -f "${ROOT_DIR}/deploy/docker-compose.yml" -f "${ROOT_DIR}/deploy/docker-compose.dev.yml")
"${compose[@]}" config --quiet

mkdir -p "${ROOT_DIR}/logs" "${ROOT_DIR}/uploads"
echo "Stopping this local stack's existing frontend, if running ..."
"${compose[@]}" stop frontend >/dev/null

echo "Starting the local backend without forcing an image rebuild ..."
"${compose[@]}" up -d --wait --wait-timeout 180 blog-db blog-redis blog-api

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
echo "Starting the cached frontend development image ..."
"${compose[@]}" up -d --build --no-deps --force-recreate frontend
container_id="$("${compose[@]}" ps -aq frontend)"
docker logs --follow --tail=30 "${container_id}" &
logs_pid=$!
for (( attempt=0; attempt<150; attempt++ )); do
  health="$(docker inspect --format '{{.State.Status}} {{.State.Health.Status}}' "${container_id}")"
  [[ "${health}" != 'running healthy' ]] || break
  if [[ "${health}" != 'running starting' ]]; then
    echo "The frontend development container failed its startup check: ${health}" >&2
    exit 1
  fi
  sleep 2
done
if [[ "${health}" != 'running healthy' ]]; then
  echo "Timed out waiting for frontend dependencies and Vite startup." >&2
  exit 1
fi

published_frontend="$("${compose[@]}" port frontend 4180)"
frontend_port="${published_frontend##*:}"
for path in / /training/multiple /training-app/multiple /api/health; do
  curl -fsS "http://localhost:${frontend_port}${path}" >/dev/null
done

echo
echo "Developer mode is ready:"
echo "  Blog:     http://localhost:${frontend_port}/"
echo "  Training: http://localhost:${frontend_port}/training/multiple"
echo "  API:      http://localhost:${frontend_port}/api/health"
echo "Edit either frontend locally; Vite updates the browser without rebuilding images."
echo "Press Ctrl-C to stop the frontend container. Backend services and volumes remain."

while [[ "$(docker inspect --format '{{.State.Running}}' "${container_id}" 2>/dev/null || true)" == true ]]; do
  sleep 2
done

echo "The frontend development container stopped." >&2
exit 1
