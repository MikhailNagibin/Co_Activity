# Service Modules

This directory contains 3 equal service modules.

- `core-service` - auth, users/rooms/requests, outbox producer
- `qa-service` - Q&A domain with JWT validation
- `notifications-service` - email dispatch (HTTP + Kafka consumer)

Build/run examples:

```bash
./mvnw -f services/core-service/pom.xml spring-boot:run
./mvnw -f services/qa-service/pom.xml spring-boot:run
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

Notifications endpoint:

- `POST /api/notifications/email`

QA endpoints:

- `POST /api/qa/questions`
- `POST /api/qa/answers`
- `GET /api/qa/questions`
- `GET /api/qa/questions/category?categoryId=...`
- `GET /api/qa/questions/{questionId}`
