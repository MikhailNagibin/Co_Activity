import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import { clearAccessToken, getAccessToken } from '../api/tokenStorage.js'
import { ApiError } from '../api/httpClient.js'
import {
  deleteMyAccount,
  getMyProfile,
  logout,
  updateMyNotificationSettings,
  updateMyProfile,
} from '../services/profileService.js'

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
  const [isSavingNotifications, setIsSavingNotifications] = useState(false)
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
  const [notificationsForm, setNotificationsForm] = useState({
    membershipAccepted: true,
    membershipRejected: true,
    activityClosed: true,
    newJoinRequest: true,
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
        const enabled = Array.isArray(payload?.notifications) ? payload.notifications : []
        const has = (key) => enabled.some((item) => String(item) === key)
        setNotificationsForm({
          membershipAccepted: has('MembershipAccepted'),
          membershipRejected: has('MembershipRejected'),
          activityClosed: has('ActivityClosed'),
          newJoinRequest: has('NewJoinRequest'),
        })
      } catch (error) {
        if (!isMounted) {
          return
        }
        if (error instanceof ApiError) {
          setErrorMessage(error.message)
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
  }, [hasToken])

  const notificationsDisabled = useMemo(
    () =>
      !notificationsForm.membershipAccepted &&
      !notificationsForm.membershipRejected &&
      !notificationsForm.activityClosed &&
      !notificationsForm.newJoinRequest,
    [notificationsForm],
  )

  const handleProfileChange = (event) => {
    const { name, value } = event.target
    setProfileForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleNotificationToggle = (event) => {
    const { name, checked } = event.target
    setNotificationsForm((prev) => ({ ...prev, [name]: checked }))
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
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
      } else {
        setErrorMessage('Не удалось сохранить профиль')
      }
    } finally {
      setIsSavingProfile(false)
    }
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
    }

    setIsSavingNotifications(true)
    try {
      await updateMyNotificationSettings(payload)
      setSuccessMessage(
        notificationsDisabled
          ? 'Все уведомления отключены'
          : 'Настройки уведомлений сохранены',
      )
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
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
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
      } else {
        setErrorMessage('Не удалось удалить аккаунт')
      }
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <>
      <AppHeader activeTab={null} />
      <section className="main-hero">
        <h2>Личный кабинет</h2>
        <h3 className="gray-elem">Настройки профиля, уведомлений и быстрые переходы</h3>
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

        {hasToken && !isLoading && profile ? (
          <div className="profile-grid">
            <section className="profile-panel">
              <h3>Быстрые разделы</h3>
              <div className="profile-links">
                <button type="button" disabled>
                  Уведомления
                </button>
                <button type="button" disabled>
                  Персональные данные
                </button>
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
            </section>

            <section className="profile-panel">
              <h3>Уведомления</h3>
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

                <button type="submit" className="create-room-submit" disabled={isSavingNotifications}>
                  {isSavingNotifications ? 'Сохранение...' : 'Сохранить уведомления'}
                </button>
              </form>
            </section>

            <section className="profile-panel">
              <h3>Действия с аккаунтом</h3>
              <div className="profile-actions">
                <button type="button" className="create-room-submit" onClick={handleLogout}>
                  Выйти из аккаунта
                </button>
                <button
                  type="button"
                  className="profile-delete-btn"
                  onClick={handleDeleteAccount}
                  disabled={isDeleting}
                >
                  {isDeleting ? 'Удаление...' : 'Удалить аккаунт'}
                </button>
              </div>
            </section>
          </div>
        ) : null}
      </main>
    </>
  )
}

export default ProfilePage
