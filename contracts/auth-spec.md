# Auth Contract v1 (Co_Activity)

## Token type
- Access token only (JWT, no refresh in v1).
- Transport: `Authorization: Bearer <token>`.

## JWT claims
- `iss`: `coactivity-core`
- `sub`: user id as string
- `aud`: `coactivity-api`
- `roles`: array of roles
- `iat`: issued-at timestamp
- `exp`: expiration timestamp
- `jti`: unique token id

## Validation rules
- Signature must be valid (`HS256` in v1).
- `iss`, `aud`, `exp` must be valid.
- Token is invalid if `jti` is revoked in issuer instance.

## Operational notes (v1 simplification)
- No refresh token flow.
- Logout is best-effort because revocation is in-memory for now.
- Short TTL (30 minutes) is mandatory.
