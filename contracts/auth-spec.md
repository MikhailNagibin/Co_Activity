# Auth Contract v2 (Co_Activity)

## Transport
- Browser auth uses server-side HTTP sessions via Spring Security + Spring Session + Redis.
- Session cookie: `COACTIVITY_SESSION`.
- Cookie flags: `HttpOnly`, `SameSite=Lax`, `Secure` outside local development.

## CSRF
- Mutating requests use CSRF protection.
- CSRF cookie: `XSRF-TOKEN`.
- Client sends the same value in header `X-XSRF-TOKEN`.
- Bootstrap endpoint: `GET /api/auth/csrf`.

## Public auth endpoints
- `POST /api/auth/register`
- `POST /api/auth/register/verify`
- `POST /api/auth/login`
- `GET /api/auth/csrf`

## Authenticated auth endpoints
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/password/change`

## Registration verification
- User is created in status `PENDING_VERIFICATION`.
- Verification code is short-lived and stored server-side in Redis.
- Login is allowed only after successful email verification.

## Session rules
- Session state is shared across instances through Redis.
- Logout invalidates the server-side session.
- Login invalidates an existing session cookie before issuing a fresh session.
- Password change invalidates all active sessions of that user.

## Account deletion flow
- `DELETE /api/users/me` works only when the user owns no rooms.
- If the user owns rooms, backend returns `409 Conflict` with code `OWNED_ROOMS_RESOLUTION_REQUIRED`.
- `GET /api/users/me/deletion-preview` returns `canDeleteImmediately` and a per-room list of transfer candidates.
- `POST /api/users/me/deletion` accepts one action per owned room:
  - `DELETE_ROOM`
  - `TRANSFER_OWNERSHIP` with `transferToUserId`
- Ownership can be transferred only to an existing room member with role `ADMIN` or `PARTICIPANT`.
- Successful account deletion invalidates all active sessions of that user.
