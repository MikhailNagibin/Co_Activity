# Microservice Modules (starter)

This directory contains independent service modules for incremental extraction.

- `qa-service` - working Q&A service with JWT validation
- `notifications-service` - email dispatch service
- `contracts` - shared contracts/specifications

Build/run examples:

```bash
./mvnw -f modules/qa-service/pom.xml spring-boot:run
./mvnw -f modules/notifications-service/pom.xml spring-boot:run
```

Notifications endpoint:

- `POST /api/notifications/email`

QA endpoints:

- `POST /api/qa/questions`
- `POST /api/qa/answers`
- `GET /api/qa/questions`
- `GET /api/qa/questions/category?categoryId=...`
- `GET /api/qa/questions/{questionId}`
