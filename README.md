# Co_Activity

Backend service based on Java 21 + Spring Boot + PostgreSQL.

## Prerequisites

- Docker Desktop (with `docker compose`)
- Java 21 (for local run)
- Optional: Maven (or use `./mvnw`)

Quick checks:

```bash
java -version
./mvnw -v
docker --version
docker compose version
```

## Why `config.json` exists

This project has custom JDBC repositories that use `DataRepository`.
`DataRepository` reads DB settings from `DB_*` environment variables first, and if they are absent,
falls back to `src/main/resources/config.json`.

This allows:

- Docker run: use `DB_*` from compose (`postgres:5432`)
- Local run: keep using `config.json` (`localhost:5430`) if needed

## Start with Docker Compose (recommended)

1. Create local environment file:

```bash
cp .env.example .env
```

2. Start full local microservices stack (postgres + kafka + core + qa-service + notifications-service):

```bash
docker compose up --build -d
```

3. Check status:

```bash
docker compose ps
docker compose logs -f postgres
docker compose logs -f kafka
docker compose logs -f app
docker compose logs -f qa-service
docker compose logs -f notifications-service
```

4. Health check:

```bash
curl http://localhost:8080/actuator/health
```

5. Stop:

```bash
docker compose down
```

If you need a clean database:

```bash
docker compose down -v
```

## Mixed mode (DB in Docker, app local)

1. Start only PostgreSQL + Kafka:

```bash
docker compose up -d postgres kafka
```

2. Run app locally:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5430/postgres_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
DB_HOST=localhost \
DB_PORT=5430 \
DB_NAME=postgres_db \
DB_USER=postgres \
DB_PASSWORD=postgres \
./mvnw spring-boot:run
```

## Modular microservices skeleton (monorepo)

Current repository now contains starter modules for gradual extraction:

- `core` (current app in repository root)
- `modules/qa-service`
- `modules/notifications-service`
- `modules/contracts`

Run module services independently:

```bash
./mvnw -f modules/qa-service/pom.xml spring-boot:run
./mvnw -f modules/notifications-service/pom.xml spring-boot:run
```

Run Kafka consumer integration test for notifications-service:

```bash
./mvnw -f modules/notifications-service/pom.xml -Dtest=KafkaEmailConsumerIntegrationTest test
```

`qa-service` now contains a working Q&A API:

- `POST /api/qa/questions`
- `POST /api/qa/answers`
- `GET /api/qa/questions`
- `GET /api/qa/questions/category?categoryId=...`
- `GET /api/qa/questions/{questionId}`

Core can route QA requests in two modes:

- `QA_MODE=MONOLITH` (default): handles Q&A inside core
- `QA_MODE=SERVICE`: proxies Q&A requests to `QA_SERVICE_BASE_URL`

Notifications can also run in two modes:

- `NOTIFICATIONS_MODE=INPROC` (default): core sends emails directly
- `NOTIFICATIONS_MODE=SERVICE`: core calls `NOTIFICATIONS_SERVICE_BASE_URL`
- `NOTIFICATIONS_MODE=KAFKA`: core publishes email commands to Kafka topic

In `docker compose`, core is fixed to `QA_MODE=SERVICE` and `NOTIFICATIONS_MODE=KAFKA`
to validate extracted modules with event delivery.

Transactional outbox is supported for join request decisions:

- set `OUTBOX_ENABLED=true` to persist membership notification events in `outbox_events`
- background worker polls outbox and delivers notifications with retries
- with Kafka mode enabled, outbox worker publishes to `NOTIFICATIONS_KAFKA_TOPIC`,
  then `notifications-service` consumes and sends emails

Core outbox/notification flow tests:

```bash
./mvnw -Dtest=NotificationOutboxWorkerTest,NotificationOutboxKafkaFlowTest test
./mvnw -Dtest=NotificationIntegrationTest test
```

## JWT configuration (v1)

Core now uses signed JWT access tokens (no refresh token yet).
Set these env vars for non-default setup:

```bash
JWT_SECRET_BASE64=...
JWT_ISSUER=coactivity-core
JWT_AUDIENCE=coactivity-api
JWT_EXPIRATION_MINUTES=30
```
