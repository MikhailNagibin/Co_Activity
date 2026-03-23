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

wait_for_core_health() {
  local health_status=""
  for attempt in $(seq 1 30); do
    if health_status="$(curl -sS -o /tmp/coactivity_core_health.out -w "%{http_code}" \
      http://localhost:8080/actuator/health 2>/tmp/coactivity_core_health.err)"; then
      if [[ "${health_status}" == "200" ]]; then
        return 0
      fi
    fi
    sleep 1
  done

  if [[ -s /tmp/coactivity_core_health.out ]]; then
    cat /tmp/coactivity_core_health.out
    echo
  fi
  if [[ -s /tmp/coactivity_core_health.err ]]; then
    cat /tmp/coactivity_core_health.err >&2
  fi
  echo "core-service health check failed with HTTP ${health_status:-unknown}" >&2
  docker compose logs core-service --tail=100 || true
  return 1
}

wait_for_notifications_health() {
  for attempt in $(seq 1 30); do
    if [[ "$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{end}}' \
      coactivity_notifications_service 2>/tmp/coactivity_notifications_health.err || true)" \
      == "healthy" ]]; then
      return 0
    fi
    sleep 1
  done

  if [[ -s /tmp/coactivity_notifications_health.err ]]; then
    cat /tmp/coactivity_notifications_health.err >&2
  fi
  echo "notifications-service health check failed" >&2
  docker compose logs notifications-service --tail=100 || true
  return 1
}

wait_for_email_delivery() {
  local since_ts="$1"
  local success_pattern="Kafka email command delivered to ${TEST_LOGIN_EMAIL}"
  local failure_pattern="Failed to deliver Kafka email command"
  local logs=""

  for attempt in $(seq 1 15); do
    logs="$(docker compose logs --since "${since_ts}" notifications-service \
      2>/tmp/coactivity_notifications_delivery.err || true)"
    if grep -Fq "${success_pattern}" <<< "${logs}"; then
      return 0
    fi
    if grep -Fq "${failure_pattern}" <<< "${logs}"; then
      echo "${logs}"
      return 1
    fi
    sleep 1
  done

  if [[ -s /tmp/coactivity_notifications_delivery.err ]]; then
    cat /tmp/coactivity_notifications_delivery.err >&2
  fi
  echo "Timed out waiting for delivery confirmation in notifications-service logs" >&2
  if [[ -n "${logs}" ]]; then
    echo "${logs}"
  fi
  return 1
}

prompt_if_empty "SPRING_MAIL_USERNAME" "Yandex sender address: "
prompt_if_empty "SPRING_MAIL_PASSWORD" "Yandex app password: " "true"

if [[ -z "${SPRING_MAIL_USERNAME}" || -z "${SPRING_MAIL_PASSWORD}" ]]; then
  echo "Yandex sender and app password are required" >&2
  exit 1
fi

if [[ -z "${TEST_LOGIN_EMAIL:-}" ]]; then
  read -r -p "Verification email address [${SPRING_MAIL_USERNAME}]: " TEST_LOGIN_EMAIL
  TEST_LOGIN_EMAIL="${TEST_LOGIN_EMAIL:-${SPRING_MAIL_USERNAME}}"
fi

if [[ -z "${TEST_LOGIN_EMAIL}" ]]; then
  echo "Verification email address is required" >&2
  exit 1
fi

TEST_USERNAME="${TEST_USERNAME:-cay$(date +%s)}"
TEST_PASSWORD="${TEST_PASSWORD:-Password123}"

export SPRING_MAIL_HOST="smtp.yandex.ru"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_SMTP_AUTH="true"
export SPRING_MAIL_SMTP_STARTTLS_ENABLE="true"
export SPRING_MAIL_SMTP_STARTTLS_REQUIRED="true"
export SPRING_MAIL_USERNAME
export SPRING_MAIL_PASSWORD

echo "Rebuilding notifications-service with Yandex SMTP settings..."
docker compose up --build -d --no-deps notifications-service

echo "Waiting for core-service to start..."
wait_for_core_health

echo "Waiting for notifications-service to start..."
wait_for_notifications_health

echo "Registering a temporary user for email smoke test..."
register_status="$(curl -sS -o /tmp/coactivity_register.out -w "%{http_code}" \
  -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d "{
    \"login\": \"${TEST_LOGIN_EMAIL}\",
    \"userName\": \"${TEST_USERNAME}\",
    \"password\": \"${TEST_PASSWORD}\",
    \"dateOfBirth\": \"2000-01-01T00:00:00Z\",
    \"city\": \"Moscow\",
    \"country\": \"Russia\",
    \"description\": \"Yandex SMTP smoke test user\",
    \"avatarId\": 1
  }")"

if [[ "${register_status}" != "201" && "${register_status}" != "409" ]]; then
  if [[ -s /tmp/coactivity_register.out ]]; then
    cat /tmp/coactivity_register.out
    echo
  fi
  echo "Registration failed with HTTP ${register_status}" >&2
  docker compose logs core-service --tail=100 || true
  exit 1
fi

if [[ "${register_status}" == "409" ]]; then
  echo "User already exists, continuing with login flow..."
fi

delivery_check_since="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

echo "Triggering login verification email through core-service..."
login_status="$(curl -sS -o /tmp/coactivity_login.out -w "%{http_code}" \
  -X POST http://localhost:8080/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{
    \"login\": \"${TEST_LOGIN_EMAIL}\",
    \"password\": \"${TEST_PASSWORD}\"
  }")"

if [[ -s /tmp/coactivity_login.out ]]; then
  cat /tmp/coactivity_login.out
  echo
fi

echo "HTTP status: ${login_status}"
echo
echo "Recent core-service logs:"
docker compose logs core-service --tail=100 || true
echo
echo "Recent notifications-service logs:"
docker compose logs notifications-service --tail=100 || true

if [[ "${login_status}" != "202" ]]; then
  cat <<EOF

Login verification flow did not return 202.
Typical causes:
- 401: the user already exists with a different password
- 503: Kafka publish or notifications delivery pipeline is unavailable
EOF
  exit 1
fi

echo
echo "Waiting for notifications-service delivery confirmation..."
if ! wait_for_email_delivery "${delivery_check_since}"; then
  cat <<EOF

Email delivery was not confirmed by notifications-service.
Typical causes:
- MailConnectException / timeout: container cannot reach smtp.yandex.ru
- MailAuthenticationException: invalid Yandex app password
- SMTP provider rejected the connection or delivery attempt
EOF
  exit 1
fi

cat <<EOF

Login verification flow returned 202 and notifications-service confirmed email delivery.
Now verify the message in the inbox or spam folder of: ${TEST_LOGIN_EMAIL}
EOF
