#!/bin/sh
set -eu

config=/etc/nginx/custacm/nginx-http.conf
if [ "${TLS_ENABLED:-false}" = "true" ]; then
  config=/etc/nginx/custacm/nginx-https.conf
fi

cp "$config" /etc/nginx/conf.d/default.conf
