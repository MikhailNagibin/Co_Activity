# Notifications Kafka Contract v1 (Co_Activity)

## Topic
- `notifications.email.v1`
- default DLT: `notifications.email.v1.dlt`

## Ownership
- These topics are infrastructure-managed in production.
- `core-service` and `notifications-service` use the configured topic names, but neither service is the production owner of topic provisioning.
- Local development may create the topics via Docker Compose bootstrap or explicit `kafka-topics` commands before the services start.

## Producer
- `core-service` (`NotificationService`)

## Consumer
- `notifications-service` (`KafkaEmailConsumer`)

## Payload schema (JSON)
```json
{
  "to": "user@example.com",
  "subject": "string",
  "body": "string"
}
```

## Rules
- `to` must be a non-empty email string.
- `subject` must be a non-empty string.
- `body` must be a non-empty string.
- Producer key is recipient email (`to`) to keep ordering per recipient.

## Delivery semantics
- `login` verification waits for Kafka publish acknowledgement before `core-service` returns success.
- Other notification emails are published after the main DB change and are best-effort.
- Consumer is at-least-once, so duplicates are possible.
- Email delivery should be idempotent on business level where possible.
- `MailException` is retried and, after retries are exhausted, the original record is published to DLT.
- Invalid JSON or DTO validation failures are not retried and are published to DLT immediately.

## Observability
- `notifications-service` should expose actuator health for SMTP connectivity and Kafka topic presence.
- DLT publications should be observable via metrics, not only through logs.
