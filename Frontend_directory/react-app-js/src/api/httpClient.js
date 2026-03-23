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

function joinUrl(baseUrl, path) {
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBase}${normalizedPath}`
}

function buildHeaders(customHeaders = {}, withAuth = false) {
  const headers = {
    'Content-Type': 'application/json',
    ...customHeaders,
  }

  if (withAuth) {
    const token = getAccessToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  return headers
}

function parseApiErrorPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return { message: null, details: null }
  }

  const details = payload.details ?? null
  const message =
    typeof payload.message === 'string' && payload.message.trim()
      ? payload.message
      : typeof payload.error === 'string' && payload.error.trim()
        ? payload.error
        : null

  return { message, details }
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')

  if (response.status === 204) {
    return null
  }

  if (isJson) {
    return response.json()
  }

  return response.text()
}

export async function apiRequest(path, options = {}) {
  const { method = 'GET', body, headers, withAuth = false, signal } = options
  const response = await fetch(joinUrl(getApiBaseUrl(), path), {
    method,
    headers: buildHeaders(headers, withAuth),
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

  const payload = await parseResponse(response)

  if (!response.ok) {
    const parsedError = parseApiErrorPayload(payload)
    throw new ApiError(
      parsedError.message || `Request failed with status ${response.status}`,
      response.status,
      parsedError.details,
    )
  }

  return payload
}

export function get(path, options = {}) {
  return apiRequest(path, { ...options, method: 'GET' })
}

export function post(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'POST', body })
}
