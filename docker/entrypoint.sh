#!/bin/bash
set -euo pipefail

CLOUDFEEDS_ENV=${CLOUDFEEDS_ENV:-test}
FEEDSCATALOG_CONFIG_ROOT=${FEEDSCATALOG_CONFIG_ROOT:-/opt/feedscatalog-config/env}
FEEDSCATALOG_TARGET=${FEEDSCATALOG_TARGET:-/etc/feedscatalog/feedscatalog.xml}

escape_for_sed() {
  printf '%s' "$1" | sed 's/[\/&]/\\&/g'
}

replace_xml_value() {
  local tag_name=$1
  local new_value=$2
  local escaped_value

  escaped_value=$(escape_for_sed "$new_value")
  sed -i.bak "s|<${tag_name}>[^<]*</${tag_name}>|<${tag_name}>${escaped_value}</${tag_name}>|g" "${FEEDSCATALOG_TARGET}"
  rm -f "${FEEDSCATALOG_TARGET}.bak"
}

configure_feedscatalog() {
  local source_file="${FEEDSCATALOG_CONFIG_ROOT}/${CLOUDFEEDS_ENV}/feedscatalog.xml"

  if [ ! -f "${source_file}" ]; then
    echo "Unsupported CLOUDFEEDS_ENV='${CLOUDFEEDS_ENV}'. Expected a feedscatalog file at ${source_file}." >&2
    exit 1
  fi

  cp "${source_file}" "${FEEDSCATALOG_TARGET}"

  if [ -n "${FEEDSCATALOG_REGION:-}" ]; then
    replace_xml_value "region" "${FEEDSCATALOG_REGION}"
  fi

  if [ -n "${FEEDSCATALOG_VIP_URL:-}" ]; then
    replace_xml_value "vipURL" "${FEEDSCATALOG_VIP_URL}"
  fi

  if [ -n "${FEEDSCATALOG_EXTERNAL_VIP_URL:-}" ]; then
    replace_xml_value "externalVipURL" "${FEEDSCATALOG_EXTERNAL_VIP_URL}"
  fi

  if [ -n "${FEEDSCATALOG_PREFS_VIP_URL:-}" ]; then
    replace_xml_value "prefsSvcVipURL" "${FEEDSCATALOG_PREFS_VIP_URL}"
  fi
}

configure_feedscatalog
echo "Using feedscatalog config for CLOUDFEEDS_ENV=${CLOUDFEEDS_ENV}"

exec "$@"
