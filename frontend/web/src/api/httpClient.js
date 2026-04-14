import { getApiBaseUrl } from './config.js'

export class ApiError extends Error {
  constructor(message, status, details = null, code = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
    this.code = code
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
  const raw = typeof error?.message === 'string' ? error.message : ''

  if (
    raw.includes('Failed to fetch') ||
    raw.includes('NetworkError') ||
    raw.includes('Load failed') ||
    raw.includes('fetch') && raw.includes('Network')
  ) {
    return 'Не удаётся связаться с сервером. Проверьте интернет и что сервер приложения запущен (для разработки: core-service, CORS и адрес API в настройках фронта).'
  }

  if (raw) {
    return 'Запрос не выполнен. Проверьте подключение к интернету и что сервер приложения запущен.'
  }

  return 'Запрос не выполнен. Проверьте подключение и настройки приложения.'
}

function joinUrl(baseUrl, path) {
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBase}${normalizedPath}`
}

let unauthorizedResponseHandler = null

export function setUnauthorizedResponseHandler(handler) {
  unauthorizedResponseHandler = typeof handler === 'function' ? handler : null
}

function isFormDataBody(body) {
  return typeof FormData !== 'undefined' && body instanceof FormData
}

function readCookie(name) {
  if (typeof document === 'undefined' || !document.cookie) {
    return null
  }

  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

async function ensureCsrfToken() {
  const existing = readCookie('XSRF-TOKEN')
  if (existing) {
    return existing
  }

  const response = await fetch(joinUrl(getApiBaseUrl(), '/auth/csrf'), {
    method: 'GET',
    credentials: 'include',
  })

  if (!response.ok) {
    return null
  }

  try {
    const payload = await response.json()
    return readCookie('XSRF-TOKEN') || payload?.token || null
  } catch {
    return readCookie('XSRF-TOKEN')
  }
}

function buildHeaders(customHeaders = {}, contentType = null, csrfToken = null) {
  const headers = {
    ...customHeaders,
  }

  if (contentType) {
    headers['Content-Type'] = contentType
  }

  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  return headers
}

function normalizeDetails(details) {
  if (details == null) {
    return null
  }
  if (Array.isArray(details)) {
    return details
      .map((item) => {
        if (!item) {
          return null
        }
        if (typeof item === 'string') {
          return item
        }
        if (typeof item === 'object') {
          const field = typeof item.field === 'string' ? item.field : ''
          const message = typeof item.message === 'string' ? item.message : ''
          if (field && message) {
            return `${field}: ${message}`
          }
          if (message) {
            return message
          }
        }
        return String(item)
      })
      .filter(Boolean)
      .join('; ')
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
    return { message: null, details: null, code: null }
  }

  const rawDetails = payload.details ?? payload.errors ?? null
  const details = normalizeDetails(rawDetails)
  const code =
    typeof payload.code === 'string' && payload.code.trim()
      ? payload.code
      : null
  const message =
    typeof payload.message === 'string' && payload.message.trim()
      ? payload.message
      : typeof payload.error === 'string' && payload.error.trim()
        ? payload.error
        : typeof payload.detail === 'string' && payload.detail.trim()
          ? payload.detail
          : typeof payload.title === 'string' && payload.title.trim()
            ? payload.title
        : null

  return { message, details, code }
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
  const isJson =
    contentType.includes('application/json') || contentType.includes('application/problem+json')

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
  const { method = 'GET', body, headers, signal, stringifyBody = true, contentType = null } = options
  const hasFormDataBody = isFormDataBody(body)
  const hasStructuredBody = body !== undefined && !hasFormDataBody
  const upperMethod = String(method).toUpperCase()
  const csrfToken = ['GET', 'HEAD', 'OPTIONS'].includes(upperMethod)
    ? null
    : await ensureCsrfToken()
  const resolvedContentType =
    contentType ??
    (hasStructuredBody && stringifyBody ? 'application/json' : null)
  const response = await fetch(joinUrl(getApiBaseUrl(), path), {
    method,
    headers: buildHeaders(headers, resolvedContentType, csrfToken),
    body:
      body === undefined
        ? undefined
        : hasFormDataBody
          ? body
          : stringifyBody
            ? JSON.stringify(body)
            : body,
    signal,
    credentials: 'include',
  })

  const payload = await parseResponse(response)

  if (!response.ok) {
    if (response.status === 401) {
      try {
        unauthorizedResponseHandler?.()
      } catch {
        // Ignore sync errors in auth state cleanup and keep throwing the API error.
      }
    }
    const parsedError = parseApiErrorPayload(payload)
    const baseMessage = parsedError.message || `Request failed with status ${response.status}`
    const fullMessage =
      parsedError.details && parsedError.details !== baseMessage
        ? `${baseMessage}${baseMessage.endsWith('.') ? '' : '.'} ${parsedError.details}`
        : baseMessage
    throw new ApiError(fullMessage, response.status, parsedError.details, parsedError.code)
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

export function patch(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'PATCH', body })
}

export function del(path, options = {}) {
  return apiRequest(path, { ...options, method: 'DELETE' })
}
