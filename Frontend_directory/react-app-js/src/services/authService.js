import { get, post } from '../api/httpClient.js'
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

  if (payload?.token) {
    setAccessToken(payload.token)
  }

  return payload
}

export function me() {
  return get('/users/me', { withAuth: true })
}
