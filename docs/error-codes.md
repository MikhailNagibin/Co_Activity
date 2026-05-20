# Error codes catalog (core-service)

Этот документ — “контракт ошибок” между backend и frontend. Он отвечает на два вопроса:

1) **Какой единый формат ошибки возвращает API?**
2) **Какие `code` бывают, какой у них HTTP `status`, и когда они возникают?**

## 1) Единый формат ошибки (RFC7807 Problem Details)

`core-service` возвращает ошибки в формате **RFC7807** (`application/problem+json`) через Spring `ProblemDetail`.

Обязательные/стандартные поля RFC7807:

- `type` — URI типа ошибки (у нас: `urn:coactivity:error:<CODE>`)
- `title` — короткий заголовок (обычно reason phrase статуса, для валидации — `Validation failed`)
- `status` — HTTP status code (число)
- `detail` — человекочитаемое сообщение (English)
- `instance` — путь запроса (например `/api/rooms/123`)

Дополнительные top-level поля (custom properties):

- `code` — стабильный машинный код ошибки (UPPER_SNAKE_CASE)
- `timestamp` — время формирования ответа (`Instant`)
- `traceId` — request id для поддержки (и в заголовке `X-Request-Id`)
- `errors` — опционально, список структурированных ошибок (в основном для валидации)

### Пример

```json
{
  "type": "urn:coactivity:error:VALIDATION_FAILED",
  "title": "Validation failed",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/rooms/createRoom",
  "code": "VALIDATION_FAILED",
  "timestamp": "2026-04-13T12:34:56.789Z",
  "traceId": "3c3d5b5d-2a61-4f12-9b42-0d4c3b75f5a0",
  "errors": [
    { "field": "name", "message": "must not be blank", "code": "NotBlank" }
  ]
}
```

## 2) Где в коде описан/собирается этот формат

Источник формата — **handler’ы**, а не DTO:

- `GlobalExceptionHandler` — основной builder `ProblemDetail` для доменных ошибок и валидации
- `RestAuthenticationEntryPoint` — формат 401 (неаутентифицирован)
- `RestAccessDeniedHandler` — формат 403 (нет прав)

`code/status/errors` для “наших” бизнес-ошибок приходят из `DomainException`.

`traceId` задаётся фильтром `RequestIdFilter` (и отдаётся в `X-Request-Id`).

## 3) Error codes (таблица)

Ниже перечислены коды, которые реально используются в текущем коде `core-service`.

> Правило: **frontend должен ветвиться по `code`, а не по `detail`.**

### 3.1 Общие инфраструктурные коды

| code | status | Когда возникает |
|---|---:|---|
| `INTERNAL_ERROR` | 500 | Непредвиденная ошибка (catch-all `Exception`) |
| `AUTH_REQUIRED` | 401 | Не залогинен / нет сессии |
| `ACCESS_DENIED` | 403 | Нет прав (включая CSRF/Authorization) без более точного кода |
| `VALIDATION_FAILED` | 400 | Ошибка валидации/парсинга входных данных |
| `STORAGE_UNAVAILABLE` | 503 | Ошибка работы с хранилищем файлов (S3/local) |
| `NOTIFICATION_DELIVERY_FAILED` | 503 | Невозможно доставить уведомление (например email-код) |

### 3.2 Conflict (409)

| code | status | Когда возникает |
|---|---:|---|
| `ALREADY_ROOM_MEMBER` | 409 | Пользователь уже участник комнаты при добавлении |
| `USERNAME_ALREADY_TAKEN` | 409 | Имя пользователя занято |
| `EMAIL_ALREADY_REGISTERED` | 409 | Email уже зарегистрирован |
| `EMAIL_ALREADY_VERIFIED` | 409 | Email уже подтверждён |
| `USER_REGISTRATION_CONFLICT` | 409 | Конфликт регистрации пользователя |
| `OWNED_ROOMS_RESOLUTION_REQUIRED` | 409 | Нельзя удалить аккаунт без решения по комнатам, где пользователь owner |
| `INVALID_OWNERSHIP_TRANSFER` | 409 | Невалидная передача ownership |

### 3.3 Not Found (404)

| code | status | Когда возникает |
|---|---:|---|
| `ROOM_NOT_FOUND` | 404 | Комната не найдена |
| `ROOM_IMAGE_NOT_FOUND` | 404 | Картинка комнаты не найдена |
| `USER_NOT_FOUND` | 404 | Пользователь не найден |
| `QUESTION_NOT_FOUND` | 404 | Вопрос Q&A не найден |
| `ANSWER_NOT_FOUND` | 404 | Ответ Q&A не найден |
| `JOIN_REQUEST_NOT_FOUND` | 404 | Join request не найден |
| `REQUEST_NOT_FOUND` | 404 | RoomsRequest (заявка) не найдена |
| `PICTURE_NOT_FOUND` | 404 | Фото (picture) не найдено |
| `BULLETIN_BOARD_NOT_FOUND` | 404 | Bulletin board для комнаты отсутствует |
| `AVATAR_NOT_FOUND` | 404 | Аватар отсутствует |
| `AVATAR_METADATA_NOT_FOUND` | 404 | Метаданные аватара не найдены |
| `ROOM_MEMBERSHIP_NOT_FOUND` | 404 | “Membership” (пользователь не участник комнаты) не найден |
| `ROOM_BAN_NOT_FOUND` | 404 | Бан пользователя в комнате не найден |

### 3.4 Forbidden (403) — специализированные коды

| code | status | Когда возникает |
|---|---:|---|
| `USER_BANNED_FROM_ROOM` | 403 | Попытка добавить забаненного пользователя в комнату |
| `INVALID_VERIFICATION_CODE` | 403 | Неверный verification code |
| `VERIFICATION_CODE_EXPIRED` | 403 | Verification code просрочен/отсутствует |
| `INVALID_PASSWORD_RESET_CODE` | 403 | Неверный password reset code |
| `PASSWORD_RESET_CODE_EXPIRED` | 403 | Password reset code просрочен/отсутствует |
| `EMAIL_NOT_VERIFIED` | 403 | Логин/действие запрещены, email не подтвержден |
| `INVALID_CREDENTIALS` | 403 | Неверная почта или пароль при логине |
| `ACCOUNT_DISABLED` | 403 | Аккаунт отключён/заблокирован (логин запрещён) |
| `AUTHENTICATION_FAILED` | 403 | Иная ошибка аутентификации (невалидное состояние провайдера) |

### 3.5 Too Many Requests (429)

| code | status | Когда возникает |
|---|---:|---|
| `REGISTRATION_CODE_RESEND_COOLDOWN` | 429 | Слишком частая повторная отправка verification code |

## 4) Как добавлять новый code правильно (короткий чеклист)

1) Выбери **стабильный** `code` (UPPER_SNAKE_CASE), который не зависит от текста.
2) Выбери правильный `HttpStatus` (на уровне `DomainException`/наследника).
3) Текст в `detail` делай человекочитаемым, но не используй его как “ключ” логики.
4) Если это валидация конкретного поля — лучше добавить `errors[]`, а не лепить строку в `detail`.

