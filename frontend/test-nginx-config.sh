#!/bin/sh
set -eu

FRONTEND_IMAGE_REFERER_HOSTS='custacm.top,*.custacm.top,custacm.*' \
FRONTEND_ALLOW_LOCAL_REFERERS=true \
  /docker-entrypoint.d/10-select-nginx-config.sh

grep -F 'valid_referers custacm.top *.custacm.top custacm.* localhost 127.0.0.1;' \
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
