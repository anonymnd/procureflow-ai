#!/bin/sh
set -eu
# Require deployment configuration without printing credentials.
for name in PROCUREFLOW_DB_URL PROCUREFLOW_DB_USERNAME PROCUREFLOW_DB_PASSWORD \
    PROCUREFLOW_SMTP_HOST PROCUREFLOW_SMTP_USERNAME PROCUREFLOW_SMTP_PASSWORD \
    PROCUREFLOW_PUBLIC_BASE_URL PROCUREFLOW_EMAIL_FROM
do
    value="$(printenv "$name" 2>/dev/null || true)"
    if [ -z "$value" ]; then
        printf 'Missing required deployment setting: %s\n' "$name" >&2
        exit 1
    fi
done
case "$PROCUREFLOW_PUBLIC_BASE_URL" in
    https://*) ;;
    *) printf 'PROCUREFLOW_PUBLIC_BASE_URL must use HTTPS.\n' >&2; exit 1 ;;
esac
exec java -jar /app/app.jar
