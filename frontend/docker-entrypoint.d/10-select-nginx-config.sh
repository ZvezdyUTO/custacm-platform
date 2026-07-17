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

validate_referer_pattern() {
  printf '%s\n' "$1" | awk '
    length($0) > 253 { exit 1 }
    {
      star_count = gsub(/\*/, "&", $0)
      if (star_count > 1) exit 1
      if (star_count == 1) {
        if ($0 !~ /^\*\.[A-Za-z0-9]/ && $0 !~ /^[A-Za-z0-9].*\.\*$/) exit 1
      }
    }
    {
      gsub(/^\*\./, "", $0)
      gsub(/\.\*$/, "", $0)
      if (length($0) == 0) exit 1
      if (length($0) > 253) exit 1
      if ($0 !~ /^[A-Za-z0-9]/ || $0 !~ /[A-Za-z0-9]$/) exit 1
      label_count = split($0, labels, ".")
      for (i = 1; i <= label_count; i++) {
        if (length(labels[i]) > 63 || labels[i] !~ /^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?$/) {
          exit 1
        }
      }
      exit 0
    }
  '
}

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
  esac

  if ! validate_referer_pattern "$referer"; then
    echo "Invalid FRONTEND_IMAGE_REFERER_HOSTS token: $referer" >&2
    exit 1
  fi

  case " $trusted_referers " in
    *" $referer "*)
      ;;
    *)
      trusted_referers="${trusted_referers:+$trusted_referers }$referer"
      ;;
  esac
}

referer_hosts=$(printf '%s' "${FRONTEND_IMAGE_REFERER_HOSTS:-custacm.top,www.custacm.top}" | tr ',' ' ')
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

image_public_origin=${FRONTEND_IMAGE_PUBLIC_ORIGIN:-https://www.custacm.top}
case "$image_public_origin" in
  https://*)
    image_public_host=${image_public_origin#https://}
    ;;
  *)
    echo "FRONTEND_IMAGE_PUBLIC_ORIGIN must be an HTTPS origin without a path" >&2
    exit 1
    ;;
esac

case "$image_public_host" in
  */*|*'*'*)
    echo "FRONTEND_IMAGE_PUBLIC_ORIGIN must be an HTTPS origin without a path or wildcard" >&2
    exit 1
    ;;
esac

if ! validate_referer_pattern "$image_public_host"; then
  echo "Invalid FRONTEND_IMAGE_PUBLIC_ORIGIN host: $image_public_host" >&2
  exit 1
fi

sed \
  -e "s|__CUSTACM_IMAGE_TRUSTED_REFERERS__|$trusted_referers|g" \
  -e "s|__CUSTACM_IMAGE_PUBLIC_ORIGIN__|$image_public_origin|g" \
  "$config" > "$output_config"
