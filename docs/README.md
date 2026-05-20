# Документация (точки входа)

Цель: быстро находить нужные документы для **frontend** и **DevOps**.

## Для frontend (HTTP API)

- API изменения / release notes: `docs/api-update.md`
- Контракт ошибок (HTTP Problem Details + codes): `docs/error-codes.md`
- Auth/CSRF/session контракт: `contracts/auth-spec.md`

## Для DevOps (запуск, окружения, эксплуатация)

- Главная инструкция запуска (docker compose, env, профили): `README.md`
- Backend services (как связаны сервисы, порты, команды): `services/README.md`

## Spring profiles (local / prod / test)

Профиль выбирается через переменную окружения `SPRING_PROFILES_ACTIVE`.
Если переменная не задана, для `core-service` по умолчанию используется `local` (см. `spring.profiles.default`).

### `local` (docker-compose.yml)

- `core-service`
  - storage: `app.storage.type=local` (файлы в `./var/app-storage` внутри окружения)
  - cookie session: `SESSION_COOKIE_SECURE=false` (подходит для HTTP localhost)
  - логирование: `com.coactivity=DEBUG`, `org.springframework=DEBUG`
  - health details: `management.endpoint.health.show-details=always`
- `notifications-service`
  - SMTP: Mailpit (`SPRING_MAIL_HOST=mailpit`, порт `1025`)
  - health details обычно `always`

### `prod` (docker-compose.prod.yml / production-like)

- `core-service`
  - storage: `app.storage.type=s3` (MinIO/S3 через `APP_STORAGE_S3_*`)
  - cookie session: `SESSION_COOKIE_SECURE=true` (для HTTPS окружений)
  - логирование: `org.springframework=WARN`, `com.coactivity=INFO`
  - health details: `management.endpoint.health.show-details=never`
- `notifications-service`
  - SMTP: Yandex SMTP по умолчанию (через `SPRING_MAIL_*`)
  - health details обычно `never`

### `test` (опционально)

Профиль `test` добавлен для **ручного** запуска приложения в “тестовом режиме”.
Автотесты `core-service` используют конфиг из `services/core-service/src/test/resources/application.yml` и не зависят от `SPRING_PROFILES_ACTIVE`, если ты специально его не задашь.

## Межсервисные контракты

- Auth contract (HTTP): `contracts/auth-spec.md`
- Notifications Kafka contract: `contracts/notifications-kafka-spec.md`

## Product / scope

- User stories: `docs/user-stories.md`
