import { del, get, post, put } from '../api/httpClient.js'
import { logout as authLogout } from './authService.js'

export function getMyProfile(options = {}) {
  return get('/users/me', options)
}

export function updateMyProfile(payload, options = {}) {
  return put('/users/me', payload, options)
}

export function getPublicUserProfile(userId, options = {}) {
  return get(`/users/${encodeURIComponent(String(userId))}`, options)
}

export function followUser(userId, options = {}) {
  return post(`/users/${encodeURIComponent(String(userId))}/follow`, undefined, options)
}

export function unfollowUser(userId, options = {}) {
  return del(`/users/${encodeURIComponent(String(userId))}/follow`, options)
}

export function getMyFollowing(options = {}) {
  return get('/users/me/following', options)
}

export function getMyFollowers(options = {}) {
  return get('/users/me/followers', options)
}

export function getMyNotificationSettings(options = {}) {
  return get('/users/me/notifications', options)
}

export function updateMyNotificationSettings(payload, options = {}) {
  return put('/users/me/notifications', payload, options)
}

export function uploadMyAvatar(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  return put('/users/me/avatar', formData, options)
}

export function deleteMyAvatar(options = {}) {
  return del('/users/me/avatar', options)
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

export function cancelSentJoinRequest(requestId, options = {}) {
  return del(`/users/requests/${requestId}`, options)
}

export function getBannedRooms(options = {}) {
  return get('/users/banned-rooms', options)
}
