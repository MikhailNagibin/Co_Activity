import { useCallback, useEffect, useMemo, useState } from 'react'
import { AuthSessionContext } from './authSessionContext.js'
import { me } from '../services/authService.js'
import { isUnauthorizedApiError } from '../utils/sessionExpiredRedirect.js'

export function AuthSessionProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const refreshSession = useCallback(async () => {
    setIsLoading(true)
    try {
      const payload = await me()
      setUser(payload)
      return payload
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        setUser(null)
        return null
      }
      setUser(null)
      throw error
    } finally {
      setIsLoading(false)
    }
  }, [])

  const markAuthenticated = useCallback((nextUser) => {
    setUser(nextUser)
    setIsLoading(false)
  }, [])

  const clearSession = useCallback(() => {
    setUser(null)
    setIsLoading(false)
  }, [])

  useEffect(() => {
    refreshSession().catch(() => {
      setUser(null)
      setIsLoading(false)
    })
  }, [refreshSession])

  const value = useMemo(
    () => ({
      currentUser: user,
      isAuthenticated: Boolean(user),
      isLoading,
      refreshSession,
      markAuthenticated,
      clearSession,
    }),
    [user, isLoading, refreshSession, markAuthenticated, clearSession],
  )

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>
}
