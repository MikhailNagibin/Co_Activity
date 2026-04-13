import { apiRequest, del, get, post, put } from '../api/httpClient.js'

export function getRooms(options = {}) {
  return get('/rooms', options)
}

export function getRoomById(roomId, options = {}) {
  return get(`/rooms/${roomId}`, options)
}

export function getMyRooms(options = {}) {
  return get('/rooms/me', options)
}

export function getRoomMembershipStatus(roomId, options = {}) {
  return get(`/rooms/${roomId}/membership/status`, options)
}

/** GET /api/users/requests/pending — pending-заявки во все администрируемые комнаты. */
export function getPendingJoinRequests(options = {}) {
  return get('/users/requests/pending', options)
}

/** GET /api/users/rooms/{roomId}/requests/pending — pending-заявки по конкретной комнате. */
export function getPendingJoinRequestsForRoom(roomId, options = {}) {
  return get(`/users/rooms/${roomId}/requests/pending`, options)
}

/** POST /api/users/requests/{requestId}?action=... — обработать заявку на вступление. */
export function processJoinRequest(requestId, action, options = {}) {
  const normalizedAction = String(action ?? '').trim().toUpperCase()
  return post(`/users/requests/${requestId}?action=${encodeURIComponent(normalizedAction)}`, undefined, options)
}

/** GET /api/rooms/{roomId}/participants — только участник с ролью OWNER или ADMIN. */
export function getRoomParticipants(roomId, options = {}) {
  return get(`/rooms/${roomId}/participants`, options)
}

/**
 * PUT /api/rooms/{roomId}/bulletin — plain text, требуется OWNER/ADMIN и CSRF.
 */
export function updateRoomBulletin(roomId, content, options = {}) {
  return apiRequest(`/rooms/${roomId}/bulletin`, {
    ...options,
    method: 'PUT',
    body: content,
    stringifyBody: false,
    contentType: 'text/plain',
  })
}

/** DELETE /api/rooms/{roomId}/bulletin — очистка доски объявлений. */
export function deleteRoomBulletin(roomId, options = {}) {
  return del(`/rooms/${roomId}/bulletin`, options)
}

/** POST /api/rooms/{roomId}/join — требуется активная HTTP-сессия и CSRF. */
export function joinRoom(roomId, options = {}) {
  return post(`/rooms/${roomId}/join`, undefined, options)
}

/** POST /api/rooms/{roomId}/leave — требуется активная HTTP-сессия и CSRF. */
export function leaveRoom(roomId, options = {}) {
  return post(`/rooms/${roomId}/leave`, undefined, options)
}

/** DELETE /api/rooms/{roomId}/participants/{userId} — удаление участника. */
export function removeRoomParticipant(roomId, userId, options = {}) {
  return del(`/rooms/${roomId}/participants/${userId}`, options)
}

/** GET /api/rooms/{roomId}/bans — список банов комнаты. */
export function getRoomBans(roomId, options = {}) {
  return get(`/rooms/${roomId}/bans`, options)
}

/** POST /api/rooms/{roomId}/bans/{userId} — заблокировать пользователя в комнате. */
export function banRoomUser(roomId, userId, options = {}) {
  return post(`/rooms/${roomId}/bans/${userId}`, undefined, options)
}

/** DELETE /api/rooms/{roomId}/bans/{userId} — снять бан пользователя. */
export function unbanRoomUser(roomId, userId, options = {}) {
  return del(`/rooms/${roomId}/bans/${userId}`, options)
}

/** POST /api/users/rooms/{roomId}/admins/{userId} — назначить администратора. */
export function assignRoomAdmin(roomId, userId, options = {}) {
  return post(`/users/rooms/${roomId}/admins/${userId}`, undefined, options)
}

/** DELETE /api/users/rooms/{roomId}/admins/{userId} — снять роль администратора. */
export function demoteRoomAdmin(roomId, userId, options = {}) {
  return del(`/users/rooms/${roomId}/admins/${userId}`, options)
}

/** POST /api/rooms/{roomId}/ownership/transfer — передать ownership. */
export function transferRoomOwnership(roomId, targetUserId, options = {}) {
  return post(`/rooms/${roomId}/ownership/transfer`, { targetUserId }, options)
}

/** DELETE /api/rooms/{roomId} — только владелец, требуется активная HTTP-сессия и CSRF. */
export function deleteRoom(roomId, options = {}) {
  return del(`/rooms/${roomId}`, options)
}

/** POST /api/rooms/createRoom — требуется активная HTTP-сессия и CSRF. */
export function createRoom(payload, options = {}) {
  return post('/rooms/createRoom', payload, options)
}

/** PUT /api/rooms/{roomId} — требуется OWNER/ADMIN и активная HTTP-сессия с CSRF. */
export function updateRoom(roomId, payload, options = {}) {
  return put(`/rooms/${roomId}`, payload, options)
}

/** POST /api/rooms/{roomId}/images — multipart files[]. */
export function uploadRoomImages(roomId, files, options = {}) {
  const formData = new FormData()
  for (const file of files ?? []) {
    formData.append('files', file)
  }
  return post(`/rooms/${roomId}/images`, formData, options)
}

/** DELETE /api/rooms/{roomId}/images/{imageId} — возвращает актуальный список картинок. */
export function deleteRoomImage(roomId, imageId, options = {}) {
  return del(`/rooms/${roomId}/images/${imageId}`, options)
}
