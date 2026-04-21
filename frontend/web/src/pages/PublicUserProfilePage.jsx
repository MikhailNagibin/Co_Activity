import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileAuthRail from '../components/ProfileAuthRail.jsx'
import { isApiError } from '../api/httpClient.js'
import {
  followUser,
  getMyFollowing,
  getPublicUserProfile,
  logout,
  unfollowUser,
} from '../services/profileService.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import { resolveUserAvatarUrl, resolveUserName } from '../utils/userProfile.js'

function formatDateOfBirth(value) {
  if (!value) {
    return 'Не указана'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return 'Не указана'
  }

  return parsed.toLocaleDateString('ru-RU')
}

function resolveSessionUserId(user) {
  const raw = user?.id ?? user?.userId
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

function buildErrorState(error) {
  if (isApiError(error) && error.status === 404) {
    return {
      title: 'Пользователь не найден',
      detail: 'Проверьте ссылку или вернитесь к списку активностей.',
    }
  }

  if (isApiError(error) && error.status === 403) {
    return {
      title: 'Профиль недоступен',
      detail: 'У вас нет доступа к просмотру этого профиля.',
    }
  }

  return {
    title: 'Профиль не загрузился',
    detail: isApiError(error)
      ? getUserFacingApiMessage(error, 'Не удалось загрузить профиль пользователя')
      : 'Не удалось загрузить профиль пользователя',
  }
}

function PublicUserProfilePage() {
  const navigate = useNavigate()
  const { userId: userIdParam } = useParams()
  const { isAuthenticated, clearSession, currentUser } = useAuthSession()
  const userId = Number.parseInt(String(userIdParam ?? ''), 10)
  const sessionUserId = useMemo(() => resolveSessionUserId(currentUser), [currentUser])

  const [isLoading, setIsLoading] = useState(true)
  const [profile, setProfile] = useState(null)
  const [errorState, setErrorState] = useState(null)
  const [isFollowing, setIsFollowing] = useState(false)
  const [isFollowingStatusLoading, setIsFollowingStatusLoading] = useState(false)
  const [followActionLoading, setFollowActionLoading] = useState(false)
  const [followActionError, setFollowActionError] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadProfile = async () => {
      if (!Number.isFinite(userId) || userId <= 0) {
        setErrorState({
          title: 'Некорректный профиль',
          detail: 'Идентификатор пользователя указан неверно.',
        })
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setErrorState(null)
      setFollowActionError('')

      try {
        const payload = await getPublicUserProfile(userId)
        if (!isMounted) {
          return
        }
        setProfile(payload)
      } catch (error) {
        if (!isMounted) {
          return
        }
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, {
            next: `/users/${encodeURIComponent(String(userId))}`,
          })
          return
        }
        setProfile(null)
        setErrorState(buildErrorState(error))
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    if (isAuthenticated) {
      loadProfile()
    } else {
      setIsLoading(false)
      setIsFollowing(false)
      setIsFollowingStatusLoading(false)
      setFollowActionError('')
    }

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, navigate, userId])

  useEffect(() => {
    if (!profile || sessionUserId == null || Number(sessionUserId) === Number(profile.id)) {
      setIsFollowing(false)
      setIsFollowingStatusLoading(false)
      return
    }

    let cancelled = false

    const loadFollowingStatus = async () => {
      setIsFollowingStatusLoading(true)
      try {
        const list = await getMyFollowing()
        if (cancelled) {
          return
        }
        const rows = Array.isArray(list) ? list : []
        setIsFollowing(rows.some((u) => Number(u?.id) === userId))
      } catch (error) {
        if (cancelled) {
          return
        }
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, {
            next: `/users/${encodeURIComponent(String(userId))}`,
          })
          return
        }
        setFollowActionError(getUserFacingApiMessage(error, 'Не удалось проверить подписку'))
        setIsFollowing(false)
      } finally {
        if (!cancelled) {
          setIsFollowingStatusLoading(false)
        }
      }
    }

    loadFollowingStatus()

    return () => {
      cancelled = true
    }
  }, [profile, sessionUserId, userId, navigate])

  const handleFollowToggle = useCallback(async () => {
    if (!Number.isFinite(userId) || userId <= 0) {
      return
    }

    setFollowActionError('')

    const nextFollow = !isFollowing
    setFollowActionLoading(true)
    try {
      if (nextFollow) {
        await followUser(userId)
        setIsFollowing(true)
        setProfile((prev) => {
          if (!prev) {
            return prev
          }
          const c = prev.followersCount ?? 0
          return { ...prev, followersCount: c + 1 }
        })
      } else {
        await unfollowUser(userId)
        setIsFollowing(false)
        setProfile((prev) => {
          if (!prev) {
            return prev
          }
          const c = prev.followersCount ?? 0
          return { ...prev, followersCount: Math.max(0, c - 1) }
        })
      }
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/users/${encodeURIComponent(String(userId))}`,
        })
        return
      }
      if (isApiError(error) && error.status === 409) {
        if (error.code === 'ALREADY_FOLLOWING') {
          setIsFollowing(true)
          return
        }
        if (error.code === 'NOT_FOLLOWING') {
          setIsFollowing(false)
          return
        }
      }
      setFollowActionError(
        getUserFacingApiMessage(
          error,
          nextFollow ? 'Не удалось подписаться' : 'Не удалось отписаться',
        ),
      )
    } finally {
      setFollowActionLoading(false)
    }
  }, [isFollowing, navigate, userId])

  const handleLogout = async () => {
    try {
      await logout()
    } catch {
      // Even if backend logout fails, local session state should be reset.
    } finally {
      clearSession()
      navigate('/sign-in')
    }
  }

  const sessionEnded = Boolean(isAuthenticated && !isLoading && !profile && !errorState)
  const displayName = resolveUserName(profile, 'Пользователь')
  const avatarUrl = resolveUserAvatarUrl(profile)
  const showFollowControls =
    Boolean(profile) &&
    sessionUserId != null &&
    Number(sessionUserId) !== Number(profile?.id)
  const followersDisplay =
    profile?.followersCount != null ? profile.followersCount : '—'

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-shell">
        <div className="profile-shell__column">
          <section className="main-hero">
            <h2>Профиль пользователя</h2>
            <h3 className="gray-elem">
              <Link to="/main" className="notification-settings-back">
                ← К активностям
              </Link>
            </h3>
          </section>

          <main className="profile-page">
            {isLoading ? <p>Загрузка профиля...</p> : null}

            {!isLoading && errorState ? (
              <section className="profile-panel profile-session-fallback">
                <h3>{errorState.title}</h3>
                <p className="gray-elem">{errorState.detail}</p>
              </section>
            ) : null}

            {!isLoading && profile ? (
              <div className="profile-grid">
                <section className="profile-panel profile-panel--overview">
                  <div className="profile-overview">
                    <div className="profile-overview__avatar">
                      {avatarUrl ? (
                        <img
                          className="profile-overview__avatar-image"
                          src={avatarUrl}
                          alt={`Аватар пользователя ${displayName}`}
                        />
                      ) : (
                        <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
                      )}
                    </div>
                    <div className="profile-overview__body">
                      <p className="profile-kicker">Публичный профиль</p>
                      <h3>{displayName}</h3>
                      <p className="gray-elem">ID пользователя: {profile.id}</p>
                    </div>
                  </div>
                  {showFollowControls ? (
                    <div className="profile-follow-row">
                      <button
                        type="button"
                        className={
                          isFollowing
                            ? 'profile-follow-btn profile-follow-btn--following'
                            : 'profile-follow-btn'
                        }
                        onClick={handleFollowToggle}
                        disabled={followActionLoading || isFollowingStatusLoading}
                      >
                        {followActionLoading
                          ? 'Сохранение…'
                          : isFollowingStatusLoading
                            ? 'Проверка…'
                            : isFollowing
                              ? 'Отписаться'
                              : 'Подписаться'}
                      </button>
                      {followActionError ? (
                        <p className="profile-follow-error" role="alert">
                          {followActionError}
                        </p>
                      ) : null}
                    </div>
                  ) : null}
                  <div className="profile-summary-grid">
                    <article className="profile-summary-card">
                      <span className="profile-summary-card__label">Подписчики</span>
                      <strong className="profile-summary-card__value">{followersDisplay}</strong>
                    </article>
                    <article className="profile-summary-card">
                      <span className="profile-summary-card__label">Дата рождения</span>
                      <strong className="profile-summary-card__value">
                        {formatDateOfBirth(profile.dateOfBirth)}
                      </strong>
                    </article>
                    <article className="profile-summary-card">
                      <span className="profile-summary-card__label">Город</span>
                      <strong className="profile-summary-card__value">
                        {profile.city || 'Не указан'}
                      </strong>
                    </article>
                    <article className="profile-summary-card">
                      <span className="profile-summary-card__label">Страна</span>
                      <strong className="profile-summary-card__value">
                        {profile.country || 'Не указана'}
                      </strong>
                    </article>
                  </div>
                </section>

                <section className="profile-panel">
                  <p className="profile-kicker">О пользователе</p>
                  <h3>Описание</h3>
                  <p className="profile-description">
                    {profile.description || 'Пользователь пока ничего о себе не рассказал.'}
                  </p>
                </section>
              </div>
            ) : null}
          </main>
        </div>
        <ProfileAuthRail
          hasToken={isAuthenticated}
          onLogout={handleLogout}
          sessionEnded={sessionEnded}
        />
      </div>
    </>
  )
}

export default PublicUserProfilePage
