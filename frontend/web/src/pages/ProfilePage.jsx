import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import { isApiError } from '../api/httpClient.js'
import { changePassword } from '../services/authService.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInAfterPasswordChange,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  deleteMyAvatar,
  deleteMyAccount,
  deleteMyAccountWithActions,
  getMyDeletionPreview,
  getMyProfile,
  logout,
  uploadMyAvatar,
  updateMyProfile,
} from '../services/profileService.js'
import { resolveUserAvatarUrl, resolveUserName, withCacheBust } from '../utils/userProfile.js'
import { useConfirmDialog } from '../hooks/useConfirmDialog.jsx'

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

function buildProfileForm(payload) {
  return {
    username: payload?.username ?? payload?.userName ?? '',
    dateOfBirth: instantToInputDate(payload?.dateOfBirth),
    country: payload?.country ?? '',
    city: payload?.city ?? '',
    description: payload?.description ?? '',
  }
}

function ProfilePage() {
  const navigate = useNavigate()
  const { isAuthenticated, clearSession } = useAuthSession()
  const { requestConfirm, confirmDialog } = useConfirmDialog()
  const passwordErrorRef = useRef(null)
  const avatarInputRef = useRef(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isLoadingDeletionPreview, setIsLoadingDeletionPreview] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [passwordErrorMessage, setPasswordErrorMessage] = useState('')
  const [passwordInvalidFields, setPasswordInvalidFields] = useState({})
  const [profile, setProfile] = useState(null)
  const [selectedAvatarFile, setSelectedAvatarFile] = useState(null)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState('')
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false)
  const [isDeletingAvatar, setIsDeletingAvatar] = useState(false)
  const [avatarVersion, setAvatarVersion] = useState(0)
  const [deletionPreview, setDeletionPreview] = useState(null)
  const [deletionModes, setDeletionModes] = useState({})
  const [deletionTransfers, setDeletionTransfers] = useState({})
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [profileForm, setProfileForm] = useState({
    username: '',
    dateOfBirth: '',
    country: '',
    city: '',
    description: '',
  })

  const applyProfilePayload = useCallback((payload) => {
    setProfile(payload)
    setProfileForm(buildProfileForm(payload))
  }, [])

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
        applyProfilePayload(payload)
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
  }, [applyProfilePayload, isAuthenticated, navigate])

  useEffect(() => {
    if (!passwordErrorMessage) {
      return
    }
    passwordErrorRef.current?.focus()
  }, [passwordErrorMessage])

  useEffect(() => {
    return () => {
      if (avatarPreviewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarPreviewUrl)
      }
    }
  }, [avatarPreviewUrl])

  const handleProfileChange = (event) => {
    const { name, value, type, checked } = event.target
    setProfileForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handlePasswordFieldChange = (event) => {
    const { name, value } = event.target
    setPasswordForm((prev) => ({ ...prev, [name]: value }))
    setPasswordInvalidFields((prev) => {
      if (!prev[name]) {
        return prev
      }
      return { ...prev, [name]: false }
    })
  }

  const showPasswordError = (message, fields = {}) => {
    setPasswordInvalidFields(fields)
    setPasswordErrorMessage(message)
  }

  const resetSelectedAvatar = () => {
    setSelectedAvatarFile(null)
    setAvatarPreviewUrl('')
    if (avatarInputRef.current) {
      avatarInputRef.current.value = ''
    }
  }

  const handleAvatarSelection = (event) => {
    const file = event.target.files?.[0] ?? null
    if (!file) {
      resetSelectedAvatar()
      return
    }

    const allowedMimeTypes = new Set(['image/png', 'image/jpeg', 'image/webp'])
    if (file.type && !allowedMimeTypes.has(file.type)) {
      resetSelectedAvatar()
      setErrorMessage('Аватар должен быть в формате PNG, JPEG или WEBP')
      return
    }

    setErrorMessage('')
    setSelectedAvatarFile(file)
    setAvatarPreviewUrl(URL.createObjectURL(file))
  }

  const handleUploadAvatar = async () => {
    if (!selectedAvatarFile) {
      setErrorMessage('Сначала выберите файл аватара')
      return
    }

    setErrorMessage('')
    setIsUploadingAvatar(true)
    try {
      const updated = await uploadMyAvatar(selectedAvatarFile)
      applyProfilePayload(updated)
      resetSelectedAvatar()
      setAvatarVersion((prev) => prev + 1)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить аватар'))
      } else {
        setErrorMessage('Не удалось загрузить аватар')
      }
    } finally {
      setIsUploadingAvatar(false)
    }
  }

  const handleDeleteAvatar = async () => {
    setErrorMessage('')
    setIsDeletingAvatar(true)
    try {
      await deleteMyAvatar()
      resetSelectedAvatar()
      setProfile((prev) =>
        prev
          ? {
              ...prev,
              avatarId: null,
              avatarUrl: null,
            }
          : prev,
      )
      setAvatarVersion((prev) => prev + 1)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось удалить аватар'))
      } else {
        setErrorMessage('Не удалось удалить аватар')
      }
    } finally {
      setIsDeletingAvatar(false)
    }
  }

  const handleSaveProfile = async (event) => {
    event.preventDefault()
    setErrorMessage('')

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

    const payload = {
      username,
      dateOfBirth: dateInputToInstant(profileForm.dateOfBirth),
      country: profileForm.country.trim() || null,
      city: profileForm.city.trim() || null,
      description: profileForm.description.trim() || null,
    }

    setIsSavingProfile(true)
    try {
      const updated = await updateMyProfile(payload)
      applyProfilePayload(updated)
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

  const handleChangePassword = async (event) => {
    event.preventDefault()
    setPasswordErrorMessage('')
    setPasswordInvalidFields({})

    if (passwordForm.currentPassword.length < 8) {
      showPasswordError('Текущий пароль: от 8 символов', { currentPassword: true })
      return
    }
    if (passwordForm.newPassword.length < 8) {
      showPasswordError('Новый пароль: от 8 символов', { newPassword: true })
      return
    }
    if (passwordForm.confirmPassword !== passwordForm.newPassword) {
      showPasswordError('Новые пароли не совпадают', { confirmPassword: true })
      return
    }

    setIsChangingPassword(true)
    try {
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      })
      clearSession()
      redirectToSignInAfterPasswordChange(navigate, {
        next: '/profile',
        email: profile?.email,
      })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile' })
        return
      }
      if (isApiError(error)) {
        const rawMessage = String(error.message ?? '').toLowerCase()
        showPasswordError(getUserFacingApiMessage(error, 'Не удалось сменить пароль'), {
          currentPassword: rawMessage.includes('current password'),
          newPassword: rawMessage.includes('new password'),
        })
      } else {
        showPasswordError('Не удалось сменить пароль', { newPassword: true })
      }
    } finally {
      setIsChangingPassword(false)
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
    setIsLoadingDeletionPreview(true)
    try {
      const preview = await getMyDeletionPreview()
      if (preview?.canDeleteImmediately) {
        const ok = await requestConfirm({
          title: 'Удаление аккаунта',
          message:
            'У аккаунта нет активностей во владении. Удалить аккаунт без возможности восстановления?',
          confirmLabel: 'Удалить',
          cancelLabel: 'Отмена',
          variant: 'danger',
        })
        if (!ok) {
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

    const ok = await requestConfirm({
      title: 'Удаление аккаунта',
      message:
        'Аккаунт и выбранные изменения по вашим активностям будут удалены без возможности восстановления. Продолжить?',
      confirmLabel: 'Удалить',
      cancelLabel: 'Отмена',
      variant: 'danger',
    })
    if (!ok) {
      return
    }

    await executeAccountDeletion(() => deleteMyAccountWithActions({ actions }))
  }

  const sessionEnded = Boolean(isAuthenticated && !isLoading && !profile)
  const savedAvatarUrl = resolveUserAvatarUrl(profile)
  const displayedAvatarUrl =
    avatarPreviewUrl || (savedAvatarUrl ? withCacheBust(savedAvatarUrl, avatarVersion) : '')
  const hasSavedAvatar = Boolean(savedAvatarUrl)
  const profileDisplayName = resolveUserName(profile, 'Пользователь')

  return (
    <>
      <ProfileCabinetShell
        heroTitle="Личный кабинет"
        heroSubtitle="Данные аккаунта, настройки сайта и безопасность"
        sessionEnded={sessionEnded}
        onLogout={handleLogout}
        showCabinetNav={isAuthenticated}
      >
        {!isAuthenticated ? (
          <main className="profile-page">
            <p className="create-room-hint">
              <Link to="/sign-in">Войдите</Link>, чтобы открыть личный кабинет.
            </p>
          </main>
        ) : null}

        {isAuthenticated ? (
          <main className="profile-page">
            {isLoading ? <p>Загрузка профиля...</p> : null}
            {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}

            {!isLoading && !profile ? (
              <section className="profile-panel profile-session-fallback">
                <h3>Профиль не загрузился</h3>
                <p className="gray-elem">Войдите снова.</p>
              </section>
            ) : null}

            {!isLoading && profile ? (
              <div className="profile-grid">
                <section className="profile-panel profile-panel--overview">
                  <div className="profile-overview">
                    <div className="profile-overview__avatar">
                      {displayedAvatarUrl ? (
                        <img
                          className="profile-overview__avatar-image"
                          src={displayedAvatarUrl}
                          alt={`Аватар пользователя ${profileDisplayName}`}
                        />
                      ) : (
                        <i className="fa-regular fa-circle-user"></i>
                      )}
                    </div>
                    <div className="profile-overview__body">
                      <p className="profile-kicker">Профиль</p>
                      <h3>{profileDisplayName}</h3>
                      <p className="gray-elem">{profile.email}</p>
                      <div className="profile-avatar-meta">
                        {selectedAvatarFile ? (
                          <span>Выбран файл: {selectedAvatarFile.name}</span>
                        ) : hasSavedAvatar ? (
                          <span>Текущий аватар активен</span>
                        ) : (
                          <span>Аватар не загружен</span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="profile-summary-grid">
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
                  <div className="profile-avatar-actions">
                    <label className="profile-avatar-upload" htmlFor="avatarFile">
                      <span>Выбрать аватар</span>
                      <input
                        ref={avatarInputRef}
                        id="avatarFile"
                        type="file"
                        accept="image/png,image/jpeg,image/webp"
                        onChange={handleAvatarSelection}
                        disabled={isUploadingAvatar || isDeletingAvatar}
                      />
                    </label>
                    <button
                      type="button"
                      className="create-room-submit profile-avatar-submit"
                      onClick={handleUploadAvatar}
                      disabled={!selectedAvatarFile || isUploadingAvatar || isDeletingAvatar}
                    >
                      {isUploadingAvatar ? 'Загрузка...' : 'Загрузить аватар'}
                    </button>
                    {selectedAvatarFile ? (
                      <button
                        type="button"
                        className="profile-delete-account-link"
                        onClick={resetSelectedAvatar}
                        disabled={isUploadingAvatar || isDeletingAvatar}
                      >
                        Сбросить выбор
                      </button>
                    ) : null}
                    {hasSavedAvatar ? (
                      <button
                        type="button"
                        className="profile-delete-account-link"
                        onClick={handleDeleteAvatar}
                        disabled={isUploadingAvatar || isDeletingAvatar}
                      >
                        {isDeletingAvatar ? 'Удаление...' : 'Удалить аватар'}
                      </button>
                    ) : null}
                  </div>
                </section>

                <section className="profile-panel">
                  <p className="profile-kicker">Редактирование</p>
                  <h3>Персональные данные</h3>
                  <p className="gray-elem">Управляйте публичной информацией и описанием аккаунта.</p>
                  <form onSubmit={handleSaveProfile} className="profile-form">
                    <div className="profile-form-row">
                      <label htmlFor="username">Имя пользователя</label>
                      <input
                        id="username"
                        name="username"
                        value={profileForm.username}
                        onChange={handleProfileChange}
                        disabled={isSavingProfile}
                        required
                      />
                    </div>

                    <div className="profile-form-row">
                      <label htmlFor="dateOfBirth">Дата рождения</label>
                      <input
                        id="dateOfBirth"
                        name="dateOfBirth"
                        type="date"
                        value={profileForm.dateOfBirth}
                        onChange={handleProfileChange}
                        disabled={isSavingProfile}
                      />
                    </div>

                    <div className="auth-grid auth-grid--two">
                      <div className="profile-form-row">
                        <label htmlFor="country">Страна</label>
                        <input
                          id="country"
                          name="country"
                          value={profileForm.country}
                          onChange={handleProfileChange}
                          disabled={isSavingProfile}
                        />
                      </div>
                      <div className="profile-form-row">
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

                    <div className="profile-form-row">
                      <label htmlFor="description">О себе</label>
                      <textarea
                        id="description"
                        name="description"
                        rows={4}
                        value={profileForm.description}
                        onChange={handleProfileChange}
                        disabled={isSavingProfile}
                      />
                    </div>
                    <button type="submit" className="create-room-submit" disabled={isSavingProfile}>
                      {isSavingProfile ? 'Сохранение...' : 'Сохранить профиль'}
                    </button>
                  </form>
                </section>

                <section className="profile-panel profile-panel--danger">
                  <p className="profile-kicker">Безопасность</p>
                  <h3>Пароль и удаление аккаунта</h3>
                  <p className="gray-elem">
                    Смените пароль в текущей сессии или удалите аккаунт с планом для ваших
                    активностей.
                  </p>

                  <section className="profile-security-section">
                    <h4>Смена пароля</h4>
                    <form onSubmit={handleChangePassword} className="profile-form">
                      <div className="profile-form-row">
                        <label htmlFor="currentPassword">Текущий пароль</label>
                        <input
                          id="currentPassword"
                          name="currentPassword"
                          type="password"
                          value={passwordForm.currentPassword}
                          onChange={handlePasswordFieldChange}
                          disabled={isChangingPassword}
                          autoComplete="current-password"
                          aria-invalid={passwordInvalidFields.currentPassword ? 'true' : 'false'}
                          aria-describedby={passwordErrorMessage ? 'profile-password-error' : undefined}
                        />
                      </div>

                      <div className="profile-form-row">
                        <label htmlFor="newPassword">Новый пароль</label>
                        <input
                          id="newPassword"
                          name="newPassword"
                          type="password"
                          value={passwordForm.newPassword}
                          onChange={handlePasswordFieldChange}
                          disabled={isChangingPassword}
                          autoComplete="new-password"
                          aria-invalid={passwordInvalidFields.newPassword ? 'true' : 'false'}
                          aria-describedby={passwordErrorMessage ? 'profile-password-error' : undefined}
                        />
                      </div>

                      <div className="profile-form-row">
                        <label htmlFor="confirmPassword">Повторите новый пароль</label>
                        <input
                          id="confirmPassword"
                          name="confirmPassword"
                          type="password"
                          value={passwordForm.confirmPassword}
                          onChange={handlePasswordFieldChange}
                          disabled={isChangingPassword}
                          autoComplete="new-password"
                          aria-invalid={passwordInvalidFields.confirmPassword ? 'true' : 'false'}
                          aria-describedby={passwordErrorMessage ? 'profile-password-error' : undefined}
                        />
                      </div>

                      {passwordErrorMessage ? (
                        <p
                          id="profile-password-error"
                          ref={passwordErrorRef}
                          tabIndex={-1}
                          className="auth-banner auth-banner--error"
                        >
                          {passwordErrorMessage}
                        </p>
                      ) : null}

                      <button
                        type="submit"
                        className="auth-submit-button profile-security-submit"
                        disabled={isChangingPassword}
                      >
                        {isChangingPassword ? 'Сохранение...' : 'Сменить пароль'}
                      </button>
                    </form>
                  </section>

                  <section className="profile-security-section">
                    <h4>Удаление аккаунта</h4>
                    <p className="gray-elem">
                      Перед удалением можно решить, что делать с активностями, где вы владелец.
                    </p>
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
                </section>
              </div>
            ) : null}
          </main>
        ) : null}
      </ProfileCabinetShell>
      {confirmDialog}
    </>
  )
}

export default ProfilePage
