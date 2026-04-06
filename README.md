# Co_Activity

Co_Activity — приложение для комнат, заявок на вступление, Q&A и email-уведомлений.

Сейчас репозиторий устроен так:

- `services/core-service` — основной backend и единственный публичный HTTP API
- `services/notifications-service` — Kafka consumer для отправки email
- `frontend/web` — активный React frontend
- `frontend/legacy` — старые статические макеты, не рабочий runtime

Q&A уже встроен в `core-service`. Отдельного `qa-service` больше нет.

## Стек

- Java 21
- Spring Boot 3
- PostgreSQL 16
- Redis 7
- Kafka
- React 19 + Vite

## Какой запуск использовать

Нормальный вариант для разработки:

1. backend через Docker Compose
2. frontend локально через Vite

Это самый простой и наименее ломкий режим. Он покрывает почти весь реальный runtime и не заставляет вручную поднимать PostgreSQL, Redis и Kafka.

## Что нужно заранее

- Docker Desktop с `docker compose`
- Java 21
- Node.js 20+
- `npm`

Проверка:

```bash
docker --version
docker compose version
java -version
node -v
npm -v
```

Maven отдельно ставить не нужно. В проекте есть `./mvnw`.

## Быстрый запуск приложения

### 1. Подготовить `.env`

```bash
cp .env.example .env
```

Минимально важные переменные:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD` — PostgreSQL в Docker Compose
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092` — Kafka с хоста
- `SPRING_DATA_REDIS_HOST=localhost`, `SPRING_DATA_REDIS_PORT=6379` — Redis для Spring Session

Для стандартного Docker Compose из этого репозитория `SPRING_DATA_REDIS_PASSWORD` не нужен.
Redis поднимается без auth. Указывай пароль только если сам подключаешь приложение к внешнему Redis с включённой аутентификацией.

Если хочешь рабочую регистрацию с подтверждением по email, нужно ещё заполнить:

- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

Без SMTP backend поднимется, но отправка verification code и email-уведомлений работать не будет.

### 2. Поднять backend

Из корня проекта:

```bash
docker compose up --build -d
```

Что стартует:

- `postgres`
- `kafka`
- `redis`
- `core-service`
- `notifications-service`

Если `core-service` не стартует на миграции `V2__session_auth_upgrade.sql` с ошибкой про duplicate `email_normalized`, проблема в старом локальном volume `postgres_data`, а не в Redis.
Это значит, что в существующей таблице `users` уже есть несколько строк с одним и тем же email, и новая session-based auth модель корректно запрещает такие данные.
Для локальной разработки в таком случае нормально сделать полный reset данных:

```bash
docker compose down -v
docker compose up --build -d
```

Делай это только локально, если старые данные не нужны. В реальной среде такие дубликаты надо сначала разбирать и чистить явно.

### 3. Проверить backend

```bash
curl http://localhost:8080/actuator/health
docker compose ps
```

Ожидаемый результат:

- `core-service` отвечает на `http://localhost:8080`
- `actuator/health` возвращает `200`

### 4. Запустить frontend

```bash
cd frontend/web
npm ci
npm run dev
```

Открыть:

```text
http://localhost:5173
```

### 5. Что получится в итоге

- frontend: `http://localhost:5173`
- backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5430`
- Kafka: `localhost:29092`
- Redis: `localhost:6379`

## Как auth работает сейчас

Проект больше не использует JWT.

Текущая схема:

- HTTP session через Spring Security + Spring Session
- сессии хранятся в Redis
- cookie сессии: `COACTIVITY_SESSION`
- CSRF cookie: `XSRF-TOKEN`

Для браузера это значит:

- frontend не должен хранить access token в `localStorage`
- frontend работает через cookie-based session
- mutating запросы требуют CSRF

Локально frontend уже настроен правильно: Vite proxy отправляет `/api/*` на `http://localhost:8080`.

## Полезные команды

Логи:

```bash
docker compose logs -f core-service
docker compose logs -f notifications-service
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f kafka
```

Остановить backend:

```bash
docker compose down
```

Полностью сбросить данные и поднять заново:

```bash
docker compose down -v
docker compose up --build -d
```

## Проверка backend вручную

Получить CSRF:

```bash
curl -c /tmp/coactivity.cookies http://localhost:8080/api/auth/csrf
```

Проверить, что без логина защищённый endpoint не пускает:

```bash
curl -b /tmp/coactivity.cookies http://localhost:8080/api/auth/me
```

Ожидаемо будет `401 Unauthorized`.

## Локальный запуск сервисов без Docker backend

Этот режим нужен, если хочешь дебажить Java-код в IDE.

### 1. Поднять только инфраструктуру

```bash
docker compose up -d postgres kafka redis
```

### 2. Загрузить `.env` в shell

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
```

`source` загружает переменные в текущую shell-сессию.  
`set -a` делает их экспортируемыми для Java-процессов.

### 3. Запустить `notifications-service`

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
./mvnw -f services/notifications-service/pom.xml spring-boot:run
```

Порт:

- `http://localhost:8082`

### 4. Запустить `core-service`

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity
set -a
source .env
set +a
./mvnw -f services/core-service/pom.xml spring-boot:run
```

Порт:

- `http://localhost:8080`

### 5. Запустить frontend

```bash
cd /Users/bomnik/IdeaProjects/Co_Activity/frontend/web
npm ci
npm run dev
```

## Тесты

Backend:

```bash
./mvnw -f services/core-service/pom.xml test
./mvnw -f services/notifications-service/pom.xml test
```

Frontend:

```bash
cd frontend/web
npm run lint
npm run build
```

Docker-tagged integration tests:

```bash
./mvnw -f services/core-service/pom.xml -Pwith-docker-tests test
```

Эти тесты требуют рабочий Docker daemon. Если Docker socket недоступен, они упадут ещё до старта Spring context.

## Частые проблемы

### `core-service` не стартует

Проверь:

- поднят ли `postgres`
- поднят ли `redis`
- совпадают ли значения в `.env` и `docker-compose.yml`

### Регистрация не работает

Проверь:

- поднят ли `notifications-service`
- работает ли Kafka
- заданы ли `SPRING_MAIL_USERNAME` и `SPRING_MAIL_PASSWORD`

Важно: регистрация сейчас зависит от отправки verification code по email.

### Frontend не видит backend

Проверь:

- backend слушает `localhost:8080`
- frontend запущен из `frontend/web`
- Vite proxy не менялся в [vite.config.js](/Users/bomnik/IdeaProjects/Co_Activity/frontend/web/vite.config.js)

## Куда смотреть дальше

- backend сервисы: [services/README.md](/Users/bomnik/IdeaProjects/Co_Activity/services/README.md)
- frontend структура: [frontend/README.md](/Users/bomnik/IdeaProjects/Co_Activity/frontend/README.md)
- frontend app: [frontend/web/README.md](/Users/bomnik/IdeaProjects/Co_Activity/frontend/web/README.md)
- auth контракт: [contracts/auth-spec.md](/Users/bomnik/IdeaProjects/Co_Activity/contracts/auth-spec.md)
