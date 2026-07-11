#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REMOTE="${AUTO_UPDATE_REMOTE:-origin}"
BRANCH="${AUTO_UPDATE_BRANCH:-main}"
INTERVAL_SECONDS="${AUTO_UPDATE_INTERVAL_SECONDS:-60}"
STATE_FILE="${AUTO_UPDATE_STATE_FILE:-${ROOT_DIR}/deploy/.auto-update-main.state}"
LOCK_DIR="${AUTO_UPDATE_LOCK_DIR:-${ROOT_DIR}/deploy/.auto-update-main.lock}"

usage() {
  cat <<EOF
Usage:
  ./scripts/auto-update-main.sh once
  ./scripts/auto-update-main.sh watch
  ./scripts/auto-update-main.sh status
  ./scripts/auto-update-main.sh classify <changed-file>...

Environment:
  AUTO_UPDATE_REMOTE=origin
  AUTO_UPDATE_BRANCH=main
  AUTO_UPDATE_INTERVAL_SECONDS=60
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

with_lock() {
  if ! mkdir "${LOCK_DIR}" 2>/dev/null; then
    echo "Another auto-update run is active: ${LOCK_DIR}" >&2
    return 1
  fi
  trap 'rmdir "${LOCK_DIR}" 2>/dev/null || true' RETURN
  "$@"
}

require_clean_worktree() {
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Working tree has tracked local changes. Stop auto update to avoid overwriting work." >&2
    git status --short >&2
    return 1
  fi
}

require_remote() {
  if ! git remote get-url "${REMOTE}" >/dev/null 2>&1; then
    echo "Missing git remote '${REMOTE}'. Configure GitHub first, then run this script." >&2
    return 1
  fi
}

classify_changes() {
  local file
  local needs_full=0
  local needs_blog=0
  local has_runtime_change=0

  for file in "$@"; do
    case "${file}" in
      Dockerfile|pom.xml|deploy/docker-compose.yml|deploy/.env.example|scripts/deploy.sh|scripts/server-deploy.sh|scripts/update-module.sh|scripts/auto-update-main.sh)
        needs_full=1
        has_runtime_change=1
        ;;
      platform-blog/*|platform-training-data/*|platform-common/*)
        needs_blog=1
        has_runtime_change=1
        ;;
      *)
        ;;
    esac
  done

  if [[ "${needs_full}" == "1" ]]; then
    echo "full"
  elif [[ "${has_runtime_change}" == "0" ]]; then
    echo "none"
  else
    local modules=()
    if [[ "${needs_blog}" == "1" ]]; then
      modules+=("blog-api")
    fi
    local IFS=,
    echo "modules:${modules[*]}"
  fi
}

deploy_by_classification() {
  local classification="$1"
  case "${classification}" in
    full)
      log "Runtime changes require full deploy."
      "${ROOT_DIR}/scripts/deploy.sh"
      ;;
    modules:*)
      local modules_csv="${classification#modules:}"
      local module
      IFS=',' read -ra modules <<< "${modules_csv}"
      for module in "${modules[@]}"; do
        log "Updating module ${module}."
        "${ROOT_DIR}/scripts/update-module.sh" "${module}"
      done
      ;;
    none)
      log "No runtime container changes detected. Nothing to deploy."
      ;;
    *)
      echo "Unknown classification: ${classification}" >&2
      return 1
      ;;
  esac
}

run_once() {
  cd "${ROOT_DIR}"

  if ! git rev-parse --verify HEAD >/dev/null 2>&1; then
    echo "Repository has no commits yet. Auto update needs a committed main branch." >&2
    return 1
  fi

  require_remote
  require_clean_worktree

  log "Fetching ${REMOTE}/${BRANCH}."
  git fetch --quiet "${REMOTE}" "${BRANCH}"

  local head_sha remote_sha deployed_sha base_sha classification
  head_sha="$(git rev-parse HEAD)"
  remote_sha="$(git rev-parse FETCH_HEAD)"

  if [[ -f "${STATE_FILE}" ]]; then
    deployed_sha="$(cat "${STATE_FILE}")"
  else
    deployed_sha="${head_sha}"
  fi

  if [[ "${remote_sha}" == "${deployed_sha}" ]]; then
    mkdir -p "$(dirname "${STATE_FILE}")"
    printf '%s\n' "${remote_sha}" > "${STATE_FILE}"
    log "Already deployed ${remote_sha}."
    return 0
  fi

  if ! git merge-base --is-ancestor "${head_sha}" "${remote_sha}"; then
    echo "${REMOTE}/${BRANCH} is not a fast-forward update from current HEAD." >&2
    echo "Manual intervention required." >&2
    return 1
  fi

  if git cat-file -e "${deployed_sha}^{commit}" 2>/dev/null; then
    base_sha="${deployed_sha}"
  else
    base_sha="${head_sha}"
  fi

  changed_files=()
  while IFS= read -r changed_file; do
    changed_files+=("${changed_file}")
  done < <(git diff --name-only "${base_sha}" "${remote_sha}")
  classification="$(classify_changes "${changed_files[@]}")"

  log "Fast-forwarding to ${remote_sha}."
  git merge --ff-only --quiet "${remote_sha}"

  if [[ "${classification}" == "none" ]]; then
    log "Changed files do not map to a runtime container."
  else
    log "Changed files:"
    printf '  %s\n' "${changed_files[@]}"
  fi

  deploy_by_classification "${classification}"
  mkdir -p "$(dirname "${STATE_FILE}")"
  printf '%s\n' "${remote_sha}" > "${STATE_FILE}"
  log "Recorded deployed revision ${remote_sha}."
}

status() {
  cd "${ROOT_DIR}"
  printf 'branch: %s\n' "$(git branch --show-current 2>/dev/null || true)"
  printf 'head: %s\n' "$(git rev-parse --short HEAD 2>/dev/null || true)"
  if git remote get-url "${REMOTE}" >/dev/null 2>&1; then
    printf 'remote: %s %s\n' "${REMOTE}" "$(git remote get-url "${REMOTE}")"
  else
    printf 'remote: %s missing\n' "${REMOTE}"
  fi
  if [[ -f "${STATE_FILE}" ]]; then
    printf 'deployed: %s\n' "$(cat "${STATE_FILE}")"
  else
    printf 'deployed: not recorded\n'
  fi
}

MODE="${1:-once}"
case "${MODE}" in
  once)
    with_lock run_once
    ;;
  watch)
    while true; do
      if ! with_lock run_once; then
        log "Auto update failed; will retry in ${INTERVAL_SECONDS}s."
      fi
      sleep "${INTERVAL_SECONDS}"
    done
    ;;
  status)
    status
    ;;
  classify)
    shift
    classify_changes "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
