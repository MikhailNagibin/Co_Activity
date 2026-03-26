const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'

/**
 * VITE_API_BASE_URL should end with `/api` (same prefix as Spring `@RequestMapping` on controllers).
 * If only origin is set (e.g. http://localhost:8080), `/api` is appended.
 */
function normalizeApiBaseUrl(url) {
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

export function getApiBaseUrl() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL
  return normalizeApiBaseUrl(envBaseUrl || DEFAULT_API_BASE_URL)
}
