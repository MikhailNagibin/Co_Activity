import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileAuthRail from '../components/ProfileAuthRail.jsx'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  getMyNotificationSettings,
  logout,
  updateMyNotificationSettings,
} from '../services/profileService.js'

function normalizeNotificationSettings(payload) {
  return {
    membershipAccepted: payload?.membershipAccepted !== false,
    membershipRejected: payload?.membershipRejected !== false,
    activityClosed: payload?.activityClosed !== false,
    newJoinRequest: payload?.newJoinRequest !== false,
    importantRoomUpdates: payload?.importantRoomUpdates !== false,
  }
}

function NotificationSettingsPage() {
  const navigate = useNavigate()
  const { isAuthenticated, clearSession } = useAuthSession()
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingNotifications, setIsSavingNotifications] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [settingsLoaded, setSettingsLoaded] = useState(false)
  const [notificationsForm, setNotificationsForm] = useState({
    membershipAccepted: true,
    membershipRejected: true,
    activityClosed: true,
    newJoinRequest: true,
    importantRoomUpdates: true,
  })

  useEffect(() => {
    let isMounted = true

    const load = async () => {
      setIsLoading(true)
      setErrorMessage('')
      try {
        const payload = await getMyNotificationSettings()
        if (!isMounted) {
          return
        }
        setSettingsLoaded(true)
        setNotificationsForm(normalizeNotificationSettings(payload))
      } catch (error) {
        if (!isMounted) {
          return
        }
        setSettingsLoaded(false)
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, { next: '/profile/notifications' })
          return
        }
        if (isApiError(error)) {
          setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить настройки'))
        } else {
          setErrorMessage('Не удалось загрузить настройки')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    if (isAuthenticated) {
      load()
    } else {
      setIsLoading(false)
    }

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, navigate])

  const notificationsDisabled = useMemo(
    () =>
      !notificationsForm.membershipAccepted &&
      !notificationsForm.membershipRejected &&
      !notificationsForm.activityClosed &&
      !notificationsForm.newJoinRequest &&
      !notificationsForm.importantRoomUpdates,
    [notificationsForm],
  )

  const handleNotificationToggle = (event) => {
    const { name, checked } = event.target
    setNotificationsForm((prev) => ({ ...prev, [name]: checked }))
  }

  const handleSaveNotifications = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')

    const payload = {
      membershipAccepted: notificationsForm.membershipAccepted,
      membershipRejected: notificationsForm.membershipRejected,
      activityClosed: notificationsForm.activityClosed,
      newJoinRequest: notificationsForm.newJoinRequest,
      importantRoomUpdates: notificationsForm.importantRoomUpdates,
    }

    setIsSavingNotifications(true)
    try {
      const savedSettings = await updateMyNotificationSettings(payload)
      setSettingsLoaded(true)
      setNotificationsForm(normalizeNotificationSettings(savedSettings))
      setSuccessMessage(
        notificationsDisabled
          ? 'Все уведомления отключены. По правилам продукта это эквивалентно глобальному отключению.'
          : 'Настройки уведомлений сохранены',
      )
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/notifications' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось сохранить настройки уведомлений'))
      } else {
        setErrorMessage('Не удалось сохранить настройки уведомлений')
      }
    } finally {
      setIsSavingNotifications(false)
    }
  }

  const handleLogout = async () => {
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await logout()
    } catch {
      // Even if backend logout fails, local session state should be reset.
    } finally {
      clearSession()
      navigate('/sign-in')
    }
  }

  const sessionEnded = false

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-shell">
        <div className="profile-shell__column">
          <section className="main-hero">
            <h2>Настройки уведомлений</h2>
            <h3 className="gray-elem">
              <Link to="/profile" className="notification-settings-back">
                ← Личный кабинет
              </Link>
            </h3>
          </section>

          <main className="profile-page">
        {!isAuthenticated ? (
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы изменить уведомления.
          </p>
        ) : null}

        {isAuthenticated && isLoading ? <p>Загрузка...</p> : null}
        {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}
        {successMessage ? <p className="profile-success">{successMessage}</p> : null}

        {isAuthenticated && !isLoading && !settingsLoaded ? (
          <section className="profile-panel profile-session-fallback">
            <h3>Настройки не загрузились</h3>
            <p className="gray-elem">Войдите снова.</p>
          </section>
        ) : null}

        {isAuthenticated && !isLoading && settingsLoaded ? (
          <section className="profile-panel notification-settings-panel">
            <form onSubmit={handleSaveNotifications} className="profile-form">
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="membershipAccepted"
                  checked={notificationsForm.membershipAccepted}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                Заявка на вступление принята
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="membershipRejected"
                  checked={notificationsForm.membershipRejected}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                Заявка на вступление отклонена
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="activityClosed"
                  checked={notificationsForm.activityClosed}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                Активность, в которой я состою, закрылась
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="newJoinRequest"
                  checked={notificationsForm.newJoinRequest}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                Новая заявка на вступление в мою активность
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="importantRoomUpdates"
                  checked={notificationsForm.importantRoomUpdates}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                Важные обновления по активностям
              </label>

              <p className="gray-elem notification-settings-hint">
                Если выключить все пункты, это будет считаться полным отключением уведомлений.
              </p>

              <button type="submit" className="create-room-submit" disabled={isSavingNotifications}>
                {isSavingNotifications ? 'Сохранение...' : 'Сохранить'}
              </button>
            </form>
          </section>
        ) : null}
          </main>
        </div>
        <ProfileAuthRail hasToken={isAuthenticated} onLogout={handleLogout} sessionEnded={sessionEnded} />
      </div>
    </>
  )
}

export default NotificationSettingsPage
