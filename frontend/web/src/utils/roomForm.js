import { isApiError } from '../api/httpClient.js'
import { getApiBaseUrl } from '../api/config.js'
import { normalizeRoomCategory } from '../constants/categoryOptions.js'
import { getUserFacingApiMessage } from './userFacingApiError.js'

/**
 * ФТ: максимум участников «может отсутствовать». В OpenAPI (`RoomCreationRequest` / `RoomUpdateRequest`)
 * поле `maximumNumberOfPeople` обязательное — без смены API фронт не может отправить «без лимита»;
 * здесь остаётся числовой дефолт для формы.
 */
export const DEFAULT_ROOM_FORM_STATE = {
  name: '',
  description: '',
  category: 'SPORT',
  isPublic: true,
  maximumNumberOfPeople: 10,
  chatLink: '',
  dateOfStartEvent: '',
  dateOfEndEvent: '',
  frequency: '',
  ageRating: 0,
  city: '',
  country: '',
  status: 'ACTIVE',
}

function clampAgeRating(value) {
  const parsed = Number.parseInt(String(value), 10)
  if (!Number.isFinite(parsed)) {
    return 0
  }
  return Math.min(21, Math.max(0, parsed))
}

function sanitizeOptionalText(value, maxLength) {
  const trimmed = String(value ?? '').trim()
  if (trimmed === '') {
    return null
  }
  return trimmed.slice(0, maxLength)
}

export function localDateTimeToInstantIso(value) {
  if (!value || typeof value !== 'string') {
    return null
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return null
  }

  return parsed.toISOString()
}

export function instantIsoToLocalDateTime(value) {
  if (!value || typeof value !== 'string') {
    return ''
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }

  const localMs = parsed.getTime() - parsed.getTimezoneOffset() * 60_000
  return new Date(localMs).toISOString().slice(0, 16)
}

export function createRoomFormState(overrides = {}) {
  return {
    ...DEFAULT_ROOM_FORM_STATE,
    ...overrides,
  }
}

export function roomToFormState(room) {
  return createRoomFormState({
    name: String(room?.name ?? ''),
    description: String(room?.description ?? ''),
    category: normalizeRoomCategory(room?.category),
    isPublic: room?.isPublic !== false,
    maximumNumberOfPeople: Number(room?.maximumParticipants) || DEFAULT_ROOM_FORM_STATE.maximumNumberOfPeople,
    chatLink: String(room?.chatLink ?? ''),
    dateOfStartEvent: instantIsoToLocalDateTime(room?.dateOfStartEvent),
    dateOfEndEvent: instantIsoToLocalDateTime(room?.dateOfEndEvent),
    frequency: instantIsoToLocalDateTime(room?.frequency),
    ageRating: clampAgeRating(room?.ageRating),
    city: String(room?.city ?? ''),
    country: String(room?.country ?? ''),
    status: String(room?.status ?? DEFAULT_ROOM_FORM_STATE.status).trim().toUpperCase() || 'ACTIVE',
  })
}

export function validateRoomForm(formData, options = {}) {
  const { requireStatus = false } = options
  const name = String(formData?.name ?? '').trim()
  const description = String(formData?.description ?? '').trim()
  const city = String(formData?.city ?? '').trim()
  const country = String(formData?.country ?? '').trim()

  if (name.length < 3 || name.length > 100) {
    return 'Название: от 3 до 100 символов'
  }

  if (description.length > 2000) {
    return 'Описание не длиннее 2000 символов'
  }

  const maxPeople = Number.parseInt(String(formData?.maximumNumberOfPeople ?? ''), 10)
  if (!Number.isFinite(maxPeople) || maxPeople < 2 || maxPeople > 100000) {
    return 'Максимум участников: от 2 до 100 000'
  }

  const ageRating = Number.parseInt(String(formData?.ageRating ?? ''), 10)
  if (!Number.isFinite(ageRating) || ageRating < 0 || ageRating > 21) {
    return 'Возрастной рейтинг: от 0 до 21'
  }

  if (city.length > 100) {
    return 'Город: не длиннее 100 символов'
  }

  if (country.length > 100) {
    return 'Страна: не длиннее 100 символов'
  }

  const chatLink = String(formData?.chatLink ?? '').trim()
  if (chatLink.length > 255) {
    return 'Ссылка на чат: не длиннее 255 символов'
  }

  const dateOfStartEvent = localDateTimeToInstantIso(formData?.dateOfStartEvent)
  const dateOfEndEvent = localDateTimeToInstantIso(formData?.dateOfEndEvent)
  const frequency = localDateTimeToInstantIso(formData?.frequency)

  if (formData?.dateOfStartEvent && !dateOfStartEvent) {
    return 'Некорректная дата начала'
  }

  if (formData?.dateOfEndEvent && !dateOfEndEvent) {
    return 'Некорректная дата окончания'
  }

  if (formData?.frequency && !frequency) {
    return 'Некорректное значение частоты'
  }

  if (dateOfStartEvent && dateOfEndEvent && new Date(dateOfEndEvent) <= new Date(dateOfStartEvent)) {
    return 'Окончание должно быть позже начала'
  }

  if (requireStatus) {
    const status = String(formData?.status ?? '').trim().toUpperCase()
    if (!['ACTIVE', 'INACTIVE', 'COMPLETED'].includes(status)) {
      return 'Выберите корректный статус комнаты'
    }
  }

  return ''
}

export function buildRoomPayload(formData, options = {}) {
  const { includeStatus = false } = options
  const chatLink = sanitizeOptionalText(formData?.chatLink, 255)
  const payload = {
    isPublic: Boolean(formData?.isPublic),
    category: normalizeRoomCategory(formData?.category),
    name: String(formData?.name ?? '').trim(),
    description: String(formData?.description ?? '').trim(),
    city: sanitizeOptionalText(formData?.city, 100),
    country: sanitizeOptionalText(formData?.country, 100),
    maximumNumberOfPeople: Number.parseInt(String(formData?.maximumNumberOfPeople ?? ''), 10),
    chatLink,
    dateOfStartEvent: localDateTimeToInstantIso(formData?.dateOfStartEvent),
    dateOfEndEvent: localDateTimeToInstantIso(formData?.dateOfEndEvent),
    frequency: localDateTimeToInstantIso(formData?.frequency),
    ageRating: clampAgeRating(formData?.ageRating),
  }

  if (includeStatus) {
    payload.status = String(formData?.status ?? '').trim().toUpperCase()
  }

  return payload
}

/**
 * ФТ «Активности» п.2: после создания поля «статические» не меняются — в PUT подставляем их из снимка комнаты,
 * остальное — из формы (защита и от рассинхрона состояния).
 */
export function buildRoomUpdatePayloadFromSnapshot(room, formData, options = {}) {
  const base = buildRoomPayload(formData, options)
  if (!room || typeof room !== 'object') {
    return base
  }

  return {
    ...base,
    isPublic: room.isPublic !== false,
    category: normalizeRoomCategory(room.category),
    name: String(room.name ?? '').trim(),
    description: String(room.description ?? '').trim(),
    ageRating: clampAgeRating(room.ageRating),
  }
}

export function getProblemDetailsMessage(error, fallback) {
  if (!isApiError(error)) {
    return fallback
  }

  return getUserFacingApiMessage(error, fallback)
}

export function sortRoomImages(images) {
  if (!Array.isArray(images)) {
    return []
  }

  return [...images].sort((left, right) => {
    const leftOrder = Number.isFinite(Number(left?.order)) ? Number(left.order) : Number.MAX_SAFE_INTEGER
    const rightOrder = Number.isFinite(Number(right?.order)) ? Number(right.order) : Number.MAX_SAFE_INTEGER

    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }

    const leftId = Number.isFinite(Number(left?.id)) ? Number(left.id) : Number.MAX_SAFE_INTEGER
    const rightId = Number.isFinite(Number(right?.id)) ? Number(right.id) : Number.MAX_SAFE_INTEGER
    return leftId - rightId
  })
}

export function resolveRoomImageUrl(url) {
  if (!url || typeof url !== 'string') {
    return ''
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  if (url.startsWith('/')) {
    const apiBaseUrl = getApiBaseUrl()
    if (/^https?:\/\//i.test(apiBaseUrl)) {
      const apiOrigin = apiBaseUrl.replace(/\/api$/, '')
      return `${apiOrigin}${url}`
    }
  }

  return url
}
