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
