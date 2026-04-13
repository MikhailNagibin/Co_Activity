import { del, get, post, put } from '../api/httpClient.js'

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

/** GET /api/rooms/{roomId}/participants — только участник с ролью OWNER или ADMIN. */
export function getRoomParticipants(roomId, options = {}) {
  return get(`/rooms/${roomId}/participants`, options)
}

/**
 * PUT /api/rooms/{roomId}/bulletin — тело: JSON-строка с текстом (как @RequestBody String на бэкенде).
 */
export function updateRoomBulletin(roomId, content, options = {}) {
  return put(`/rooms/${roomId}/bulletin`, content, options)
}

/** POST /api/rooms/{roomId}/join — требуется активная HTTP-сессия и CSRF. */
export function joinRoom(roomId, options = {}) {
  return post(`/rooms/${roomId}/join`, undefined, options)
}

/** POST /api/rooms/{roomId}/leave — требуется активная HTTP-сессия и CSRF. */
export function leaveRoom(roomId, options = {}) {
  return post(`/rooms/${roomId}/leave`, undefined, options)
}

/** DELETE /api/rooms/{roomId} — только владелец, требуется активная HTTP-сессия и CSRF. */
export function deleteRoom(roomId, options = {}) {
  return del(`/rooms/${roomId}`, options)
}

/** POST /api/rooms/createRoom — требуется активная HTTP-сессия и CSRF. */
export function createRoom(payload, options = {}) {
  return post('/rooms/createRoom', payload, options)
}
