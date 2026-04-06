# Backend Services

В каталоге `services` лежат активные backend-модули:

- `core-service` — основной HTTP API, auth, users, rooms, join requests, Q&A
- `notifications-service` — Kafka consumer для email-уведомлений

## Как они связаны

Схема работы:

```text
client -> core-service
core-service -> Kafka -> notifications-service -> SMTP
```

Важно:

- `core-service` — единственный публичный backend
- `notifications-service` не используется как business API напрямую
- auth работает через Spring Security + Spring Session + Redis
- JWT больше не используется

## Локальный запуск сервисов

Сначала подними инфраструктуру:

```bash
docker compose up -d postgres kafka redis
```

Потом в каждой новой shell-сессии загрузи `.env`:

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
```

### `core-service`

```bash
./mvnw -f services/core-service/pom.xml spring-boot:run
```

Порт:

- `http://localhost:8080`

Зависимости:

- PostgreSQL
- Redis
- Kafka

### `notifications-service`

```bash
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

Порт:

- `http://localhost:8082`

Зависимости:

- Kafka
- SMTP credentials в `.env`, если нужна реальная отправка email

## Тесты

```bash
./mvnw -f services/core-service/pom.xml test
./mvnw -f services/notifications-service/pom.xml test
```

Docker integration tests для `core-service`:

```bash
./mvnw -f services/core-service/pom.xml -Pwith-docker-tests test
```
