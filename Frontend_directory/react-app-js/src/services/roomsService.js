import { get } from '../api/httpClient.js'

export function getRooms() {
  return get('/rooms')
}
