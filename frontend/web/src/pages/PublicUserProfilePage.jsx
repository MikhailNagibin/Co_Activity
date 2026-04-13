import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileAuthRail from '../components/ProfileAuthRail.jsx'
import { isApiError } from '../api/httpClient.js'
import { getPublicUserProfile, logout } from '../services/profileService.js'
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
  const { isAuthenticated, clearSession } = useAuthSession()
  const userId = Number.parseInt(String(userIdParam ?? ''), 10)

  const [isLoading, setIsLoading] = useState(true)
  const [profile, setProfile] = useState(null)
  const [errorState, setErrorState] = useState(null)

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
    }

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, navigate, userId])

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
                  <div className="profile-summary-grid">
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
