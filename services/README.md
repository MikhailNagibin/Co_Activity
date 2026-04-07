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
- Kafka topic'и для email-контракта в production создаются инфраструктурой или отдельным bootstrap job до старта сервисов
- корневой `docker-compose.yml` нужен для local/dev, а не как production deployment config
- `notifications-service` использует профиль `local` для Mailpit и профиль `prod` для реального SMTP
- для dockerized Yandex SMTP запуска используй `docker-compose.prod.yml` и `.env.prod`
- cookie сессии `COACTIVITY_SESSION` использует env-политику `SESSION_COOKIE_SECURE` (Option C): `false` для local/dev HTTP, `true` для HTTPS-окружений

## Локальный запуск сервисов

Сначала подними инфраструктуру:

```bash
docker compose --env-file .env.local -f docker-compose.yml up -d postgres kafka redis kafka-topics-init
```

Это вызовет `scripts/create-kafka-topics.sh` и создаст `notifications.email.v1` и DLT topic до старта Java-сервисов.
Через compose `notifications-service` всегда стартует в профиле `local` и шлёт почту в Mailpit.

Для production-like compose без Mailpit:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

При необходимости topic bootstrap можно повторно запустить вручную:

```bash
docker compose --env-file .env.local -f docker-compose.yml run --rm kafka-topics-init
```

Если запускаешь сервисы из IDE или через `mvnw`, загрузи нужный env-файл в shell.

Для local/dev:

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env.local
set +a
```

Для production-like с Yandex SMTP:

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env.prod
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
- SMTP credentials в `.env.prod`, если нужна реальная отправка email
- `NOTIFICATIONS_MAIL_FROM`, если у SMTP provider sender address должен отличаться от логина

Monitoring:

- `/actuator/health` на `notifications-service` включает SMTP health и проверку основного/DLT topic'ов
- `/actuator/metrics/coactivity.notifications.email.commands` показывает delivery outcomes
- `/actuator/metrics/coactivity.notifications.email.dlt.published` показывает публикации в DLT

Если запускать `notifications-service` без Compose, topic'и в Kafka должны уже существовать. Сервис больше не должен неявно создавать production topic'и через Spring runtime.

Для Yandex SMTP запускай `notifications-service` отдельно с переменными из `.env.prod` и заполненными `SPRING_MAIL_*` / `NOTIFICATIONS_MAIL_FROM`.

## Тесты

```bash
./mvnw -f services/core-service/pom.xml test
./mvnw -f services/notifications-service/pom.xml test
```

Docker integration tests для `core-service`:

```bash
./mvnw -f services/core-service/pom.xml -Pwith-docker-tests test
```
