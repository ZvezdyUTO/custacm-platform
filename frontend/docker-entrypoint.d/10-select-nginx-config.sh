#!/bin/sh
set -eu
set -f

config_dir=${CUSTACM_NGINX_CONFIG_DIR:-/etc/nginx/custacm}
output_config=${CUSTACM_NGINX_OUTPUT_CONFIG:-/etc/nginx/conf.d/default.conf}

config="$config_dir/nginx-http.conf"
if [ "${TLS_ENABLED:-false}" = "true" ]; then
  config="$config_dir/nginx-https.conf"
fi

trusted_referers=

append_referer() {
  referer=$1

  case "$referer" in
    "" )
      return
      ;;
    none|blocked )
      echo "Unsafe FRONTEND_IMAGE_REFERER_HOSTS token is not allowed: $referer" >&2
      exit 1
      ;;
    *[!A-Za-z0-9._*-]* )
      echo "Invalid FRONTEND_IMAGE_REFERER_HOSTS token: $referer" >&2
      exit 1
      ;;
  esac

  case " $trusted_referers " in
    *" $referer "*)
      ;;
    *)
      trusted_referers="${trusted_referers:+$trusted_referers }$referer"
      ;;
  esac
}

referer_hosts=$(printf '%s' "${FRONTEND_IMAGE_REFERER_HOSTS:-custacm.top}" | tr ',' ' ')
for referer_host in $referer_hosts; do
  append_referer "$referer_host"
done

if [ "${FRONTEND_ALLOW_LOCAL_REFERERS:-false}" = "true" ]; then
  append_referer localhost
  append_referer 127.0.0.1
fi

if [ -z "$trusted_referers" ]; then
  echo "FRONTEND_IMAGE_REFERER_HOSTS must contain at least one trusted referer host" >&2
  exit 1
fi

sed "s|__CUSTACM_IMAGE_TRUSTED_REFERERS__|$trusted_referers|g" "$config" > "$output_config"
