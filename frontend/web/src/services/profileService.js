import { del, get, post, put } from '../api/httpClient.js'
import { logout as authLogout, me } from './authService.js'

export function getMyProfile() {
  return me()
}

export function updateMyProfile(payload, options = {}) {
  return put('/users/me', payload, options)
}

export function updateMyNotificationSettings(payload, options = {}) {
  return put('/users/me/notifications', payload, options)
}

export function logout(options = {}) {
  return authLogout(options)
}

export function deleteMyAccount(options = {}) {
  return del('/users/me', options)
}

export function getMyDeletionPreview(options = {}) {
  return get('/users/me/deletion-preview', options)
}

export function deleteMyAccountWithActions(payload, options = {}) {
  return post('/users/me/deletion', payload, options)
}

export function getSentJoinRequests(options = {}) {
  return get('/users/requests/sent', options)
}

export function getBannedRooms(options = {}) {
  return get('/users/banned-rooms', options)
}
