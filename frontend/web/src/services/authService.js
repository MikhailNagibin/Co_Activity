import { get, post } from '../api/httpClient.js'

export function register(userData) {
  return post('/auth/register', userData)
}

export function verifyRegistration({ email, code }) {
  return post('/auth/register/verify', { email, code })
}

export function resendRegistrationVerificationCode({ email }) {
  return post('/auth/register/resend', { email })
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

export function changePassword({ currentPassword, newPassword }) {
  return post('/auth/password/change', { currentPassword, newPassword })
}

export function requestPasswordReset({ email }) {
  return post('/auth/password/reset/request', { email })
}

export function verifyPasswordReset({ email, code }) {
  return post('/auth/password/reset/verify', { email, code })
}

export function confirmPasswordReset({ email, code, newPassword }) {
  return post('/auth/password/reset/confirm', { email, code, newPassword })
}
