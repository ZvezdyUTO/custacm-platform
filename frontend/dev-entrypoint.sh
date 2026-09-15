#!/bin/sh
# Internal container entrypoint; start development with scripts/dev.sh.
set -eu

training_pid=""
blog_pid=""
# shellcheck disable=SC2329 # Invoked indirectly by the exit trap.
cleanup() {
  trap - EXIT INT TERM
  [ -z "$blog_pid" ] || kill "$blog_pid" 2>/dev/null || true
  [ -z "$training_pid" ] || kill "$training_pid" 2>/dev/null || true
  [ -z "$blog_pid" ] || wait "$blog_pid" 2>/dev/null || true
  [ -z "$training_pid" ] || wait "$training_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 0' INT TERM

cd /workspace/frontend
training_hash="$({ cat package.json pnpm-lock.yaml; node --version; pnpm --version; } | sha256sum | cut -d ' ' -f 1)"
if [ ! -x node_modules/.bin/vite ] || [ "$(cat node_modules/.custacm-deps-hash 2>/dev/null || true)" != "$training_hash" ]; then
  echo "Installing Training dependencies into the development volume ..."
  pnpm install --frozen-lockfile --store-dir /pnpm/store
  printf '%s\n' "$training_hash" > node_modules/.custacm-deps-hash
fi

cd /workspace/blog-view
blog_hash="$( { cat package.json; node --version; npm --version; } | sha256sum | cut -d ' ' -f 1)"
if [ ! -x node_modules/.bin/vite ] || [ "$(cat node_modules/.custacm-deps-hash 2>/dev/null || true)" != "$blog_hash" ]; then
  echo "Installing Blog dependencies into the development volume ..."
  npm install --package-lock=false --no-audit --no-fund
  printf '%s\n' "$blog_hash" > node_modules/.custacm-deps-hash
fi

(cd /workspace/frontend && exec node node_modules/vite/bin/vite.js) &
training_pid=$!
(cd /workspace/blog-view && exec node node_modules/vite/bin/vite.js) &
blog_pid=$!

while kill -0 "$training_pid" 2>/dev/null && kill -0 "$blog_pid" 2>/dev/null; do
  sleep 1
done
echo "A Vite server exited unexpectedly; stopping the development container." >&2
exit 1
