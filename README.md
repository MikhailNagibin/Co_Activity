# Co_Activity

Backend monorepo on Java 21 + Spring Boot + PostgreSQL + Kafka.

Current local architecture:

- `core-service` handles main business logic.
- `qa-service` serves Q&A API over HTTP.
- `notifications-service` consumes Kafka email commands and sends SMTP messages.
- Notification flow is fixed: `core-service -> Kafka -> notifications-service -> SMTP`.

## Prerequisites

- Docker Desktop with `docker compose`
- Java 21 for local runs
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

2. Start the full stack:

```bash
docker compose up --build -d
```

3. Check health:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

4. Open Mailpit UI for local email smoke tests:

```text
http://localhost:8025
```

5. View logs:

```bash
docker compose logs -f core-service
docker compose logs -f qa-service
docker compose logs -f notifications-service
docker compose logs -f kafka
docker compose logs -f postgres
```

6. Stop:

```bash
docker compose down
```

If you changed the SQL schema, recreate PostgreSQL volume so init scripts run again:

```bash
docker compose down -v
docker compose up --build -d
```

## Local Run

Start infrastructure only:

```bash
docker compose up -d postgres kafka qa-service notifications-service
```

Run `core-service` locally:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5430/postgres_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
QA_MODE=SERVICE \
QA_SERVICE_BASE_URL=http://localhost:8081 \
./mvnw -f services/core-service/pom.xml spring-boot:run
```

Run services independently:

```bash
./mvnw -f services/core-service/pom.xml spring-boot:run
./mvnw -f services/qa-service/pom.xml spring-boot:run
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

## SMTP for Local Development

- Docker Compose uses `Mailpit` by default, so local email smoke tests do not depend on Gmail.
- Local SMTP server is available on `localhost:1025`, and Mailpit UI is available on `localhost:8025`.
- `SPRING_MAIL_*` variables are used only by `notifications-service`.
- To switch from Mailpit to real Gmail, override `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`,
  `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_SMTP_STARTTLS_ENABLE`,
  `SPRING_MAIL_SMTP_STARTTLS_REQUIRED`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD`.
- For Gmail you need an app password, not your normal account password.
- `notifications-service` health no longer depends on SMTP availability, so `/actuator/health` stays useful for local diagnostics.
- Do not store a real Gmail app password in `.env`. Use one-off shell environment variables instead.

Manual email endpoint for smoke tests:

```text
POST /api/notifications/email
```

Local smoke test flow:

1. Send `POST http://localhost:8082/api/notifications/email`
2. Open `http://localhost:8025`
3. Confirm the message appears in Mailpit

### Real Gmail Smoke Test

Preferred path:

```bash
./scripts/gmail-smoke-test.sh
```

What the script does:

1. Prompts for a Gmail sender and app password without writing them to repo files
2. Recreates only `notifications-service` with Gmail SMTP settings
3. Stops `mailpit` to avoid false positives
4. Checks `http://localhost:8082/actuator/health`
5. Sends `POST /api/notifications/email`
6. Shows recent `notifications-service` logs

Why the script is safer than a raw `curl` right after recreate:

- It waits until `notifications-service` really answers `/actuator/health`
- This avoids false failures like `curl: (56) Recv failure: Connection reset by peer` while Spring Boot is still starting

Manual equivalent:

```bash
export SPRING_MAIL_HOST=smtp.gmail.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_SMTP_AUTH=true
export SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
export SPRING_MAIL_SMTP_STARTTLS_REQUIRED=true
export SPRING_MAIL_USERNAME='your@gmail.com'
read -s "SPRING_MAIL_PASSWORD?Gmail app password: "
export SPRING_MAIL_PASSWORD

docker compose up --build -d --no-deps notifications-service
docker compose stop mailpit

curl -i http://localhost:8082/actuator/health

curl -i -X POST http://localhost:8082/api/notifications/email \
  -H 'Content-Type: application/json' \
  -d '{
    "to": "your@gmail.com",
    "subject": "CoActivity Gmail smoke test",
    "body": "This is a direct SMTP smoke test from notifications-service"
  }'

docker compose logs notifications-service --tail=100
```

Rollback to Mailpit:

```bash
unset SPRING_MAIL_HOST
unset SPRING_MAIL_PORT
unset SPRING_MAIL_SMTP_AUTH
unset SPRING_MAIL_SMTP_STARTTLS_ENABLE
unset SPRING_MAIL_SMTP_STARTTLS_REQUIRED
unset SPRING_MAIL_USERNAME
unset SPRING_MAIL_PASSWORD

docker compose up -d mailpit
docker compose up -d --no-deps notifications-service
```

Temporary simple path through `.env`:

1. Put Gmail values into `.env`
2. Run `docker compose up --build -d --no-deps notifications-service`
3. Run the same `curl` test from above
4. Remove Gmail password from `.env` after the test

This is simpler, but weaker from a security standpoint because the secret lives in a file on disk.

Expected results:

- `204` from `POST /api/notifications/email` and a real message in Gmail means SMTP works.
- `500` with `AuthenticationFailedException` or `535-5.7.8` means the password is wrong or is not an app password.
- `500` with `MailConnectException`, timeout, or `Connection refused` means the container cannot reach `smtp.gmail.com:587`.
- `200` on `/actuator/health` does not prove Gmail works because mail health is intentionally disabled.

## Tests

Run core-service unit and integration tests:

```bash
./mvnw -f services/core-service/pom.xml test
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
- There is no outbox flow anymore. If Kafka is unavailable, business changes in `core-service` still commit, but notification delivery can be lost in this version.
