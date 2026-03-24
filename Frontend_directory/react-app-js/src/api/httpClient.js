import { getApiBaseUrl } from './config.js'
import { getAccessToken } from './tokenStorage.js'

export class ApiError extends Error {
  constructor(message, status, details = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

/** HMR/бандлер иногда ломает `instanceof ApiError` — дублируем проверку по имени. */
export function isApiError(error) {
  if (!error) {
    return false
  }
  if (error instanceof ApiError) {
    return true
  }
  return error.name === 'ApiError' && typeof error.status === 'number'
}

/**
 * Текст для ошибок вне HTTP-ответа (сеть, CORS, обрыв соединения).
 */
export function describeFetchFailure(error) {
  const baseUrl = getApiBaseUrl()
  const raw = typeof error?.message === 'string' ? error.message : ''

  if (
    raw.includes('Failed to fetch') ||
    raw.includes('NetworkError') ||
    raw.includes('Load failed') ||
    raw.includes('fetch') && raw.includes('Network')
  ) {
    return `Не удаётся связаться с API (${baseUrl}). Частые причины: core-service не запущен, неверный VITE_API_BASE_URL, или CORS (перезапустите core-service после смены настроек; origin Vite должен быть разрешён).`
  }

  if (raw) {
    return `Запрос не выполнен: ${raw}`
  }

  return 'Запрос не выполнен. Проверьте, что backend запущен и адрес API в настройках фронта верный.'
}

function joinUrl(baseUrl, path) {
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBase}${normalizedPath}`
}

function buildHeaders(customHeaders = {}, withAuth = false, jsonBody = false) {
  const headers = {
    ...customHeaders,
  }

  if (jsonBody) {
    headers['Content-Type'] = 'application/json'
  }

  if (withAuth) {
    const token = getAccessToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  return headers
}

function normalizeDetails(details) {
  if (details == null) {
    return null
  }
  if (Array.isArray(details)) {
    return details.filter(Boolean).join('; ')
  }
  if (typeof details === 'string') {
    return details
  }
  try {
    return JSON.stringify(details)
  } catch {
    return String(details)
  }
}

function parseApiErrorPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return { message: null, details: null }
  }

  const rawDetails = payload.details ?? null
  const details = normalizeDetails(rawDetails)
  const message =
    typeof payload.message === 'string' && payload.message.trim()
      ? payload.message
      : typeof payload.error === 'string' && payload.error.trim()
        ? payload.error
        : null

  return { message, details }
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  if (text == null || text.trim() === '') {
    return null
  }

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')

  if (isJson) {
    try {
      return JSON.parse(text)
    } catch {
      return text
    }
  }

  return text
}

export async function apiRequest(path, options = {}) {
  const { method = 'GET', body, headers, withAuth = false, signal } = options
  const hasJsonBody = body !== undefined
  const response = await fetch(joinUrl(getApiBaseUrl(), path), {
    method,
    headers: buildHeaders(headers, withAuth, hasJsonBody),
    body: hasJsonBody ? JSON.stringify(body) : undefined,
    signal,
  })

  const payload = await parseResponse(response)

  if (!response.ok) {
    const parsedError = parseApiErrorPayload(payload)
    const baseMessage = parsedError.message || `Request failed with status ${response.status}`
    const fullMessage =
      parsedError.details && parsedError.details !== baseMessage
        ? `${baseMessage}${baseMessage.endsWith('.') ? '' : '.'} ${parsedError.details}`
        : baseMessage
    throw new ApiError(fullMessage, response.status, parsedError.details)
  }

  return payload
}

export function get(path, options = {}) {
  return apiRequest(path, { ...options, method: 'GET' })
}

export function post(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'POST', body })
}

export function put(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'PUT', body })
}

export function del(path, options = {}) {
  return apiRequest(path, { ...options, method: 'DELETE' })
}
