import { del, get, post, put } from '../api/httpClient.js'
import { me } from './authService.js'

export function getMyProfile() {
  return me()
}

export function updateMyProfile(payload, options = {}) {
  return put('/users/me', payload, { ...options, withAuth: true })
}

export function updateMyNotificationSettings(payload, options = {}) {
  return put('/users/me/notifications', payload, { ...options, withAuth: true })
}

export function logout(options = {}) {
  return post('/users/logout', undefined, { ...options, withAuth: true })
}

export function deleteMyAccount(options = {}) {
  return del('/users/me', { ...options, withAuth: true })
}

export function getSentJoinRequests(options = {}) {
  return get('/users/requests/sent', { ...options, withAuth: true })
}

export function getBannedRooms(options = {}) {
  return get('/users/banned-rooms', { ...options, withAuth: true })
}
