import { ApiError, get, post } from '../api/httpClient.js'
import { setAccessToken } from '../api/tokenStorage.js'

export function register(userData) {
  return post('/users', userData)
}

export function loginStep1({ login, password }) {
  return post('/users/login', { login, password })
}

export async function verifyCode({ login, code }) {
  const loginParam = encodeURIComponent(login)
  const codeParam = encodeURIComponent(code)
  const payload = await post(`/users/login/verify?login=${loginParam}&code=${codeParam}`)

  const token = payload?.token
  if (!token || typeof token !== 'string') {
    throw new ApiError('Сервер не вернул токен. Повторите вход.', 200, null)
  }
  setAccessToken(token)

  return payload
}

export function me() {
  return get('/users/me', { withAuth: true })
}
