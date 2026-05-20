# API Update для фронта (backend)

Период изменений: **после коммита `f80ab5e` (2026-04-19)**.

Дата обновления: **2026-04-19**.

Цель документа: зафиксировать изменения API, связанные с фичей подписок на пользователей и email-уведомлениями о новых комнатах.

---

## Changelog

### Added

- `POST /api/users/{userId}/follow` (auth + CSRF)
  - Создаёт подписку текущего пользователя на пользователя `userId`.
  - `204 No Content` при успехе.
  - `409 Conflict` с `ApiProblemDetail.code=ALREADY_FOLLOWING`, если подписка уже существует.

- `DELETE /api/users/{userId}/follow` (auth + CSRF)
  - Удаляет подписку текущего пользователя на пользователя `userId`.
  - `204 No Content` при успехе.
  - `409 Conflict` с `ApiProblemDetail.code=NOT_FOLLOWING`, если подписки не было.

- `GET /api/users/me/following` (auth)
  - Возвращает `List<UserSummaryResponse>` пользователей, на которых подписан текущий пользователь.

- `GET /api/users/me/followers` (auth)
  - Возвращает `List<UserSummaryResponse>` пользователей, подписанных на текущего пользователя.

- Email-уведомления по подпискам:
  - при создании комнаты пользователем всем его подписчикам публикуется email-команда в Kafka topic `notifications.email.v1`;
  - доставка best-effort, публикация запускается после коммита транзакции создания комнаты (`AFTER_COMMIT`).

### Changed

- `UserSummaryResponse`:
  - добавлено поле `followersCount: Long`.

- `GET /api/users/{userId}` (auth-only, как и раньше):
  - теперь в ответе `UserSummaryResponse` заполняется `followersCount`.

### Error handling

- Для подписок добавлены явные конфликтные коды в `application/problem+json`:
  - `ALREADY_FOLLOWING` — повторная попытка подписаться;
  - `NOT_FOLLOWING` — попытка отписаться без существующей подписки.

- Формат ошибки: `ApiProblemDetail` (RFC 7807 + поля `code`, `traceId`, `timestamp`).

### CSRF (важно для фронта)

Для `POST`/`DELETE` follow-endpoints обязателен CSRF:

- cookie: `XSRF-TOKEN`
- header: `X-XSRF-TOKEN`
- получение токена: `GET /api/auth/csrf`

Без CSRF-токена Spring Security вернёт `403` (`application/problem+json`).

---

## Дополнение: приглашения пользователей в комнату (2026-04-19)

### Added

- `POST /api/rooms/{roomId}/invites` (auth + CSRF)
  - Request body: `{ "userId": <int> }`.
  - Доступ: только `OWNER` комнаты.
  - `204 No Content` при успешной отправке или переотправке приглашения.

### Invitation semantics

- Инвайт всегда отправляется email-командой в Kafka topic `notifications.email.v1`.
- Тело письма содержит:
  - `roomName`;
  - имя автора инвайта (`ownerUserName`);
  - кликабельную ссылку на страницу комнаты во фронте: `{APP_WEB_APP_BASE_URL}/rooms/{roomId}` (настройка `app.links.web-app-base-url` в `core-service`, по умолчанию `http://localhost:5173`).
- Отдельные endpoints для «входящих приглашений» не добавлялись.

### Join behavior changes

- `POST /api/rooms/{roomId}/join` для **приватной** комнаты:
  - если у пользователя есть активный инвайт в эту комнату, пользователь вступает сразу (auto-join);
  - стадия `CONSIDERATION` не создаётся;
  - если pending-заявка уже существовала в `CONSIDERATION`, её статус обновляется в `ACCEPTED`.
- Для **публичной** комнаты инвайт не даёт дополнительных side effects: это только письмо со ссылкой.

### Error handling

- `403 Forbidden`:
  - `ONLY_ROOM_OWNER` — приглашать может только владелец комнаты.
- `404 Not Found`:
  - `ROOM_NOT_FOUND`;
  - `USER_NOT_FOUND`.
- `409 Conflict`:
  - `ALREADY_MEMBER` — приглашённый уже участник;
  - `USER_BANNED` — приглашённый забанен в комнате.
- `503 Service Unavailable`:
  - `NOTIFICATION_DELIVERY_FAILED` — email не удалось опубликовать в Kafka.

### Transaction and consistency notes

- Повторная отправка приглашения разрешена, пока пользователь не стал участником комнаты.
- Создание приглашения и отправка письма связаны транзакционно:
  - при ошибке публикации email-команды возвращается `503`;
  - запись приглашения не должна сохраняться.
- При удалении комнаты или пользователя связанные записи в `room_invitations` удаляются.
