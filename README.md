# Co_Activity

Co_Activity is a microservice-based application for users, rooms, join requests, Q&A, and notification delivery.

This repository contains:

- backend services on Java 21 + Spring Boot
- PostgreSQL for persistence
- Kafka for asynchronous notification delivery
- an active frontend app on React + Vite

Yes: the backend can be started with Docker Compose. That is the main and simplest run mode for this project.

## Stack

- Backend: Java 21, Spring Boot, Maven Wrapper
- Database: PostgreSQL 16
- Messaging: Kafka
- Frontend: React 19, Vite
- Mail delivery: Yandex SMTP through `notifications-service`

## Runtime Architecture

- `core-service` is the only public HTTP API
- `qa-service` is an internal HTTP microservice
- `notifications-service` is a Kafka consumer for email delivery
- `core-service` talks to `qa-service` over HTTP
- `core-service` publishes email commands to Kafka
- `notifications-service` consumes those commands and sends email

Canonical runtime flow:

```text
browser/client -> core-service -> qa-service
browser/client -> core-service -> Kafka -> notifications-service -> SMTP
```

Important constraints:

- there is no monolith fallback
- `notifications-service` is not a public business API
- frontend Q&A calls still go through `core-service`, not directly to `qa-service`

## Repository Layout

- `frontend/web` - active frontend application
- `frontend/legacy` - old static prototype, not the active runtime
- `services/core-service` - public backend API
- `services/qa-service` - internal Q&A service
- `services/notifications-service` - Kafka email consumer
- `contracts` - API and event contracts
- `docker` - database init files
- `scripts` - helper scripts such as SMTP smoke tests

## Choose a Run Mode

### 1. Docker Compose backend

Use this when you want the fastest reliable start.

What runs in Docker:

- PostgreSQL
- Kafka
- `qa-service`
- `notifications-service`
- `core-service`

What usually runs locally:

- `frontend/web`

### 2. Local Java services + Docker infrastructure

Use this when you want to debug Spring Boot services in your IDE or terminal.

What runs in Docker:

- PostgreSQL
- Kafka

What runs locally:

- `qa-service`
- `notifications-service`
- `core-service`
- optionally `frontend/web`

## Prerequisites

### For Docker Compose backend

- Docker Desktop with `docker compose`
- Node.js 20+ and `npm` if you want to run the frontend locally

### For local Java services

- Java 21
- Docker Desktop with `docker compose`
- Node.js 20+ and `npm` for the frontend

You do not need a separate Maven installation. Use the wrapper included in the repo:

```bash
./mvnw -v
```

Quick checks:

```bash
docker --version
docker compose version
java -version
node -v
npm -v
```

## Environment Setup

Create a local environment file:

```bash
cp .env.example .env
```

Generate a JWT secret and put it into `JWT_SECRET_BASE64` in `.env`:

```bash
openssl rand -base64 32
```

Important variables in `.env`:

- `JWT_SECRET_BASE64` is required for `core-service` and `qa-service`
- `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` are needed only if you want real email delivery
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092` is correct for local access to Kafka from the host
- `QA_SERVICE_BASE_URL=http://qa-service:8081` is correct for Docker Compose, but wrong for a locally started `core-service`

If you run `core-service` locally, override this variable:

```bash
QA_SERVICE_BASE_URL=http://localhost:8081
```

## Run Backend with Docker Compose

This is the recommended backend startup path.

1. Prepare `.env` as described above.

2. Start the backend:

```bash
docker compose up --build -d
```

3. Check that the public API is up:

```bash
curl http://localhost:8080/actuator/health
docker compose ps
```

4. View logs when needed:

```bash
docker compose logs -f core-service
docker compose logs -f qa-service
docker compose logs -f notifications-service
docker compose logs -f kafka
docker compose logs -f postgres
```

5. Stop everything:

```bash
docker compose down
```

If you changed the SQL schema and need PostgreSQL init scripts to run again:

```bash
docker compose down -v
docker compose up --build -d
```

### Published Ports in Docker Mode

- `core-service`: `localhost:8080`
- PostgreSQL: `localhost:5430`
- Kafka: `localhost:29092`

Notes:

- `qa-service` and `notifications-service` are internal in Docker Compose
- only `core-service` is exposed as the public backend API

## Run Backend Locally with `.env`

This mode is useful for debugging Java code.

### 1. Start infrastructure only

```bash
docker compose up -d postgres kafka
```

### 2. Load `.env` into each terminal session

Before starting a service, export variables from `.env`:

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
```

`source` loads variables into the current shell. `set -a` marks them for export so Java processes can read them.

### 3. Start `qa-service`

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
./mvnw -f services/qa-service/pom.xml spring-boot:run
```

Local port:

- `http://localhost:8081`

### 4. Start `notifications-service`

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

Local port:

- `http://localhost:8082`

Notes:

- email delivery works only if SMTP credentials in `.env` are valid
- the service can still start even if SMTP is not configured correctly, but real email sending will fail

### 5. Start `core-service`

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
export QA_SERVICE_BASE_URL=http://localhost:8081
./mvnw -f services/core-service/pom.xml spring-boot:run
```

Public local port:

- `http://localhost:8080`

Why the extra `export QA_SERVICE_BASE_URL=...` is required:

- in Docker Compose, `core-service` reaches `qa-service` by container hostname `qa-service`
- when both processes run on your machine, `core-service` must call `localhost:8081`

## Run the Frontend

The active frontend is in `frontend/web`.

Default backend URL in the frontend:

```text
http://localhost:8080/api
```

That default already matches:

- backend started through Docker Compose
- backend started locally on port `8080`

Install and start the frontend:

```bash
cd frontend/web
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

If you want to override the backend URL:

```bash
cd frontend/web
cp .env.example .env
```

Example:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## Recommended Development Setups

### Simplest setup

- backend: Docker Compose
- frontend: local Vite dev server

Commands:

```bash
docker compose up --build -d
cd frontend/web
npm ci
npm run dev
```

### Debug-friendly setup

- PostgreSQL and Kafka: Docker
- Java services: local
- frontend: local

Commands:

```bash
docker compose up -d postgres kafka
```

Then start the three Spring Boot services in separate terminals as described above.

## Quick Smoke Checks

Only `core-service` should be used from the browser or external tools.

Basic checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/rooms
curl http://localhost:8080/api/qa/questions
```

Frontend runtime contract:

- browser -> `http://localhost:5173`
- frontend -> `http://localhost:8080/api`
- `core-service` -> `http://localhost:8081` when running locally

## SMTP Smoke Test

Use this only when the backend is running through Docker Compose and you want to test real email delivery.

Before the test:

- enable IMAP/SMTP access in Yandex Mail settings
- create a Yandex app password for Mail
- fill `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` in `.env`

Run:

```bash
./scripts/yandex-smoke-test.sh
```

Expected result:

- `/api/users/login` returns `202 Accepted`
- `notifications-service` confirms delivery in logs
- the email appears in the target inbox or spam folder

## Tests

Run backend tests per service:

```bash
./mvnw -f services/core-service/pom.xml test
./mvnw -f services/qa-service/pom.xml test
./mvnw -f services/notifications-service/pom.xml test
```

## Troubleshooting

### `JWT_SECRET_BASE64` is missing

Symptom:

- `core-service` or `qa-service` fails during startup

Fix:

- generate a secret with `openssl rand -base64 32`
- put it into `.env`
- if you start services locally, make sure you actually exported `.env` in that terminal

### `core-service` cannot call `qa-service` in local mode

Symptom:

- `/api/qa/*` calls fail
- `core-service` tries to call `http://qa-service:8081`

Fix:

```bash
export QA_SERVICE_BASE_URL=http://localhost:8081
```

### Database schema changes are not visible in Docker mode

Symptom:

- PostgreSQL starts with old schema

Fix:

```bash
docker compose down -v
docker compose up --build -d
```

### Email is not sent

Common causes:

- wrong Yandex app password
- SMTP access is disabled in Yandex settings
- Kafka or `notifications-service` is not running

## Notes

- `services/core-service/src/main/resources/sql/init_tables.sql` is the main schema source used for Docker database initialization
- notification delivery is asynchronous through Kafka
- Q&A remains synchronous over internal HTTP because this project uses immediate request-response semantics there
