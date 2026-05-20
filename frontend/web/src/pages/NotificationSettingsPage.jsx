import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  getMyProfile,
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
  const [settingsLoaded, setSettingsLoaded] = useState(false)
  const [accountEmail, setAccountEmail] = useState('')
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
        const [payload, profilePayload] = await Promise.all([
          getMyNotificationSettings(),
          getMyProfile(),
        ])
        if (!isMounted) {
          return
        }
        setSettingsLoaded(true)
        setNotificationsForm(normalizeNotificationSettings(payload))
        setAccountEmail(String(profilePayload?.email ?? '').trim())
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

  const areAllNotificationsEnabled = useMemo(
    () =>
      notificationsForm.membershipAccepted &&
      notificationsForm.membershipRejected &&
      notificationsForm.activityClosed &&
      notificationsForm.newJoinRequest &&
      notificationsForm.importantRoomUpdates,
    [notificationsForm],
  )

  const handleNotificationToggle = (event) => {
    const { name, checked } = event.target
    setNotificationsForm((prev) => ({ ...prev, [name]: checked }))
  }

  const handleToggleAllNotifications = () => {
    const nextEnabled = notificationsDisabled
    setNotificationsForm({
      membershipAccepted: nextEnabled,
      membershipRejected: nextEnabled,
      activityClosed: nextEnabled,
      newJoinRequest: nextEnabled,
      importantRoomUpdates: nextEnabled,
    })
  }

  const handleSaveNotifications = async (event) => {
    event.preventDefault()
    setErrorMessage('')

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
      <ProfileCabinetShell
        heroTitle="Настройки уведомлений"
        heroSubtitle="Выберите, по каким событиям приходят письма на адрес из вашего профиля."
        onLogout={handleLogout}
        sessionEnded={sessionEnded}
      >
        <main className="profile-page">
          {!isAuthenticated ? (
            <p className="create-room-hint">
              <Link to="/sign-in">Войдите</Link>, чтобы изменить уведомления.
            </p>
          ) : null}

          {isAuthenticated && isLoading ? <p>Загрузка...</p> : null}
          {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}

          {isAuthenticated && !isLoading && !settingsLoaded ? (
            <section className="profile-panel profile-session-fallback">
              <h3>Настройки не загрузились</h3>
              <p className="gray-elem">Войдите снова.</p>
            </section>
          ) : null}

          {isAuthenticated && !isLoading && settingsLoaded ? (
            <section className="profile-panel notification-settings-panel">
            <form onSubmit={handleSaveNotifications} className="profile-form">
              <div className="notification-settings-summary">
                <p className="profile-kicker">Почта для уведомлений</p>
                <strong>{accountEmail || 'Почта не найдена'}</strong>
              </div>

              <button
                type="button"
                className="notification-settings-master"
                onClick={handleToggleAllNotifications}
                disabled={isSavingNotifications}
              >
                {notificationsDisabled
                  ? 'Включить все уведомления'
                  : areAllNotificationsEnabled
                    ? 'Отключить все уведомления'
                    : 'Переключить все уведомления'}
              </button>

              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="membershipAccepted"
                  checked={notificationsForm.membershipAccepted}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                <span className="notification-settings-option">
                  <span>Заявка на вступление принята</span>
                </span>
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="membershipRejected"
                  checked={notificationsForm.membershipRejected}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                <span className="notification-settings-option">
                  <span>Заявка на вступление отклонена</span>
                </span>
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="activityClosed"
                  checked={notificationsForm.activityClosed}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                <span className="notification-settings-option">
                  <span>Активность, в которой я состою, закрылась</span>
                </span>
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="newJoinRequest"
                  checked={notificationsForm.newJoinRequest}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                <span className="notification-settings-option">
                  <span>Новая заявка на вступление в мою активность</span>
                </span>
              </label>
              <label className="profile-checkbox">
                <input
                  type="checkbox"
                  name="importantRoomUpdates"
                  checked={notificationsForm.importantRoomUpdates}
                  onChange={handleNotificationToggle}
                  disabled={isSavingNotifications}
                />
                <span className="notification-settings-option">
                  <span>Важные обновления по активностям</span>
                </span>
              </label>

              <button type="submit" className="create-room-submit" disabled={isSavingNotifications}>
                {isSavingNotifications ? 'Сохранение...' : 'Сохранить'}
              </button>
            </form>
            </section>
          ) : null}
        </main>
      </ProfileCabinetShell>
    </>
  )
}

export default NotificationSettingsPage
