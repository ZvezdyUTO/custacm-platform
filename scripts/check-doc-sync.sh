#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAP_FILE="${ROOT_DIR}/docs/doc-sync-map.tsv"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/check-doc-sync.sh [base-ref] [head-ref]

Examples:
  ./scripts/check-doc-sync.sh origin/main HEAD
  ./scripts/check-doc-sync.sh origin/main WORKTREE
  ./scripts/check-doc-sync.sh HEAD~1 HEAD

Without arguments, the script uses GitHub PR refs when available, otherwise
origin/main..WORKTREE for local runs.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "help" ]]; then
  usage
  exit 0
fi

cd "${ROOT_DIR}"

if [[ ! -f "${MAP_FILE}" ]]; then
  echo "Missing ${MAP_FILE}." >&2
  exit 1
fi

BASE_REF="${1:-}"
HEAD_REF="${2:-}"

if [[ -z "${BASE_REF}" ]]; then
  if [[ -n "${GITHUB_BASE_REF:-}" ]]; then
    BASE_REF="origin/${GITHUB_BASE_REF}"
    HEAD_REF="${HEAD_REF:-HEAD}"
  elif [[ "${GITHUB_EVENT_NAME:-}" == "push" ]] && git rev-parse --verify HEAD^ >/dev/null 2>&1; then
    BASE_REF="HEAD^"
    HEAD_REF="${HEAD_REF:-HEAD}"
  else
    BASE_REF="origin/main"
    HEAD_REF="${HEAD_REF:-WORKTREE}"
  fi
fi

HEAD_REF="${HEAD_REF:-HEAD}"

if ! git rev-parse --verify "${BASE_REF}^{commit}" >/dev/null 2>&1; then
  if [[ "${BASE_REF}" == origin/* ]]; then
    git fetch --quiet origin "${BASE_REF#origin/}"
  fi
fi

if ! git rev-parse --verify "${BASE_REF}^{commit}" >/dev/null 2>&1; then
  echo "Cannot resolve base ref: ${BASE_REF}" >&2
  exit 1
fi

if [[ "${HEAD_REF}" != "WORKTREE" ]] && ! git rev-parse --verify "${HEAD_REF}^{commit}" >/dev/null 2>&1; then
  echo "Cannot resolve head ref: ${HEAD_REF}" >&2
  exit 1
fi

changed_files=()
if [[ "${HEAD_REF}" == "WORKTREE" ]]; then
  while IFS= read -r changed_file; do
    [[ -n "${changed_file}" ]] && changed_files+=("${changed_file}")
  done < <(git diff --name-only "${BASE_REF}")
  while IFS= read -r changed_file; do
    [[ -n "${changed_file}" ]] && changed_files+=("${changed_file}")
  done < <(git ls-files --others --exclude-standard)
else
  while IFS= read -r changed_file; do
    [[ -n "${changed_file}" ]] && changed_files+=("${changed_file}")
  done < <(git diff --name-only "${BASE_REF}" "${HEAD_REF}")
fi

if [[ "${#changed_files[@]}" -eq 0 ]]; then
  echo "Doc sync check passed: no changed files."
  exit 0
fi

file_changed() {
  local expected="$1"
  local file
  for file in "${changed_files[@]}"; do
    if [[ "${file}" == "${expected}" ]]; then
      return 0
    fi
  done
  return 1
}

any_required_doc_changed() {
  local docs_csv="$1"
  local doc
  local old_ifs="${IFS}"
  IFS=','
  for doc in ${docs_csv}; do
    IFS="${old_ifs}"
    if file_changed "${doc}"; then
      IFS="${old_ifs}"
      return 0
    fi
    IFS=','
  done
  IFS="${old_ifs}"
  return 1
}

failures=()
tab="$(printf '\t')"

while IFS="${tab}" read -r pattern docs_csv reason; do
  [[ -z "${pattern:-}" ]] && continue
  [[ "${pattern}" == \#* ]] && continue
  [[ -z "${docs_csv:-}" ]] && continue

  matched=0
  for changed_file in "${changed_files[@]}"; do
    case "${changed_file}" in
      ${pattern})
        matched=1
        ;;
      *)
        ;;
    esac
  done

  if [[ "${matched}" == "1" ]] && ! any_required_doc_changed "${docs_csv}"; then
    failures+=("${pattern} -> ${docs_csv} (${reason:-no reason})")
  fi
done < "${MAP_FILE}"

if [[ "${#failures[@]}" -gt 0 ]]; then
  echo "Doc sync check failed. Update at least one required doc for each changed area:" >&2
  printf '  - %s\n' "${failures[@]}" >&2
  echo >&2
  echo "Changed files:" >&2
  printf '  %s\n' "${changed_files[@]}" >&2
  exit 1
fi

echo "Doc sync check passed."
