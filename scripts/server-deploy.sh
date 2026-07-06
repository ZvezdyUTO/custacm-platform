#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose.yml"
LOG_DIR="${ROOT_DIR}/logs"
REMOTE="${SERVER_DEPLOY_REMOTE:-origin}"
BRANCH="${SERVER_DEPLOY_BRANCH:-main}"
SKIP_GIT_PULL="${SERVER_DEPLOY_SKIP_GIT_PULL:-0}"

usage() {
  cat <<EOF
Usage:
  ./scripts/server-deploy.sh

Environment:
  SERVER_DEPLOY_REMOTE=origin
  SERVER_DEPLOY_BRANCH=main
  SERVER_DEPLOY_SKIP_GIT_PULL=1   Skip git fetch/fast-forward for local testing.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

prepare_logs_dir() {
  mkdir -p "${LOG_DIR}"
  chmod 0777 "${LOG_DIR}"
}

build_frontend_assets() {
  local host_uid host_gid
  host_uid="$(id -u 2>/dev/null || true)"
  host_gid="$(id -g 2>/dev/null || true)"

  echo "Building frontend static assets ..."
  HOST_UID="${host_uid}" HOST_GID="${host_gid}" \
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" run --rm frontend-build
}

pull_latest() {
  if [[ "${SKIP_GIT_PULL}" == "1" ]]; then
    echo "Skipping git pull because SERVER_DEPLOY_SKIP_GIT_PULL=1."
    return
  fi

  cd "${ROOT_DIR}"

  if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "${ROOT_DIR} is not a git worktree." >&2
    exit 1
  fi

  if ! git remote get-url "${REMOTE}" >/dev/null 2>&1; then
    echo "Missing git remote '${REMOTE}'. Set SERVER_DEPLOY_REMOTE or use SERVER_DEPLOY_SKIP_GIT_PULL=1." >&2
    exit 1
  fi

  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Working tree has tracked local changes. Commit/stash them before server deploy." >&2
    git status --short >&2
    exit 1
  fi

  echo "Fetching ${REMOTE}/${BRANCH} ..."
  git fetch --quiet "${REMOTE}" "${BRANCH}"

  if ! git merge-base --is-ancestor HEAD FETCH_HEAD; then
    echo "${REMOTE}/${BRANCH} is not a fast-forward update from current HEAD." >&2
    exit 1
  fi

  echo "Fast-forwarding to ${REMOTE}/${BRANCH} ..."
  git merge --ff-only --quiet FETCH_HEAD
}

health_check() {
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a

  local auth_health_url="http://localhost:${BACKEND_PORT:-8081}/health"
  local training_health_url="http://localhost:${TRAINING_DATA_PORT:-8082}/health"
  local frontend_url="http://localhost:${FRONTEND_PORT:-3000}/"

  check_url "${auth_health_url}" "custacm-backend"
  check_url "${training_health_url}" "custacm-training-data-web"
  check_url "${frontend_url}" "custacm-frontend"

  echo "Server deploy succeeded."
}

check_url() {
  local url="$1"
  local service="$2"

  echo "Checking ${url} ..."
  for _ in {1..30}; do
    if curl -fsS "${url}" >/dev/null; then
      return
    fi
    sleep 2
  done

  echo "Health check failed for ${service}. Recent logs:" >&2
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 "${service}" >&2
  exit 1
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
  "")
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac

require_command git
require_command docker
require_command curl

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  echo "Copy deploy/.env.example to deploy/.env and update passwords before deploying." >&2
  exit 1
fi

pull_latest
prepare_logs_dir

echo "Building and starting Docker Compose services ..."
build_frontend_assets
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build

health_check
