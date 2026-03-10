#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 1
fi

prompt_if_empty() {
  local var_name="$1"
  local prompt_text="$2"
  local secret="${3:-false}"

  if [[ -n "${!var_name:-}" ]]; then
    return
  fi

  if [[ "${secret}" == "true" ]]; then
    read -r -s -p "${prompt_text}" "${var_name}"
    echo
  else
    read -r -p "${prompt_text}" "${var_name}"
  fi
}

prompt_if_empty "SPRING_MAIL_USERNAME" "Gmail sender address: "
prompt_if_empty "SPRING_MAIL_PASSWORD" "Gmail app password: " "true"

if [[ -z "${TO_EMAIL:-}" ]]; then
  read -r -p "Recipient email [${SPRING_MAIL_USERNAME}]: " TO_EMAIL
  TO_EMAIL="${TO_EMAIL:-${SPRING_MAIL_USERNAME}}"
fi

if [[ -z "${SPRING_MAIL_USERNAME}" || -z "${SPRING_MAIL_PASSWORD}" ]]; then
  echo "Gmail sender and app password are required" >&2
  exit 1
fi

export SPRING_MAIL_HOST="smtp.gmail.com"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_SMTP_AUTH="true"
export SPRING_MAIL_SMTP_STARTTLS_ENABLE="true"
export SPRING_MAIL_SMTP_STARTTLS_REQUIRED="true"
export SPRING_MAIL_USERNAME
export SPRING_MAIL_PASSWORD

echo "Rebuilding notifications-service with Gmail SMTP settings..."
docker compose up --build -d --no-deps notifications-service

echo "Stopping Mailpit to avoid false positives..."
docker compose stop mailpit >/dev/null 2>&1 || true

echo "Waiting for notifications-service to start..."
health_status=""
for attempt in $(seq 1 30); do
  if health_status="$(curl -sS -o /tmp/coactivity_gmail_health.out -w "%{http_code}" \
    http://localhost:8082/actuator/health 2>/tmp/coactivity_gmail_health.err)"; then
    if [[ "${health_status}" == "200" ]]; then
      break
    fi
  fi
  sleep 1
done

echo "Checking notifications-service health..."
if [[ -s /tmp/coactivity_gmail_health.out ]]; then
  cat /tmp/coactivity_gmail_health.out
  echo
fi
if [[ "${health_status}" != "200" ]]; then
  if [[ -s /tmp/coactivity_gmail_health.err ]]; then
    cat /tmp/coactivity_gmail_health.err >&2
  fi
  echo "notifications-service health check failed with HTTP ${health_status:-unknown}" >&2
  docker compose logs notifications-service --tail=100
  exit 1
fi

echo "Sending direct Gmail smoke test..."
email_status="$(curl -sS -o /tmp/coactivity_gmail_send.out -w "%{http_code}" \
  -X POST http://localhost:8082/api/notifications/email \
  -H 'Content-Type: application/json' \
  -d "{
    \"to\": \"${TO_EMAIL}\",
    \"subject\": \"CoActivity Gmail smoke test\",
    \"body\": \"This is a direct SMTP smoke test from notifications-service\"
  }")"

if [[ -s /tmp/coactivity_gmail_send.out ]]; then
  cat /tmp/coactivity_gmail_send.out
  echo
fi

echo "HTTP status: ${email_status}"
echo
echo "Recent notifications-service logs:"
docker compose logs notifications-service --tail=100

if [[ "${email_status}" == "204" ]]; then
  cat <<EOF

Gmail SMTP request returned 204.
Now verify the message in the inbox or spam folder of: ${TO_EMAIL}

Rollback to Mailpit:
  docker compose up -d mailpit
  docker compose up -d --no-deps notifications-service
EOF
else
  cat <<EOF

Gmail SMTP request did not return 204.
Typical causes:
- AuthenticationFailedException or 535-5.7.8: wrong password or not an app password
- MailConnectException / timeout: container cannot reach smtp.gmail.com:587
EOF
  exit 1
fi
