const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'

export function getApiBaseUrl() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL
  return envBaseUrl?.trim() || DEFAULT_API_BASE_URL
}
