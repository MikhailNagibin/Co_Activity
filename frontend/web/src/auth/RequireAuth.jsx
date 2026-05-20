import { Navigate, useLocation } from 'react-router-dom'
import { useAuthSession } from './authSessionContext.js'

export default function RequireAuth({ children }) {
  const location = useLocation()
  const { isAuthenticated, isLoading } = useAuthSession()

  if (isLoading) {
    return <p style={{ padding: '24px' }}>Проверка сессии...</p>
  }

  if (!isAuthenticated) {
    const next = `${location.pathname}${location.search || ''}`
    const params = new URLSearchParams()
    if (next.startsWith('/') && !next.startsWith('//')) {
      params.set('next', next)
    }
    return <Navigate to={`/sign-in?${params.toString()}`} replace />
  }

  return children
}
