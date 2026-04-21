const DEFAULT_API_BASE_URL = '/api'

/** Локальное переопределение базового URL API (только в браузере). */
export const API_BASE_URL_OVERRIDE_STORAGE_KEY = 'coactivity.apiBaseUrlOverride'

/**
 * VITE_API_BASE_URL should end with `/api` (same prefix as Spring `@RequestMapping` on controllers).
 * If only origin is set (e.g. http://localhost:8080), `/api` is appended.
 */
export function normalizeApiBaseUrl(url) {
  if (!url) {
    return DEFAULT_API_BASE_URL
  }
  const trimmed = url.trim().replace(/\/+$/, '')
  if (trimmed.endsWith('/api')) {
    return trimmed
  }
  if (/^https?:\/\/[^/]+$/.test(trimmed)) {
    return `${trimmed}/api`
  }
  return trimmed
}

export function getApiBaseUrlFromEnv() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL
  return normalizeApiBaseUrl(envBaseUrl || DEFAULT_API_BASE_URL)
}

function readApiBaseUrlOverride() {
  if (typeof localStorage === 'undefined') {
    return ''
  }
  try {
    const raw = localStorage.getItem(API_BASE_URL_OVERRIDE_STORAGE_KEY)
    return raw != null ? String(raw).trim() : ''
  } catch {
    return ''
  }
}

/**
 * Сохранённое в браузере переопределение. Пустая строка — снять override и использовать сборку/env.
 */
export function setApiBaseUrlOverride(urlOrEmpty) {
  if (typeof localStorage === 'undefined') {
    return
  }
  try {
    const trimmed = urlOrEmpty == null ? '' : String(urlOrEmpty).trim()
    if (!trimmed) {
      localStorage.removeItem(API_BASE_URL_OVERRIDE_STORAGE_KEY)
    } else {
      localStorage.setItem(API_BASE_URL_OVERRIDE_STORAGE_KEY, trimmed)
    }
  } catch {
    // ignore quota / private mode
  }
}

export function getApiBaseUrl() {
  const override = readApiBaseUrlOverride()
  if (override) {
    return normalizeApiBaseUrl(override)
  }
  return getApiBaseUrlFromEnv()
}

export function joinApiUrl(path) {
  const baseUrl = getApiBaseUrl()
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBase}${normalizedPath}`
}
