import { get, post } from '../api/httpClient.js'

export function register(userData) {
  return post('/auth/register', userData)
}

export function verifyRegistration({ email, code }) {
  return post('/auth/register/verify', { email, code })
}

export function login({ email, password }) {
  return post('/auth/login', { email, password })
}

export function logout() {
  return post('/auth/logout')
}

export function me() {
  return get('/auth/me')
}

export function getCsrf() {
  return get('/auth/csrf')
}
