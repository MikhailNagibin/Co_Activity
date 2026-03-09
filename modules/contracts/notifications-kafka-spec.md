# Notifications Kafka Contract v1 (Co_Activity)

## Topic
- `notifications.email.v1`

## Producer
- `core` service (`NotificationService` in `NOTIFICATIONS_MODE=KAFKA`).

## Consumer
- `notifications-service` (`KafkaEmailConsumer`).

## Payload schema (JSON)
```json
{
  "to": "user@example.com",
  "subject": "string",
  "body": "string"
}
```

## Rules
- `to` must be non-empty email string.
- `subject` must be non-empty string.
- `body` must be non-empty string.
- Producer key: recipient email (`to`) to keep ordering per recipient.

## Delivery semantics (v1)
- `core` waits for Kafka publish ack before marking outbox event as sent.
- Consumer is at-least-once (duplicates are possible).
- Email delivery should be idempotent on business level where possible.
