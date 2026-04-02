import { get, post } from '../api/httpClient.js'

export function getRooms(options = {}) {
  return get('/rooms', options)
}

/** POST /api/rooms/createRoom — требуется Bearer-токен. */
export function createRoom(payload, options = {}) {
  return post('/rooms/createRoom', payload, { ...options, withAuth: true })
}
