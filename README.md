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

### 1. Подготовить env-файлы

```bash
cp .env.example .env.local
cp .env.prod.example .env.prod
```

После этого:

- редактируй `.env.local` для local/dev
- редактируй `.env.prod` для production-like запуска через Yandex SMTP
- `.env` больше не нужен для Docker Compose запуска

Минимально важные переменные:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD` — PostgreSQL в Docker Compose
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092` — Kafka с хоста
- `NOTIFICATIONS_KAFKA_TOPIC` и `NOTIFICATIONS_KAFKA_DLT_TOPIC` — имена Kafka topic'ов для email-контракта
- `SPRING_DATA_REDIS_HOST=localhost`, `SPRING_DATA_REDIS_PORT=6379` — Redis для Spring Session
- `SPRING_PROFILES_ACTIVE=local` — локальный профиль `notifications-service` для Mailpit

Для стандартного Docker Compose из этого репозитория `SPRING_DATA_REDIS_PASSWORD` не нужен.
Redis поднимается без auth. Указывай пароль только если сам подключаешь приложение к внешнему Redis с включённой аутентификацией.

Если хочешь рабочую регистрацию с подтверждением по email в production-профиле, нужно ещё заполнить:

- `SPRING_PROFILES_ACTIVE=prod`
- `NOTIFICATIONS_MAIL_FROM` или fallback на `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

Без SMTP backend поднимется, но отправка verification code и email-уведомлений работать не будет.

Важно для Kafka: в production topic'и `notifications.email.v1` и соответствующий DLT должны существовать до старта приложений. В локальном Docker Compose из этого репозитория они создаются отдельным bootstrap-сервисом, который запускает `scripts/create-kafka-topics.sh`, а не самим Spring runtime.

`docker-compose.yml` в этом репозитории предназначен для local/dev. Он всегда поднимает `notifications-service` в профиле `local` и направляет почту в `mailpit`.

Для production-like запуска через Docker Compose используй `docker-compose.prod.yml`. Он не поднимает `mailpit` и читает реальные SMTP-настройки из `.env.prod`.

### 2. Поднять backend

Из корня проекта:

```bash
docker compose --env-file .env.local -f docker-compose.yml up --build -d
```

Production-like запуск с Yandex SMTP:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

Что стартует:

- `postgres`
- `kafka`
- `kafka-topics-init`
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

Для `notifications-service` также полезно проверить:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8082/actuator/metrics/coactivity.notifications.email.dlt.published
```

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

Kafka topic'и для email-контракта создаются заранее bootstrap-сервисом через `scripts/create-kafka-topics.sh`:

- `notifications.email.v1`
- `notifications.email.v1.dlt` или topic из `NOTIFICATIONS_KAFKA_DLT_TOPIC`

## Какие файлы использовать

- `docker-compose.yml` + `.env.local` -> local/dev, Mailpit
- `docker-compose.prod.yml` + `.env.prod` -> production-like запуск, Yandex SMTP

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
docker compose --env-file .env.local -f docker-compose.yml logs -f core-service
docker compose --env-file .env.local -f docker-compose.yml logs -f notifications-service
docker compose --env-file .env.local -f docker-compose.yml logs -f postgres
docker compose --env-file .env.local -f docker-compose.yml logs -f redis
docker compose --env-file .env.local -f docker-compose.yml logs -f kafka
```

Остановить backend:

```bash
docker compose --env-file .env.local -f docker-compose.yml down
```

Полностью сбросить данные и поднять заново:

```bash
docker compose --env-file .env.local -f docker-compose.yml down -v
docker compose --env-file .env.local -f docker-compose.yml up --build -d
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
docker compose up -d postgres kafka redis kafka-topics-init
```

Если `kafka-topics-init` не запускать, `notifications-service` теперь падает fail-fast при отсутствии topic'ов вместо тихого старта в некорректной конфигурации.

Повторно прогнать bootstrap topic'ов вручную можно так:

```bash
docker compose run --rm kafka-topics-init
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

Если используешь `.env.example` без изменений, `notifications-service` пойдёт в локальный Mailpit на `localhost:1025`.

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

Полезные actuator endpoint'ы:

- `/actuator/health` — включает SMTP health и проверку наличия основного/DLT topic'ов
- `/actuator/metrics/coactivity.notifications.email.commands` — доставка, ошибки и invalid payload
- `/actuator/metrics/coactivity.notifications.email.dlt.published` — публикации в DLT

### Использование Yandex SMTP

Для production или smoke-теста с реальным SMTP:

1. Установи `SPRING_PROFILES_ACTIVE=prod`
2. Заполни `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` и при необходимости `NOTIFICATIONS_MAIL_FROM`
3. Оставь `SPRING_MAIL_HOST=smtp.yandex.ru` и `SPRING_MAIL_PORT=587` либо переопредели их явно

Локальный `docker compose` для этого не предназначен: он жёстко использует `mailpit`. Для проверки Yandex SMTP запускай `notifications-service` отдельно через `mvnw` с `prod` профилем и уже созданными Kafka topic'ами.

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
- созданы ли `NOTIFICATIONS_KAFKA_TOPIC` и DLT topic
- задан ли `NOTIFICATIONS_MAIL_FROM`, если SMTP provider требует отдельный sender address
- заданы ли `SPRING_MAIL_USERNAME` и `SPRING_MAIL_PASSWORD`

Важно: регистрация сейчас зависит от отправки verification code по email.

### `notifications-service` не стартует с ошибкой про missing topics

Проверь:

- запускался ли `kafka-topics-init` в Docker Compose или вручную через `docker compose run --rm kafka-topics-init`
- существуют ли `NOTIFICATIONS_KAFKA_TOPIC` и DLT topic во внешнем Kafka-кластере
- совпадают ли имена topic'ов в `.env`, `docker-compose.yml` и целевом окружении

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
