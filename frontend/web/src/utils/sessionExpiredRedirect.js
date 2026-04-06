import { isApiError } from '../api/httpClient.js'

/**
 * В core-service 401 означает, что сессия отсутствует или истекла.
 * Неверный пароль при входе приходит как 403 от domain-level исключения.
 */
export function isUnauthorizedApiError(error) {
  return isApiError(error) && error.status === 401
}

/** Ведёт на вход с подсказкой о сроке сессии. */
export function redirectToSignInForExpiredSession(navigate, options = {}) {
  const params = new URLSearchParams()
  params.set('session', 'expired')
  const next = options.next
  if (typeof next === 'string' && next.startsWith('/') && !next.startsWith('//')) {
    params.set('next', next)
  }
  navigate(`/sign-in?${params.toString()}`, { replace: true })
}
