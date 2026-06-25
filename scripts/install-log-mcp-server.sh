#!/usr/bin/env bash
set -euo pipefail

TOOLS_DIR="${CUSTACM_TOOLS_DIR:-/opt/custacm-tools}"
INSTALL_DIR="${TOOLS_DIR}/local-logs-mcp-server"
REPO_URL="https://github.com/mariosss/local-logs-mcp-server.git"
COMMIT="63f25778260ec0bcc362be41396073f6e58fc190"

usage() {
  cat <<EOF
Usage:
  ./scripts/install-log-mcp-server.sh

Environment:
  CUSTACM_TOOLS_DIR=/opt/custacm-tools
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
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
require_command node

mkdir -p "${TOOLS_DIR}"

if [[ -d "${INSTALL_DIR}/.git" ]]; then
  echo "Updating ${INSTALL_DIR} ..."
  git -C "${INSTALL_DIR}" fetch --quiet origin
else
  echo "Cloning ${REPO_URL} to ${INSTALL_DIR} ..."
  git clone --quiet "${REPO_URL}" "${INSTALL_DIR}"
fi

git -C "${INSTALL_DIR}" checkout --quiet --detach "${COMMIT}"

cat <<EOF
Installed local-logs-mcp-server at:
  ${INSTALL_DIR}

Pinned commit:
  ${COMMIT}

Example SSH MCP command:
  cd ${INSTALL_DIR} && LOGS_DIR=/opt/custacm-platform/logs LOG_EXTENSIONS=.log,.txt node local-logs-mcp-server.js
EOF
