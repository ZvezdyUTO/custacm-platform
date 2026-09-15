#!/bin/sh
set -eu

FRONTEND_IMAGE_REFERER_HOSTS='custacm.top,*.custacm.top,custacm.*' \
FRONTEND_ALLOW_LOCAL_REFERERS=true \
  /docker-entrypoint.d/10-select-nginx-config.sh

grep -F 'valid_referers custacm.top *.custacm.top custacm.* localhost 127.0.0.1;' \
  /etc/nginx/conf.d/default.conf >/dev/null
grep -F 'sub_filter "/api/image/" "https://www.custacm.top/api/image/";' \
  /etc/nginx/conf.d/default.conf >/dev/null
sed -i 's|proxy_pass http://blog-api:8090;|proxy_pass http://127.0.0.1:8090;|' \
  /etc/nginx/conf.d/default.conf
nginx -t

for invalid_val in '**.example.com' 'example.**' '*.*' 'foo*bar'; do
  if FRONTEND_IMAGE_REFERER_HOSTS="$invalid_val" \
    /docker-entrypoint.d/10-select-nginx-config.sh 2>/dev/null; then
    echo "FAIL: entrypoint script should have rejected invalid referer: $invalid_val" >&2
    exit 1
  fi
done

# Exercise SPA fallbacks and missing assets against Nginx, not just its parser.
mkdir -p /usr/share/nginx/html/blog/assets /usr/share/nginx/html/training-app/assets
printf 'blog-entry' > /usr/share/nginx/html/blog/index.html
printf 'training-entry' > /usr/share/nginx/html/training-app/index.html
printf '/* cache-check */' > /usr/share/nginx/html/blog/assets/cache-check.js
printf '/* cache-check */' > /usr/share/nginx/html/training-app/assets/cache-check.js

nginx -g 'daemon off;' >/tmp/nginx-cache-check.log 2>&1 &
nginx_test_pid=$!
cleanup() {
  kill "$nginx_test_pid" 2>/dev/null || true
  wait "$nginx_test_pid" 2>/dev/null || true
  rm -f /usr/share/nginx/html/blog/index.html /usr/share/nginx/html/training-app/index.html \
    /usr/share/nginx/html/blog/assets/cache-check.js /usr/share/nginx/html/training-app/assets/cache-check.js \
    /tmp/nginx-cache-check.log /tmp/nginx-cache-check.headers /tmp/nginx-cache-check.body
}
trap cleanup EXIT

for attempt in 1 2 3 4 5; do
  if wget -q -T 2 -O /dev/null http://127.0.0.1/; then break; fi
  sleep 1
done

for route in / /training/multiple /training-app/multiple /training-app/admin/categories; do
  wget -S -T 2 -O /tmp/nginx-cache-check.body "http://127.0.0.1$route" 2>/tmp/nginx-cache-check.headers
  grep -F 'Cache-Control: no-store' /tmp/nginx-cache-check.headers >/dev/null
  grep -F 'X-Frame-Options: SAMEORIGIN' /tmp/nginx-cache-check.headers >/dev/null
  grep -F "Content-Security-Policy: frame-ancestors 'self'" /tmp/nginx-cache-check.headers >/dev/null
  case "$route" in
    /training-app/*) grep -F 'training-entry' /tmp/nginx-cache-check.body >/dev/null ;;
    *) grep -F 'blog-entry' /tmp/nginx-cache-check.body >/dev/null ;;
  esac
done

for prefix in /assets /training-app/assets; do
  wget -S -T 2 -O /dev/null "http://127.0.0.1$prefix/cache-check.js" 2>/tmp/nginx-cache-check.headers
  grep -F 'Content-Type: application/javascript' /tmp/nginx-cache-check.headers >/dev/null
  if grep -F 'Cache-Control: no-store' /tmp/nginx-cache-check.headers >/dev/null; then
    echo 'FAIL: hashed build assets must remain cacheable' >&2
    exit 1
  fi
  if wget -S -T 2 -O /dev/null "http://127.0.0.1$prefix/missing-build.js" 2>/tmp/nginx-cache-check.headers; then
    echo 'FAIL: missing build assets must not fall back to HTML' >&2
    exit 1
  fi
  grep -F '404 Not Found' /tmp/nginx-cache-check.headers >/dev/null
done
echo 'Nginx HTML cache and asset routing checks passed.'
