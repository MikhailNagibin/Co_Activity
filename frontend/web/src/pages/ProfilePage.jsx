import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import ProfileAuthRail from '../components/ProfileAuthRail.jsx'
import { clearAccessToken, getAccessToken } from '../api/tokenStorage.js'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { deleteMyAccount, getMyProfile, logout, updateMyProfile } from '../services/profileService.js'

function instantToInputDate(value) {
  if (!value) {
    return ''
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }
  return parsed.toISOString().slice(0, 10)
}

function dateInputToInstant(value) {
  if (!value) {
    return null
  }
  return `${value}T00:00:00Z`
}

function ProfilePage() {
  const navigate = useNavigate()
  const hasToken = Boolean(getAccessToken())
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [profile, setProfile] = useState(null)
  const [profileForm, setProfileForm] = useState({
    username: '',
    dateOfBirth: '',
    country: '',
    city: '',
    description: '',
    avatarId: '',
  })

  useEffect(() => {
    let isMounted = true

    const loadProfile = async () => {
      setIsLoading(true)
      setErrorMessage('')
      try {
        const payload = await getMyProfile()
        if (!isMounted) {
          return
        }
        setProfile(payload)
        setProfileForm({
          username: payload?.username ?? '',
          dateOfBirth: instantToInputDate(payload?.dateOfBirth),
          country: payload?.country ?? '',
          city: payload?.city ?? '',
          description: payload?.description ?? '',
          avatarId: payload?.avatarId == null ? '' : String(payload.avatarId),
        })
      } catch (error) {
        if (!isMounted) {
          return
        }
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, { next: '/profile' })
          return
        }
        if (isApiError(error)) {
          setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить профиль'))
        } else {
          setErrorMessage('Не удалось загрузить профиль')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    if (hasToken) {
      loadProfile()
    } else {
      setIsLoading(false)
    }

    return () => {
      isMounted = false
    }
  }, [hasToken, navigate])

  const handleProfileChange = (event) => {
    const { name, value } = event.target
    setProfileForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSaveProfile = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')

    const username = profileForm.username.trim()
    if (username.length < 2 || username.length > 20) {
      setErrorMessage('Имя пользователя: от 2 до 20 символов')
      return
    }
    if (profileForm.country.trim().length > 100 || profileForm.city.trim().length > 100) {
      setErrorMessage('Страна и город: до 100 символов')
      return
    }
    if (profileForm.description.trim().length > 500) {
      setErrorMessage('О себе: до 500 символов')
      return
    }

    const avatarId = profileForm.avatarId.trim()
    const parsedAvatarId = avatarId ? Number.parseInt(avatarId, 10) : null
    if (avatarId && (!Number.isFinite(parsedAvatarId) || parsedAvatarId <= 0)) {
      setErrorMessage('ID аватара должен быть положительным числом')
      return
    }

    const payload = {
      username,
      dateOfBirth: dateInputToInstant(profileForm.dateOfBirth),
      country: profileForm.country.trim() || null,
      city: profileForm.city.trim() || null,
      description: profileForm.description.trim() || null,
      avatarId: parsedAvatarId,
    }

    setIsSavingProfile(true)
    try {
      const updated = await updateMyProfile(payload)
      setProfile(updated)
      setSuccessMessage('Профиль сохранён')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось сохранить профиль'))
      } else {
        setErrorMessage('Не удалось сохранить профиль')
      }
    } finally {
      setIsSavingProfile(false)
    }
  }

  const handleLogout = async () => {
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await logout()
    } catch {
      // Even if backend logout fails, local token should be removed.
    } finally {
      clearAccessToken()
      navigate('/sign-in')
    }
  }

  const handleDeleteAccount = async () => {
    const ok = window.confirm('Вы уверены, что хотите удалить аккаунт?')
    if (!ok) {
      return
    }
    setIsDeleting(true)
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await deleteMyAccount()
      clearAccessToken()
      navigate('/sign-up')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось удалить аккаунт'))
      } else {
        setErrorMessage('Не удалось удалить аккаунт')
      }
    } finally {
      setIsDeleting(false)
    }
  }

  const sessionEnded = Boolean(hasToken && !isLoading && !profile)

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-shell">
        <div className="profile-shell__column">
          <section className="main-hero">
            <h2>Личный кабинет</h2>
            <h3 className="gray-elem">Настройки профиля и быстрые переходы</h3>
          </section>

          <main className="profile-page">
        {!hasToken ? (
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы открыть личный кабинет.
          </p>
        ) : null}

        {hasToken && isLoading ? <p>Загрузка профиля...</p> : null}
        {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}
        {successMessage ? <p className="profile-success">{successMessage}</p> : null}

        {hasToken && !isLoading && !profile ? (
          <section className="profile-panel profile-session-fallback">
            <h3>Профиль не загрузился</h3>
            <p className="gray-elem">Войдите снова.</p>
          </section>
        ) : null}

        {hasToken && !isLoading && profile ? (
          <div className="profile-grid">
            <section className="profile-panel">
              <h3>Быстрые разделы</h3>
              <div className="profile-links">
                <Link to="/profile/notifications" className="profile-links__action">
                  Настройка уведомлений
                </Link>
                <button type="button" disabled>
                  Мои активности
                </button>
                <button type="button" disabled>
                  Отправленные заявки
                </button>
              </div>
              <p className="gray-elem">Эти разделы будут вынесены на отдельные страницы следующим шагом.</p>
            </section>

            <section className="profile-panel">
              <h3>Персональные данные</h3>
              <p className="gray-elem">Логин (почта): {profile.login}</p>
              <form onSubmit={handleSaveProfile} className="profile-form">
                <label htmlFor="username">Имя пользователя</label>
                <input
                  id="username"
                  name="username"
                  value={profileForm.username}
                  onChange={handleProfileChange}
                  disabled={isSavingProfile}
                  required
                />

                <label htmlFor="dateOfBirth">Дата рождения</label>
                <input
                  id="dateOfBirth"
                  name="dateOfBirth"
                  type="date"
                  value={profileForm.dateOfBirth}
                  onChange={handleProfileChange}
                  disabled={isSavingProfile}
                />

                <div className="split-fields">
                  <div>
                    <label htmlFor="country">Страна</label>
                    <input
                      id="country"
                      name="country"
                      value={profileForm.country}
                      onChange={handleProfileChange}
                      disabled={isSavingProfile}
                    />
                  </div>
                  <div>
                    <label htmlFor="city">Город</label>
                    <input
                      id="city"
                      name="city"
                      value={profileForm.city}
                      onChange={handleProfileChange}
                      disabled={isSavingProfile}
                    />
                  </div>
                </div>

                <label htmlFor="description">О себе</label>
                <textarea
                  id="description"
                  name="description"
                  rows={4}
                  value={profileForm.description}
                  onChange={handleProfileChange}
                  disabled={isSavingProfile}
                />

                <label htmlFor="avatarId">ID аватара (опционально)</label>
                <input
                  id="avatarId"
                  name="avatarId"
                  type="number"
                  min={1}
                  value={profileForm.avatarId}
                  onChange={handleProfileChange}
                  disabled={isSavingProfile}
                />

                <button type="submit" className="create-room-submit" disabled={isSavingProfile}>
                  {isSavingProfile ? 'Сохранение...' : 'Сохранить профиль'}
                </button>
              </form>
              <button
                type="button"
                className="profile-delete-account-link"
                onClick={handleDeleteAccount}
                disabled={isDeleting}
              >
                {isDeleting ? 'Удаление...' : 'Удалить аккаунт'}
              </button>
            </section>
          </div>
        ) : null}
          </main>
        </div>
        <ProfileAuthRail hasToken={hasToken} onLogout={handleLogout} sessionEnded={sessionEnded} />
      </div>
    </>
  )
}

export default ProfilePage
