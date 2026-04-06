import { useEffect, useState } from 'react'
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
  deleteMyAccount,
  deleteMyAccountWithActions,
  getMyDeletionPreview,
  getMyProfile,
  logout,
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

function buildInitialDeletionPlan(preview) {
  const modes = {}
  const transfers = {}

  for (const room of preview?.ownedRooms ?? []) {
    const firstCandidateId = room.transferCandidates?.[0]?.userId ?? null
    const defaultMode = firstCandidateId ? 'TRANSFER_OWNERSHIP' : 'DELETE_ROOM'
    modes[room.roomId] = defaultMode
    transfers[room.roomId] = firstCandidateId == null ? '' : String(firstCandidateId)
  }

  return { modes, transfers }
}

function getRoleLabel(role) {
  switch (role) {
    case 'ADMIN':
      return 'администратор'
    case 'PARTICIPANT':
      return 'участник'
    default:
      return 'участник'
  }
}

function ProfilePage() {
  const navigate = useNavigate()
  const { isAuthenticated, clearSession } = useAuthSession()
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isLoadingDeletionPreview, setIsLoadingDeletionPreview] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [profile, setProfile] = useState(null)
  const [deletionPreview, setDeletionPreview] = useState(null)
  const [deletionModes, setDeletionModes] = useState({})
  const [deletionTransfers, setDeletionTransfers] = useState({})
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

    if (isAuthenticated) {
      loadProfile()
    } else {
      setIsLoading(false)
    }

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, navigate])

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
      // Even if backend logout fails, local session state should be reset.
    } finally {
      clearSession()
      navigate('/sign-in')
    }
  }

  const resetDeletionPlanner = () => {
    setDeletionPreview(null)
    setDeletionModes({})
    setDeletionTransfers({})
  }

  const finishDeletedAccount = () => {
    clearSession()
    resetDeletionPlanner()
    navigate('/sign-up')
  }

  const handleDeleteError = (error) => {
    if (isUnauthorizedApiError(error)) {
      redirectToSignInForExpiredSession(navigate, { next: '/profile' })
      return
    }
    if (isApiError(error)) {
      setErrorMessage(getUserFacingApiMessage(error, 'Не удалось удалить аккаунт'))
      return
    }
    setErrorMessage('Не удалось удалить аккаунт')
  }

  const executeAccountDeletion = async (requestFactory) => {
    setIsDeleting(true)
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await requestFactory()
      finishDeletedAccount()
    } catch (error) {
      handleDeleteError(error)
    } finally {
      setIsDeleting(false)
    }
  }

  const handlePrepareDeletion = async () => {
    setErrorMessage('')
    setSuccessMessage('')
    setIsLoadingDeletionPreview(true)
    try {
      const preview = await getMyDeletionPreview()
      if (preview?.canDeleteImmediately) {
        const confirmed = window.confirm(
          'У аккаунта нет активностей во владении. Удалить аккаунт без возможности восстановления?',
        )
        if (!confirmed) {
          return
        }
        await executeAccountDeletion(() => deleteMyAccount())
        return
      }

      const plan = buildInitialDeletionPlan(preview)
      setDeletionPreview(preview)
      setDeletionModes(plan.modes)
      setDeletionTransfers(plan.transfers)
    } catch (error) {
      handleDeleteError(error)
    } finally {
      setIsLoadingDeletionPreview(false)
    }
  }

  const handleDeletionModeChange = (roomId, mode) => {
    setDeletionModes((prev) => ({ ...prev, [roomId]: mode }))
  }

  const handleDeletionTransferChange = (roomId, nextUserId) => {
    setDeletionTransfers((prev) => ({ ...prev, [roomId]: nextUserId }))
  }

  const handleDeleteWithPlan = async () => {
    if (!deletionPreview?.ownedRooms?.length) {
      return
    }

    const actions = []
    for (const room of deletionPreview.ownedRooms) {
      const mode = deletionModes[room.roomId] ?? 'DELETE_ROOM'
      if (mode === 'TRANSFER_OWNERSHIP') {
        const selectedUserId = Number.parseInt(deletionTransfers[room.roomId] ?? '', 10)
        if (!Number.isFinite(selectedUserId) || selectedUserId <= 0) {
          setErrorMessage(`Выберите нового владельца для активности «${room.roomName}»`)
          return
        }
        actions.push({
          roomId: room.roomId,
          mode,
          transferToUserId: selectedUserId,
        })
        continue
      }

      actions.push({
        roomId: room.roomId,
        mode: 'DELETE_ROOM',
      })
    }

    const confirmed = window.confirm(
      'Аккаунт и выбранные изменения по вашим активностям будут удалены без возможности восстановления. Продолжить?',
    )
    if (!confirmed) {
      return
    }

    await executeAccountDeletion(() => deleteMyAccountWithActions({ actions }))
  }

  const sessionEnded = Boolean(isAuthenticated && !isLoading && !profile)

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
            {!isAuthenticated ? (
              <p className="create-room-hint">
                <Link to="/sign-in">Войдите</Link>, чтобы открыть личный кабинет.
              </p>
            ) : null}

            {isAuthenticated && isLoading ? <p>Загрузка профиля...</p> : null}
            {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}
            {successMessage ? <p className="profile-success">{successMessage}</p> : null}

            {isAuthenticated && !isLoading && !profile ? (
              <section className="profile-panel profile-session-fallback">
                <h3>Профиль не загрузился</h3>
                <p className="gray-elem">Войдите снова.</p>
              </section>
            ) : null}

            {isAuthenticated && !isLoading && profile ? (
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
                  <p className="gray-elem">
                    Эти разделы будут вынесены на отдельные страницы следующим шагом.
                  </p>
                </section>

                <section className="profile-panel">
                  <h3>Персональные данные</h3>
                  <p className="gray-elem">Почта: {profile.email}</p>
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

                  <div className="profile-actions">
                    <button
                      type="button"
                      className="profile-delete-btn"
                      onClick={handlePrepareDeletion}
                      disabled={isDeleting || isLoadingDeletionPreview}
                    >
                      {isLoadingDeletionPreview
                        ? 'Проверка...'
                        : deletionPreview?.ownedRooms?.length
                          ? 'Обновить план удаления'
                          : 'Удалить аккаунт'}
                    </button>

                    {deletionPreview?.ownedRooms?.length ? (
                      <section className="profile-deletion-plan">
                        <h4>Перед удалением решите, что делать с вашими активностями</h4>
                        <p className="gray-elem">
                          Для каждой активности выберите удаление или передачу владельца.
                        </p>

                        <div className="profile-deletion-rooms">
                          {deletionPreview.ownedRooms.map((room) => {
                            const selectedMode = deletionModes[room.roomId] ?? 'DELETE_ROOM'
                            const hasTransferCandidates = (room.transferCandidates?.length ?? 0) > 0
                            return (
                              <article key={room.roomId} className="profile-deletion-room">
                                <div className="profile-deletion-room__header">
                                  <strong>{room.roomName}</strong>
                                  <span className="gray-elem">
                                    Участников: {room.participantCount}
                                  </span>
                                </div>

                                <label className="profile-delete-option">
                                  <input
                                    type="radio"
                                    name={`deletion-mode-${room.roomId}`}
                                    value="DELETE_ROOM"
                                    checked={selectedMode === 'DELETE_ROOM'}
                                    onChange={() =>
                                      handleDeletionModeChange(room.roomId, 'DELETE_ROOM')
                                    }
                                    disabled={isDeleting}
                                  />
                                  Удалить активность вместе с аккаунтом
                                </label>

                                <label className="profile-delete-option">
                                  <input
                                    type="radio"
                                    name={`deletion-mode-${room.roomId}`}
                                    value="TRANSFER_OWNERSHIP"
                                    checked={selectedMode === 'TRANSFER_OWNERSHIP'}
                                    onChange={() =>
                                      handleDeletionModeChange(room.roomId, 'TRANSFER_OWNERSHIP')
                                    }
                                    disabled={isDeleting || !hasTransferCandidates}
                                  />
                                  Передать владельца другому участнику
                                </label>

                                {!hasTransferCandidates ? (
                                  <p className="gray-elem">
                                    В этой активности нет другого участника. Доступно только удаление.
                                  </p>
                                ) : null}

                                {hasTransferCandidates && selectedMode === 'TRANSFER_OWNERSHIP' ? (
                                  <select
                                    value={deletionTransfers[room.roomId] ?? ''}
                                    onChange={(event) =>
                                      handleDeletionTransferChange(room.roomId, event.target.value)
                                    }
                                    disabled={isDeleting}
                                  >
                                    <option value="">Выберите нового владельца</option>
                                    {room.transferCandidates.map((candidate) => (
                                      <option key={candidate.userId} value={candidate.userId}>
                                        {candidate.userName} ({getRoleLabel(candidate.role)})
                                      </option>
                                    ))}
                                  </select>
                                ) : null}
                              </article>
                            )
                          })}
                        </div>

                        <button
                          type="button"
                          className="profile-delete-btn"
                          onClick={handleDeleteWithPlan}
                          disabled={isDeleting}
                        >
                          {isDeleting ? 'Удаление...' : 'Подтвердить удаление аккаунта'}
                        </button>

                        <button
                          type="button"
                          className="profile-delete-account-link"
                          onClick={resetDeletionPlanner}
                          disabled={isDeleting}
                        >
                          Скрыть план удаления
                        </button>
                      </section>
                    ) : null}
                  </div>
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

export default ProfilePage
