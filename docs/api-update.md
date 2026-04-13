# API Update для фронта (backend)

Период изменений: **после коммита `5114d11` (2026-04-05)**.

Цель документа: быстро объяснить фронтенду и ИИ‑агентам, **что именно изменилось в API** (только изменения, не полный список всего API).

---

## 1) Краткое описание релиза

### Главное (важно для интеграции)

1) **Аутентификация стала session-based** (Spring Security + HttpSession).
   - Вместо `Authorization: Bearer ...` теперь используется cookie‑сессия **`COACTIVITY_SESSION`**.
   - Многие endpoints **больше не принимают** `Authorization` header.

2) **CSRF включён для “небезопасных” методов** (`POST/PUT/DELETE/...`).
   - CSRF cookie: `XSRF-TOKEN`
   - CSRF header: `X-XSRF-TOKEN`
   - Endpoint для получения токена: `GET /api/auth/csrf`

3) Добавлены крупные фичи API:
   - **Аватары пользователей** (GET/PUT/DELETE).
   - **Изображения комнат** (upload/delete + публичный download по `GET /api/rooms/{roomId}/images/{imageId}`).
   - **Email notification settings**: добавлен `GET`, и новое поле `importantRoomUpdates`.
   - **Password reset**: request/verify/confirm (все публичные).
   - **Room lifecycle & governance**: update room, bans, remove participant, ownership transfer, membership status.
   - **Q&A**: поиск по `query`, edit/delete для вопросов и ответов.

### Публичные (без сессии) GET endpoints

См. `SecurityConfig`:
- `GET /api/users/{userId}/avatar`
- `GET /api/rooms/{roomId}/images/{imageId}`
- `GET /api/rooms` и `GET /api/rooms/{roomId}`
- `GET /api/qa/questions` (+ `/category`, `/{questionId}`)

---

## 2) Новые endpoints по блокам

### Auth (`/api/auth`) — новый контроллер

**Новые/заменяющие старые auth endpoints:**

- `POST /api/auth/register`
  - Регистрирует пользователя и инициирует email verification code.
  - **Заменяет** старый `POST /api/users` (который раньше был registration).

- `POST /api/auth/register/verify`
  - Подтверждение email по коду.

- `POST /api/auth/register/resend`
  - Повторная отправка verification code (с rate limit).

- `POST /api/auth/login`
  - Логин по `email+password`.
  - **Ставит** cookie‑сессию `COACTIVITY_SESSION` (session-based auth).
  - **Заменяет** старую схему `POST /api/users/login` + `POST /api/users/login/verify`.

- `POST /api/auth/logout`
  - Завершает текущую сессию и чистит cookie `COACTIVITY_SESSION`.

- `GET /api/auth/me`
  - Текущий залогиненный пользователь (профиль) по активной сессии.

- `GET /api/auth/csrf`
  - Возвращает CSRF token и имена header/parameter для SPA.

**Password management:**

- `POST /api/auth/password/change`
  - Смена пароля (требует сессию).
  - **Заменяет** старый `PUT /api/users/me/password` (который раньше принимал query params).

**Password reset (публичные):**

- `POST /api/auth/password/reset/request`
- `POST /api/auth/password/reset/verify`
- `POST /api/auth/password/reset/confirm`

Нюанс: `request` может возвращать `204` даже если email не найден/неактивен — чтобы не “сливать” существование аккаунта.

---

### Users (`/api/users`)

#### Аватары пользователей (новое)

- `GET /api/users/{userId}/avatar` (public)
  - Возвращает **байты картинки** (не JSON).
  - `Content-Type` соответствует реальному типу (например `image/png`).

- `PUT /api/users/me/avatar` (auth + CSRF, `multipart/form-data`)
  - Part name: `file`
  - Возвращает обновлённый `UserProfileResponse` (JSON).

- `DELETE /api/users/me/avatar` (auth + CSRF)
  - `204 No Content`

#### Email notification settings (изменено + новое)

- `GET /api/users/me/notifications` (auth)
  - Возвращает `NotificationSettingsResponse`.

- `PUT /api/users/me/notifications` (auth + CSRF)
  - Принимает `NotificationSettingsRequest`.
  - **Добавлено поле** `importantRoomUpdates`.

#### Профиль (изменено)

- `GET /api/users/me` (auth)
  - Теперь работает через сессию (без `Authorization`).
  - DTO изменился: см. раздел 3.

- `PUT /api/users/me` (auth + CSRF)
  - Теперь работает через сессию (без `Authorization`).

- `GET /api/users/{userId}` (auth)
  - Теперь работает через сессию (без `Authorization`).

#### Удаление аккаунта (изменено)

- `GET /api/users/me/deletion-preview` (auth)
  - Preview того, можно ли удалить аккаунт “сразу” или нужно решать owned rooms.

- `DELETE /api/users/me` (auth + CSRF)
  - Удаляет аккаунт **только если нет owned rooms**.
  - После удаления: logout + инвалидирование всех сессий пользователя.

- `POST /api/users/me/deletion` (auth + CSRF)
  - Удаление аккаунта **с решением по owned rooms** через `AccountDeletionRequest`.
  - После удаления: logout + инвалидирование всех сессий пользователя.

---

### Rooms (`/api/rooms`)

#### Room lifecycle & governance (новое)

- `PUT /api/rooms/{roomId}` (auth + CSRF)
  - Обновление комнаты через `RoomUpdateRequest`.

- `GET /api/rooms/{roomId}/membership/status` (auth)
  - Статус членства **текущего пользователя** в комнате (`RoomMembershipStatusResponse`).

- `DELETE /api/rooms/{roomId}/participants/{userId}` (auth + CSRF)
  - Удаление участника из комнаты.

- `POST /api/rooms/{roomId}/bans/{userId}` (auth + CSRF)
  - Забанить пользователя в комнате.

- `GET /api/rooms/{roomId}/bans` (auth)
  - Список забаненных пользователей (`List<UserSummaryResponse>`).

- `DELETE /api/rooms/{roomId}/bans/{userId}` (auth + CSRF)
  - Разбан.

- `POST /api/rooms/{roomId}/ownership/transfer` (auth + CSRF)
  - Передача ownership комнаты через `OwnershipTransferRequest`.
  - Возвращает `OwnershipTransferResponse`.

#### Изображения комнат (новое)

- `POST /api/rooms/{roomId}/images` (auth + CSRF, `multipart/form-data`)
  - Part name: `files` (массив)
  - Возвращает `List<RoomImageResponse>` (актуальный список после загрузки).

- `DELETE /api/rooms/{roomId}/images/{imageId}` (auth + CSRF)
  - Возвращает `List<RoomImageResponse>` (актуальный список после удаления).

- `GET /api/rooms/{roomId}/images/{imageId}` (public)
  - Возвращает **байты картинки** (не JSON).

#### Публичность rooms list (изменено)

- `GET /api/rooms` и `GET /api/rooms/{roomId}` теперь **доступны без сессии**.
  - При наличии сессии часть полей (например “я участник?” / membershipStatus) может быть заполнена,
    без сессии — ожидай `null` там, где нужен current user.

---

### QA (`/api/qa`)

#### Поиск + редактирование/удаление (новое)

- `GET /api/qa/questions` (public)
  - Новые query params:
    - `categoryId` (optional)
    - `query` (optional) — строка поиска

- `PUT /api/qa/questions/{questionId}` (auth + CSRF)
  - Обновление вопроса через `QuestionRequest`.

- `DELETE /api/qa/questions/{questionId}` (auth + CSRF)
  - Удаление вопроса.

- `PUT /api/qa/answers/{answerId}` (auth + CSRF)
  - Обновление ответа через `AnswerUpdateRequest`.

- `DELETE /api/qa/answers/{answerId}` (auth + CSRF)
  - Удаление ответа.

Нюанс: `POST /api/qa/questions` и `POST /api/qa/answers` теперь аутентифицируются через session (без `Authorization`).

---

## 3) Изменённые DTO/контракты

Ниже только то, что реально изменилось после `5114d11`.

### Auth / Users

- `LoginRequest`
  - было: `login`
  - стало: `email`

- `UserRegistrationRequest`
  - было: `login`
  - стало: `email`

- `UserProfileResponse`
  - было: `login`
  - стало: `email`
  - добавлено: `avatarUrl` (строится как `/api/users/{userId}/avatar`, либо `null`)

- `UserSummaryResponse`
  - добавлено: `avatarUrl`

- `RoomParticipantResponse`
  - добавлено: `avatarUrl`

- `NotificationSettingsRequest` / `NotificationSettingsResponse`
  - добавлено: `importantRoomUpdates`

### Rooms

- `RoomCreationRequest`
  - добавлены поля: `city`, `country`

- `RoomUpdateRequest` (новый DTO для `PUT /api/rooms/{roomId}`)
  - обязательные поля включают: `isPublic`, `category`, `name`, `description`,
    `maximumNumberOfPeople`, `status`, `ageRating`
  - опциональные: `city`, `country`, `chatLink`, `dateOfStartEvent`, `dateOfEndEvent`, `frequency`

- `RoomSummaryResponse`
  - добавлено: `status` (и связанное поле активности)
  - добавлено: `city`, `country`
  - добавлено: `membershipStatus` (`RoomMembershipStatusResponse`)
  - добавлено: `images` (`List<RoomImageResponse>`) + остаётся `imageIds`

- `RoomImageResponse` (используется в `images`)
  - поля: `id`, `url`, `order`
  - `url` строится как `/api/rooms/{roomId}/images/{imageId}`

---

## 4) Примеры запросов/ответов

### Auth: login (session + CSRF)

**Request** `POST /api/auth/login`
```json
{
  "email": "user@example.com",
  "password": "myStrongPassword123"
}
```

**Response** `200 OK` (и сервер выставит cookie `COACTIVITY_SESSION`)
```json
{
  "id": 42,
  "email": "user@example.com",
  "username": "bomnik",
  "dateOfBirth": "2004-01-01T00:00:00Z",
  "city": "Moscow",
  "country": "Russia",
  "description": "About me",
  "avatarId": 10,
  "avatarUrl": "/api/users/42/avatar",
  "notifications": []
}
```

### Auth: password reset

**1) Request code** `POST /api/auth/password/reset/request`
```json
{ "email": "user@example.com" }
```
**Response** `204 No Content`

**2) Verify code** `POST /api/auth/password/reset/verify`
```json
{ "email": "user@example.com", "code": "123456" }
```
**Response** `204 No Content` (или `403` с `code=INVALID_PASSWORD_RESET_CODE`)

**3) Confirm reset** `POST /api/auth/password/reset/confirm`
```json
{ "email": "user@example.com", "code": "123456", "newPassword": "newStrongPassword123" }
```
**Response** `204 No Content`

### Users: notification settings

**Request** `PUT /api/users/me/notifications`
```json
{
  "membershipAccepted": true,
  "membershipRejected": true,
  "activityClosed": false,
  "newJoinRequest": true,
  "importantRoomUpdates": true
}
```

**Response** `200 OK`
```json
{
  "membershipAccepted": true,
  "membershipRejected": true,
  "activityClosed": false,
  "newJoinRequest": true,
  "importantRoomUpdates": true,
  "updatedAt": "2026-04-13T12:34:56.789Z"
}
```

### Users: avatar upload (multipart)

`PUT /api/users/me/avatar` — отправляется как `multipart/form-data` с part `file`.

**Response** `200 OK` (обновлённый профиль)
```json
{
  "id": 42,
  "email": "user@example.com",
  "username": "bomnik",
  "dateOfBirth": "2004-01-01T00:00:00Z",
  "city": "Moscow",
  "country": "Russia",
  "description": "About me",
  "avatarId": 10,
  "avatarUrl": "/api/users/42/avatar",
  "notifications": []
}
```

### Rooms: upload images (multipart)

`POST /api/rooms/123/images` — `multipart/form-data` с part `files` (массив файлов).

**Response** `200 OK`
```json
[
  { "id": 1001, "url": "/api/rooms/123/images/1001", "order": 1 },
  { "id": 1002, "url": "/api/rooms/123/images/1002", "order": 2 }
]
```

### Rooms: membership status

**Response** `GET /api/rooms/123/membership/status`
```json
{
  "roomId": 123,
  "userId": 42,
  "status": "PARTICIPANT",
  "role": "OWNER",
  "pendingRequestId": null,
  "canJoin": false
}
```

### Rooms: ownership transfer

**Request** `POST /api/rooms/123/ownership/transfer`
```json
{ "targetUserId": 77 }
```

**Response** `200 OK`
```json
{
  "roomId": 123,
  "previousOwnerId": 42,
  "newOwnerId": 77,
  "previousOwnerNewRole": "ADMIN",
  "newOwnerRole": "OWNER"
}
```

### QA: search questions

**Response** `GET /api/qa/questions?query=java`
```json
[
  {
    "id": 501,
    "category": "EDUCATION",
    "question": "How to learn Java?",
    "author": {
      "id": 42,
      "userName": "bomnik",
      "dateOfBirth": "2004-01-01T00:00:00Z",
      "city": "Moscow",
      "country": "Russia",
      "description": "About me",
      "avatarId": 10,
      "avatarUrl": "/api/users/42/avatar"
    }
  }
]
```

### QA: update answer

**Request** `PUT /api/qa/answers/9001`
```json
{ "answer": "Updated answer text" }
```

**Response** `200 OK`
```json
{
  "id": 9001,
  "questionId": 501,
  "previousAnswerId": null,
  "answer": "Updated answer text",
  "author": {
    "id": 42,
    "userName": "bomnik",
    "dateOfBirth": "2004-01-01T00:00:00Z",
    "city": "Moscow",
    "country": "Russia",
    "description": "About me",
    "avatarId": 10,
    "avatarUrl": "/api/users/42/avatar"
  },
  "createdAt": "2026-04-13T12:34:56.789Z",
  "replies": []
}
```

---

## 5) Ошибки (типовые 400/401/403/404)

Формат ошибок: **RFC7807 Problem Details** (`application/problem+json`), см. `docs/error-codes.md`.

### Типовые статусы

- **400** `VALIDATION_FAILED`
  - Невалидный JSON, неверные типы параметров, ошибки `@Valid`, ошибки multipart.

- **401** `AUTH_REQUIRED`
  - Нет активной session cookie `COACTIVITY_SESSION` для защищённых endpoints.

- **403** `ACCESS_DENIED`
  - Нет прав (например, не owner/admin), либо проблемы с CSRF (`X-XSRF-TOKEN` не совпал / отсутствует).
  - Специализированные коды, которые часто встречаются в новых фичах:
    - `EMAIL_NOT_VERIFIED`
    - `INVALID_VERIFICATION_CODE`, `VERIFICATION_CODE_EXPIRED`
    - `INVALID_PASSWORD_RESET_CODE`, `PASSWORD_RESET_CODE_EXPIRED`

- **404** (примеры кодов)
  - `AVATAR_NOT_FOUND`, `AVATAR_METADATA_NOT_FOUND`
  - `ROOM_IMAGE_NOT_FOUND`
  - `ROOM_NOT_FOUND`, `USER_NOT_FOUND`, `QUESTION_NOT_FOUND`, `ANSWER_NOT_FOUND`

### Мини‑пример ошибки
```json
{
  "type": "urn:coactivity:error:VALIDATION_FAILED",
  "title": "Validation failed",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/rooms/123/images",
  "code": "VALIDATION_FAILED",
  "timestamp": "2026-04-13T12:34:56.789Z",
  "traceId": "3c3d5b5d-2a61-4f12-9b42-0d4c3b75f5a0",
  "errors": [
    { "field": "files", "message": "required multipart part is missing", "code": "MISSING_PART" }
  ]
}
```
