# Co_Activity

Backend monorepo on Java 21 + Spring Boot + PostgreSQL + Kafka.

## Current Architecture

- `core-service` is the only public HTTP API.
- `qa-service` is an internal HTTP microservice for Q&A.
- `notifications-service` is a Kafka consumer for email delivery.
- Canonical flow:
  - `client -> core-service`
  - `core-service -> qa-service` over internal HTTP
  - `core-service -> Kafka -> notifications-service -> Yandex SMTP`

This repo intentionally has one runtime path for each use case.
There is no monolith fallback and no direct business HTTP API in `notifications-service`.

## Prerequisites

- Docker Desktop with `docker compose`
- Java 21
- Maven wrapper `./mvnw`

Quick checks:

```bash
java -version
./mvnw -v
docker --version
docker compose version
```

## Start with Docker Compose

1. Create local env file:

```bash
cp .env.example .env
```

2. Generate a JWT secret and put it into `.env`:

```bash
openssl rand -base64 32
```

3. Start the stack:

```bash
docker compose up --build -d
```

4. Check the public API:

```bash
curl http://localhost:8080/actuator/health
```

5. Check internal service health through Compose:

```bash
docker compose ps
```

6. View logs:

```bash
docker compose logs -f core-service
docker compose logs -f qa-service
docker compose logs -f notifications-service
docker compose logs -f kafka
docker compose logs -f postgres
```

7. Stop:

```bash
docker compose down
```

If you changed the SQL schema, recreate PostgreSQL volume so init scripts run again:

```bash
docker compose down -v
docker compose up --build -d
```

## Public API Smoke Test

Only `core-service` is reachable from the host.

Basic checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/rooms
curl http://localhost:8080/api/qa/questions
```

## Yandex SMTP Smoke Test

The email flow is tested through the real business path: `core-service -> Kafka -> notifications-service`.

Before the test, make sure:

- IMAP/SMTP access is enabled in Yandex Mail settings
- you created a Yandex app password for Mail
- `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` in `.env` are filled in

Preferred path:

```bash
./scripts/yandex-smoke-test.sh
```

Manual path:

1. Register a temporary user:

```bash
TEST_EMAIL="student.$(date +%s)@example.com"

curl -i -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d "{
    \"login\": \"${TEST_EMAIL}\",
    \"userName\": \"yandex-smoke\",
    \"password\": \"Password123\",
    \"dateOfBirth\": \"2000-01-01T00:00:00Z\",
    \"city\": \"Moscow\",
    \"country\": \"Russia\",
    \"description\": \"Yandex SMTP smoke test user\",
    \"avatarId\": 1
  }"
```

2. Trigger login verification email:

```bash
curl -i -X POST http://localhost:8080/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{
    \"login\": \"${TEST_EMAIL}\",
    \"password\": \"Password123\"
  }"
```

Expected result:

- `202 Accepted` from `/api/users/login`
- a verification email appears in the target Yandex inbox or spam folder

## Local Run

If you run services locally, keep Docker only for infrastructure:

```bash
docker compose up -d postgres kafka
```

Then run the services locally in separate terminals:

```bash
./mvnw -f services/qa-service/pom.xml spring-boot:run
```

```bash
SPRING_MAIL_HOST=smtp.yandex.ru \
SPRING_MAIL_PORT=587 \
SPRING_MAIL_SMTP_AUTH=true \
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true \
SPRING_MAIL_SMTP_STARTTLS_REQUIRED=true \
SPRING_MAIL_USERNAME=your-mail@yandex.ru \
SPRING_MAIL_PASSWORD=your-app-password \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5430/postgres_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
QA_SERVICE_BASE_URL=http://localhost:8081 \
./mvnw -f services/core-service/pom.xml spring-boot:run
```

Why `QA_SERVICE_BASE_URL=http://localhost:8081` is required here:

- in Docker Compose, `core-service` reaches `qa-service` by the internal hostname `qa-service`
- in local runs, your host process must call `localhost:8081`

## SMTP for Local Development

- Docker Compose uses Yandex SMTP by default.
- `SPRING_MAIL_*` variables are used only by `notifications-service`.
- `notifications-service` health does not depend on SMTP availability.
- Use a Yandex app password, not the main account password.

### Login code email does not arrive

The flow is `core-service → Kafka → notifications-service → SMTP`. A `202` from `POST /api/users/login` only means the command was published to Kafka; the inbox depends on the rest of the chain.

1. **Fill `.env`** with `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` (Yandex app password). Empty values are common: Kafka succeeds but no email is sent.
2. **Run `notifications-service`** (`docker compose ps` / `docker compose logs notifications-service`). On startup it logs an **ERROR** if SMTP credentials are missing.
3. **Kafka address**: for JVM on the host, use `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092` (see compose port mapping). Wrong broker → consumer never reads messages.
4. **No mail server at all**: set `AUTH_LOGIN_ALLOW_WITHOUT_KAFKA=true` for `core-service` and read the OTP from **core-service** logs (development only; see `AuthService`).

## Tests

Run core-service tests:

```bash
./mvnw -f services/core-service/pom.xml test
```

Run qa-service tests:

```bash
./mvnw -f services/qa-service/pom.xml test
```

Run notifications-service tests:

```bash
./mvnw -f services/notifications-service/pom.xml test
```

## Repository Structure

- `services/core-service`
- `services/qa-service`
- `services/notifications-service`
- `contracts`
- `docker`

## Notes

- `services/core-service/src/main/resources/sql/init_tables.sql` is the single schema source for Docker init and tests.
- Notification delivery is asynchronous through Kafka.
- Q&A remains synchronous through internal HTTP because reads and immediate answers fit HTTP better than Kafka request-reply in this project.
