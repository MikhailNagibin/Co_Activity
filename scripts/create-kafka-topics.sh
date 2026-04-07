#!/usr/bin/env bash
set -euo pipefail

KAFKA_BIN="${KAFKA_BIN:-/opt/kafka/bin/kafka-topics.sh}"
BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
NOTIFICATIONS_TOPIC="${NOTIFICATIONS_KAFKA_TOPIC:-notifications.email.v1}"
DLT_TOPIC="${NOTIFICATIONS_KAFKA_DLT_TOPIC:-}"
PARTITIONS="${NOTIFICATIONS_KAFKA_PARTITIONS:-1}"
REPLICATION_FACTOR="${NOTIFICATIONS_KAFKA_REPLICATION_FACTOR:-1}"

if [[ -z "${DLT_TOPIC}" ]]; then
  DLT_TOPIC="${NOTIFICATIONS_TOPIC}.dlt"
fi

until "${KAFKA_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" --list >/dev/null 2>&1; do
  sleep 2
done

"${KAFKA_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create \
  --if-not-exists \
  --topic "${NOTIFICATIONS_TOPIC}" \
  --partitions "${PARTITIONS}" \
  --replication-factor "${REPLICATION_FACTOR}"

"${KAFKA_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create \
  --if-not-exists \
  --topic "${DLT_TOPIC}" \
  --partitions "${PARTITIONS}" \
  --replication-factor "${REPLICATION_FACTOR}"
