# Service Modules

This directory contains the three backend services.

- `core-service` - the only public HTTP API; auth, users, rooms, join requests, facade for Q&A
- `qa-service` - internal HTTP microservice for Q&A
- `notifications-service` - Kafka consumer that sends email through Yandex SMTP

Build or run locally:

```bash
./mvnw -f services/core-service/pom.xml spring-boot:run
./mvnw -f services/qa-service/pom.xml spring-boot:run
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

Before running `core-service` or `qa-service`, set `JWT_SECRET_BASE64` to your own Base64-encoded secret.

Canonical runtime flow:

```text
client -> core-service
core-service -> qa-service (HTTP)
core-service -> Kafka -> notifications-service -> Yandex SMTP
```
