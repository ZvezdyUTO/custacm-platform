#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALLOWLIST_FILE="${ROOT_DIR}/docs/test-policy-allowlist.tsv"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/check-test-policy.sh

Run this after `mvn clean verify`. It checks that Java modules with executable
source either have tests and generated reports, or are explicitly listed in
docs/test-policy-allowlist.tsv.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "help" ]]; then
  usage
  exit 0
fi

cd "${ROOT_DIR}"

allowed_without_tests() {
  local module="$1"
  local path reason

  [[ -f "${ALLOWLIST_FILE}" ]] || return 1

  while IFS=$'\t' read -r path reason; do
    [[ -z "${path:-}" ]] && continue
    [[ "${path}" == \#* ]] && continue
    if [[ "${path}" == "${module}" ]]; then
      return 0
    fi
  done < "${ALLOWLIST_FILE}"

  return 1
}

count_xml_attr() {
  local file="$1"
  local attr="$2"
  sed -n "s/.* ${attr}=\\\"\\([0-9][0-9]*\\)\\\".*/\\1/p" "${file}" | head -n 1
}

modules=()
while IFS= read -r pom; do
  module_dir="$(dirname "${pom#./}")"
  [[ "${module_dir}" == "." ]] && continue
  [[ -d "${module_dir}/src/main/java" ]] || continue
  modules+=("${module_dir}")
done < <(find . -path './.git' -prune -o -path './target' -prune -o -path './logs' -prune -o -path './.idea' -prune -o -name pom.xml -print | sort)

if [[ "${#modules[@]}" -eq 0 ]]; then
  echo "Test policy check passed: no Java source modules found."
  exit 0
fi

failures=()
total_tests=0
total_failures=0
total_errors=0
total_skipped=0

for module in "${modules[@]}"; do
  main_count="$(find "${module}/src/main/java" -type f -name '*.java' | wc -l | tr -d ' ')"
  [[ "${main_count}" == "0" ]] && continue

  test_count=0
  if [[ -d "${module}/src/test/java" ]]; then
    test_count="$(find "${module}/src/test/java" -type f -name '*Test.java' | wc -l | tr -d ' ')"
  fi

  if [[ "${test_count}" == "0" ]]; then
    if allowed_without_tests "${module}"; then
      echo "Allowlisted without tests: ${module}"
      continue
    fi
    failures+=("${module}: has ${main_count} Java source file(s) but no *Test.java files")
    continue
  fi

  report_dir="${module}/target/surefire-reports"
  if [[ ! -d "${report_dir}" ]] || ! compgen -G "${report_dir}/TEST-*.xml" >/dev/null; then
    failures+=("${module}: has tests but no Surefire XML reports; run mvn clean verify before this check")
    continue
  fi

  module_tests=0
  module_failures=0
  module_errors=0
  module_skipped=0

  for report in "${report_dir}"/TEST-*.xml; do
    tests="$(count_xml_attr "${report}" tests)"
    failures_count="$(count_xml_attr "${report}" failures)"
    errors_count="$(count_xml_attr "${report}" errors)"
    skipped_count="$(count_xml_attr "${report}" skipped)"

    module_tests=$((module_tests + ${tests:-0}))
    module_failures=$((module_failures + ${failures_count:-0}))
    module_errors=$((module_errors + ${errors_count:-0}))
    module_skipped=$((module_skipped + ${skipped_count:-0}))
  done

  if [[ "${module_tests}" -eq 0 ]]; then
    failures+=("${module}: Surefire reports contain zero tests")
  fi

  if [[ "${module_failures}" -ne 0 || "${module_errors}" -ne 0 ]]; then
    failures+=("${module}: tests=${module_tests}, failures=${module_failures}, errors=${module_errors}, skipped=${module_skipped}")
  fi

  jacoco_report="${module}/target/site/jacoco/jacoco.xml"
  if [[ ! -f "${jacoco_report}" ]]; then
    failures+=("${module}: missing JaCoCo XML report; run mvn clean verify and check coverage configuration")
  fi

  total_tests=$((total_tests + module_tests))
  total_failures=$((total_failures + module_failures))
  total_errors=$((total_errors + module_errors))
  total_skipped=$((total_skipped + module_skipped))

  echo "Checked ${module}: tests=${module_tests}, failures=${module_failures}, errors=${module_errors}, skipped=${module_skipped}"
done

if [[ "${#failures[@]}" -gt 0 ]]; then
  echo "Test policy check failed:" >&2
  printf '  - %s\n' "${failures[@]}" >&2
  exit 1
fi

echo "Test policy check passed: tests=${total_tests}, failures=${total_failures}, errors=${total_errors}, skipped=${total_skipped}"
