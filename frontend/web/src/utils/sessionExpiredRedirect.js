import { isApiError } from '../api/httpClient.js'
import { clearAccessToken } from '../api/tokenStorage.js'

/**
 * В core-service 401 соответствует только TokenValidationException (сессия/токен).
 * Неверный пароль при входе — 403, не 401.
 */
export function isUnauthorizedApiError(error) {
  return isApiError(error) && error.status === 401
}

/** Сбрасывает токен и ведёт на вход с подсказкой о сроке сессии. */
export function redirectToSignInForExpiredSession(navigate, options = {}) {
  clearAccessToken()
  const params = new URLSearchParams()
  params.set('session', 'expired')
  const next = options.next
  if (typeof next === 'string' && next.startsWith('/') && !next.startsWith('//')) {
    params.set('next', next)
  }
  navigate(`/sign-in?${params.toString()}`, { replace: true })
}
