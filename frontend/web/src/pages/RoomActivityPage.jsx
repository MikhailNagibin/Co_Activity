import AppHeader from '../components/AppHeader.jsx'
import { useCallback, useEffect, useState } from 'react'
import { useAuthSession } from '../auth/authSessionContext.js'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  deleteRoom,
  getRoomById,
  getRoomMembershipStatus,
  joinRoom,
  leaveRoom,
  updateRoomBulletin,
} from '../services/roomsService.js'
import {
  formatDate,
  getRoomMembershipView,
  normalizeBulletinContent,
} from '../services/uiMappers.js'
import { resolveUserName } from '../utils/userProfile.js'

function formatCategory(category) {
  if (category == null) {
    return '—'
  }
  if (typeof category === 'string') {
    return category
  }
  if (typeof category === 'object' && category.name != null) {
    return String(category.name)
  }
  return String(category)
}

function formatDateTime(value) {
  if (value == null || value === '') {
    return '—'
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }
  return parsed.toLocaleString('ru-RU', { dateStyle: 'short', timeStyle: 'short' })
}

function getRoleLabel(role) {
  switch (String(role ?? '').toUpperCase()) {
    case 'OWNER':
      return 'Создатель'
    case 'ADMIN':
      return 'Админ'
    case 'PARTICIPANT':
      return 'Участник'
    default:
      return ''
  }
}

function RoomActivityPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const { roomId: roomIdParam } = useParams()
  const roomId = Number.parseInt(String(roomIdParam), 10)

  const [room, setRoom] = useState(null)
  const [membership, setMembership] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [membershipLoadError, setMembershipLoadError] = useState('')
  const [joinSubmitting, setJoinSubmitting] = useState(false)
  const [leaveSubmitting, setLeaveSubmitting] = useState(false)
  const [deleteSubmitting, setDeleteSubmitting] = useState(false)
  const [joinFeedback, setJoinFeedback] = useState('')
  const [joinFeedbackTone, setJoinFeedbackTone] = useState('success')
  const [bulletinEditing, setBulletinEditing] = useState(false)
  const [bulletinDraft, setBulletinDraft] = useState('')
  const [bulletinSaving, setBulletinSaving] = useState(false)
  const [bulletinFeedback, setBulletinFeedback] = useState('')

  const loadRoom = useCallback(async (options = {}) => {
    const { preserveJoinFeedback = false } = options

    if (!Number.isFinite(roomId) || roomId < 1) {
      setErrorMessage('Некорректный идентификатор активности')
      setRoom(null)
      setIsLoading(false)
      return false
    }

    setIsLoading(true)
    setErrorMessage('')
    setMembershipLoadError('')
    if (!preserveJoinFeedback) {
      setJoinFeedback('')
    }
    setMembership(null)

    try {
      const payload = await getRoomById(roomId)
      setRoom(payload)

      if (!isAuthenticated) {
        setMembership(payload?.membershipStatus ?? null)
        return true
      }

      try {
        const membershipPayload = await getRoomMembershipStatus(roomId)
        setMembership(membershipPayload ?? payload?.membershipStatus ?? null)
      } catch (membershipError) {
        if (isUnauthorizedApiError(membershipError)) {
          setMembership(null)
          setMembershipLoadError('Не удалось проверить ваш статус участия. Войдите заново, чтобы управлять участием.')
          return true
        }

        setMembership(payload?.membershipStatus ?? null)
        setMembershipLoadError('Не удалось проверить ваш статус участия. Доступны только публичные данные комнаты.')
      }
      return true
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return false
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить активность'))
      } else {
        setErrorMessage('Не удалось загрузить активность')
      }
      setRoom(null)
      return false
    } finally {
      setIsLoading(false)
    }
  }, [isAuthenticated, navigate, roomId])

  useEffect(() => {
    loadRoom()
  }, [loadRoom])

  useEffect(() => {
    setBulletinEditing(false)
    setBulletinDraft('')
    setBulletinFeedback('')
  }, [room?.id, membership?.status, membership?.role])

  const participantCount = room?.participantCount ?? 0
  const maxPeople = room?.maximumParticipants ?? 0
  const isFull = maxPeople > 0 && participantCount >= maxPeople
  const membershipView = getRoomMembershipView(room, membership)
  const isParticipant = membershipView.isParticipant
  const isBanned = membershipView.isBanned
  const hasPendingRequest = membershipView.hasPendingRequest
  const isPublic = room?.isPublic !== false
  const hasProtectedAccess = membershipView.hasProtectedAccess
  const hasMembershipSnapshot = membership != null || room?.membershipStatus != null
  const membershipUnavailable = isAuthenticated && Boolean(membershipLoadError) && !hasMembershipSnapshot
  const organizerId = room?.creator?.id
  const organizerName = resolveUserName(room?.creator, 'Не указано')
  const membershipRoleLabel = getRoleLabel(membershipView.membershipRole)

  const bulletinDisplayText =
    room?.bulletinBoard?.content != null
      ? normalizeBulletinContent(room.bulletinBoard.content)
      : ''
  const hasBulletinText = bulletinDisplayText.trim() !== ''

  const startBulletinEdit = () => {
    setBulletinDraft(normalizeBulletinContent(room?.bulletinBoard?.content ?? ''))
    setBulletinFeedback('')
    setBulletinEditing(true)
  }

  const cancelBulletinEdit = () => {
    setBulletinEditing(false)
    setBulletinDraft('')
    setBulletinFeedback('')
  }

  const saveBulletin = async () => {
    const trimmed = bulletinDraft.trim()
    if (trimmed === '') {
      setBulletinFeedback('Текст объявления не может быть пустым.')
      return
    }

    setBulletinSaving(true)
    setBulletinFeedback('')
    try {
      const response = await updateRoomBulletin(roomId, trimmed)
      setRoom((prev) => {
        if (!prev) {
          return prev
        }
        return {
          ...prev,
          bulletinBoard: response
            ? {
                id: response.id,
                content: normalizeBulletinContent(response.content),
                author: response.author,
                updatedAt: response.updatedAt,
              }
            : prev.bulletinBoard,
        }
      })
      setBulletinEditing(false)
      setBulletinDraft('')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setBulletinFeedback(getUserFacingApiMessage(error, 'Не удалось сохранить доску объявлений.'))
      } else {
        setBulletinFeedback('Не удалось сохранить доску объявлений.')
      }
    } finally {
      setBulletinSaving(false)
    }
  }

  const handleJoin = async () => {
    if (
      !isAuthenticated ||
      !room ||
      membershipUnavailable ||
      isParticipant ||
      isFull ||
      isBanned ||
      !membershipView.canJoin
    ) {
      return
    }
    setJoinSubmitting(true)
    setJoinFeedback('')
    setJoinFeedbackTone('success')
    try {
      await joinRoom(roomId)
      await loadRoom({ preserveJoinFeedback: true })
      if (isPublic) {
        setJoinFeedback('Вы присоединились к активности.')
      } else {
        setJoinFeedback('Заявка отправлена и ожидает решения организаторов.')
      }
      setJoinFeedbackTone('success')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setJoinFeedback(getUserFacingApiMessage(error, 'Не удалось выполнить действие.'))
      } else {
        setJoinFeedback('Не удалось выполнить действие.')
      }
      setJoinFeedbackTone('error')
    } finally {
      setJoinSubmitting(false)
    }
  }

  const handleLeaveRoom = async () => {
    if (!room || !membershipView.canLeave) {
      return
    }

    const confirmed = window.confirm(`Покинуть активность «${room.name || 'Без названия'}»?`)
    if (!confirmed) {
      return
    }

    setLeaveSubmitting(true)
    setJoinFeedback('')
    setJoinFeedbackTone('success')
    try {
      await leaveRoom(roomId)
      await loadRoom({ preserveJoinFeedback: true })
      setJoinFeedback('Вы покинули активность.')
      setJoinFeedbackTone('success')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setJoinFeedback(getUserFacingApiMessage(error, 'Не удалось покинуть активность.'))
      } else {
        setJoinFeedback('Не удалось покинуть активность.')
      }
      setJoinFeedbackTone('error')
    } finally {
      setLeaveSubmitting(false)
    }
  }

  const handleDeleteRoom = async () => {
    if (!room || !membershipView.canDeleteRoom) {
      return
    }

    const confirmed = window.confirm(
      `Удалить активность «${room.name || 'Без названия'}» без возможности восстановления?`,
    )
    if (!confirmed) {
      return
    }

    setDeleteSubmitting(true)
    setJoinFeedback('')
    setJoinFeedbackTone('success')
    try {
      await deleteRoom(roomId)
      navigate('/main', { replace: true })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setJoinFeedback(getUserFacingApiMessage(error, 'Не удалось удалить активность.'))
      } else {
        setJoinFeedback('Не удалось удалить активность.')
      }
      setJoinFeedbackTone('error')
    } finally {
      setDeleteSubmitting(false)
    }
  }

  const renderJoinBlock = () => {
    if (!room) {
      return null
    }

    if (!isAuthenticated) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Присоединиться к активности</h2>
          <p className="gray-elem">
            Для гостей вступление и заявки недоступны — войдите в аккаунт.
          </p>
          <Link className="cta-black-button-link" to={`/sign-in?next=/rooms/${roomId}`}>
            Войти
          </Link>
        </div>
      )
    }

    if (membershipUnavailable) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Присоединиться к активности</h2>
          <p className="gray-elem">
            Не удалось определить ваш статус участия. Обновите страницу или войдите заново.
          </p>
        </div>
      )
    }

    if (isParticipant) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Участие</h2>
          <p className="room-join-status">
            Вы состоите в этой активности{membershipRoleLabel ? ` как ${membershipRoleLabel.toLowerCase()}` : ''}.
          </p>
          <div className="room-membership-actions">
            {membershipView.canLeave ? (
              <button
                type="button"
                className="cta-black-button room-join-secondary"
                disabled={leaveSubmitting || deleteSubmitting}
                onClick={handleLeaveRoom}
              >
                {leaveSubmitting ? 'Выход...' : 'Покинуть комнату'}
              </button>
            ) : null}
            {membershipView.canDeleteRoom ? (
              <button
                type="button"
                className="room-delete-button"
                disabled={leaveSubmitting || deleteSubmitting}
                onClick={handleDeleteRoom}
              >
                {deleteSubmitting ? 'Удаление...' : 'Удалить комнату'}
              </button>
            ) : null}
          </div>
        </div>
      )
    }

    if (isBanned) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Присоединиться к активности</h2>
          <p className="room-join-disabled" role="status">
            Вы не можете вступить в активность
          </p>
        </div>
      )
    }

    if (isFull) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Присоединиться к активности</h2>
          <p className="room-join-disabled" role="status">
            Вы не можете вступить в активность
          </p>
        </div>
      )
    }

    if (!isPublic && hasPendingRequest) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Заявка на вступление</h2>
          <p className="room-join-status">Заявка на рассмотрении.</p>
        </div>
      )
    }

    if (!membershipView.canJoin) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Присоединиться к активности</h2>
          <p className="room-join-disabled" role="status">
            Сейчас вступление недоступно
          </p>
        </div>
      )
    }

    const label = isPublic ? 'Вступить' : 'Отправить заявку на вступление'

    return (
      <div className="room-join-block room-panel-soft">
        <h2 className="room-section-heading">Присоединиться к активности</h2>
        <button
          type="button"
          className="cta-black-button room-join-primary"
          disabled={joinSubmitting}
          onClick={handleJoin}
        >
          {joinSubmitting ? 'Отправка...' : label}
        </button>
      </div>
    )
  }

  return (
    <>
      <AppHeader activeTab="main" />
      <div className="virtual-elem"></div>

      <main className="room-activity-page">
        <Link className="back-link" to="/main">
          ← Назад к активностям
        </Link>

        {isLoading ? <p className="room-activity-loading">Загрузка...</p> : null}

        {!isLoading && errorMessage ? (
          <p className="room-activity-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
        {!isLoading && !errorMessage && membershipLoadError ? (
          <p className="room-activity-error" role="status">
            {membershipLoadError}
          </p>
        ) : null}

        {!isLoading && !errorMessage && room ? (
          <>
            <header className="room-activity-hero">
              <div className="room-activity-title-row">
                <h1 className="room-activity-title">{room.name || 'Без названия'}</h1>
                <div className="room-activity-statuses">
                  {membershipRoleLabel ? (
                    <span className="room-activity-status">{membershipRoleLabel}</span>
                  ) : null}
                  {isFull ? (
                    <span className="room-activity-status room-activity-status--full">Мест нет</span>
                  ) : null}
                </div>
              </div>

              <div
                className="room-meta-chips"
                role="group"
                aria-label="Параметры активности"
              >
                <div className="room-meta-chip">
                  <span className="room-meta-chip-icon" aria-hidden="true">
                    <i className="fa-solid fa-layer-group"></i>
                  </span>
                  <div className="room-meta-chip-body">
                    <span className="room-meta-chip-label">Категория</span>
                    <span className="room-meta-chip-value">{formatCategory(room.category)}</span>
                  </div>
                </div>

                <div className="room-meta-chip">
                  <span className="room-meta-chip-icon" aria-hidden="true">
                    <i className={isPublic ? 'fa-solid fa-globe' : 'fa-solid fa-lock'}></i>
                  </span>
                  <div className="room-meta-chip-body">
                    <span className="room-meta-chip-label">Доступ</span>
                    <span className="room-meta-chip-value">
                      {isPublic ? 'Публичная' : 'Приватная'}
                    </span>
                  </div>
                </div>

                <div className="room-meta-chip">
                  <span className="room-meta-chip-icon" aria-hidden="true">
                    <i className="fa-solid fa-user-shield"></i>
                  </span>
                  <div className="room-meta-chip-body">
                    <span className="room-meta-chip-label">Возраст</span>
                    <span className="room-meta-chip-value">{room.ageRating ?? 0}+</span>
                  </div>
                </div>

                <div
                  className={
                    isFull
                      ? 'room-meta-chip room-meta-chip--accent'
                      : 'room-meta-chip'
                  }
                >
                  <span className="room-meta-chip-icon" aria-hidden="true">
                    <i className="fa-solid fa-users"></i>
                  </span>
                  <div className="room-meta-chip-body">
                    <span className="room-meta-chip-label">Участники</span>
                    <span className="room-meta-chip-value">
                      {participantCount}
                      {maxPeople > 0 ? ` из ${maxPeople}` : ''}
                    </span>
                  </div>
                </div>
              </div>

              <div className="room-schedule" role="group" aria-label="Расписание">
                <div className="room-schedule-item">
                  <span className="room-schedule-icon" aria-hidden="true">
                    <i className="fa-regular fa-calendar"></i>
                  </span>
                  <div className="room-schedule-body">
                    <span className="room-schedule-label">Начало</span>
                    <span className="room-schedule-value">{formatDate(room.dateOfStartEvent)}</span>
                  </div>
                </div>
                <div className="room-schedule-divider" aria-hidden="true" />
                <div className="room-schedule-item">
                  <span className="room-schedule-icon" aria-hidden="true">
                    <i className="fa-regular fa-calendar-check"></i>
                  </span>
                  <div className="room-schedule-body">
                    <span className="room-schedule-label">Окончание</span>
                    <span className="room-schedule-value">{formatDate(room.dateOfEndEvent)}</span>
                  </div>
                </div>
              </div>
            </header>

            <section className="room-description-section room-panel-soft">
              <h2 className="room-section-heading">Описание</h2>
              <p className="room-description-text">{room.description || 'Описание отсутствует.'}</p>
            </section>

            <section className="room-activity-columns">
              <div className="room-activity-side-grid">
                <article className="room-panel room-panel-soft">
                  <h2 className="room-section-heading">Организатор</h2>
                  <div className="organizer-row">
                    <span className="organizer-avatar" aria-hidden="true">
                      <i className="fa-regular fa-circle-user"></i>
                    </span>
                    <div className="organizer-details">
                      {organizerId ? (
                        <Link to={`/users/${organizerId}`} className="organizer-name organizer-name--link">
                          {organizerName}
                        </Link>
                      ) : (
                        <span className="organizer-name">{organizerName}</span>
                      )}
                    </div>
                  </div>
                </article>

                {renderJoinBlock()}
              </div>

              {joinFeedback ? (
                <p
                  className={
                    joinFeedbackTone === 'error'
                      ? 'room-join-feedback room-join-error'
                      : 'room-join-feedback room-join-ok'
                  }
                  role="status"
                >
                  {joinFeedback}
                </p>
              ) : null}
            </section>

            <section className="room-panel room-panel-soft room-chat-section">
              <h2 className="room-section-heading">Чат</h2>
              {hasProtectedAccess && room.chatLink ? (
                <a href={room.chatLink} target="_blank" rel="noopener noreferrer">
                  Перейти в чат
                </a>
              ) : null}
              {hasProtectedAccess && !room.chatLink ? <p className="gray-elem">Чат отсутствует</p> : null}
              {!hasProtectedAccess ? (
                <p className="gray-elem">
                  Ссылка на чат видна только участникам активности после входа в аккаунт.
                </p>
              ) : null}
            </section>

            {isAuthenticated ? (
              <section
                className="activity-bulletin-board room-panel-soft"
                aria-labelledby="bulletin-heading"
              >
                <div className="room-bulletin-header">
                  <h2 id="bulletin-heading" className="room-section-heading">
                    Доска объявлений
                  </h2>
                </div>

                {!hasProtectedAccess ? (
                  <p className="gray-elem">
                    Доска объявлений доступна только участникам активности после входа в аккаунт.
                  </p>
                ) : bulletinEditing ? (
                  <div className="room-bulletin-edit">
                    <label className="room-bulletin-edit-label" htmlFor="bulletin-textarea">
                      Текст доски
                    </label>
                    <textarea
                      id="bulletin-textarea"
                      className="room-bulletin-textarea"
                      rows={8}
                      value={bulletinDraft}
                      onChange={(event) => setBulletinDraft(event.target.value)}
                      disabled={bulletinSaving}
                    />
                    <div className="room-bulletin-actions">
                      <button
                        type="button"
                        className="cta-black-button room-bulletin-save"
                        disabled={bulletinSaving}
                        onClick={saveBulletin}
                      >
                        {bulletinSaving ? 'Сохранение...' : 'Сохранить изменения'}
                      </button>
                      <button
                        type="button"
                        className="room-bulletin-cancel"
                        disabled={bulletinSaving}
                        onClick={cancelBulletinEdit}
                      >
                        Отмена
                      </button>
                    </div>
                    {bulletinFeedback ? (
                      <p
                        className={
                          bulletinFeedback.includes('не может') || bulletinFeedback.includes('Не удалось')
                            ? 'room-bulletin-feedback room-bulletin-feedback--error'
                            : 'room-bulletin-feedback'
                        }
                        role="status"
                      >
                        {bulletinFeedback}
                      </p>
                    ) : null}
                  </div>
                ) : (
                  <>
                    {hasBulletinText ? (
                      <>
                        <p className="room-bulletin-meta">
                          <span className="room-bulletin-meta-label">Обновлено:</span>{' '}
                          {formatDateTime(room.bulletinBoard.updatedAt)}
                          {room.bulletinBoard.author?.userName || room.bulletinBoard.author?.username ? (
                            <>
                              {' '}
                              <span className="room-bulletin-meta-sep" aria-hidden="true">
                                ·
                              </span>{' '}
                              <span className="room-bulletin-meta-label">Автор:</span>{' '}
                              {room.bulletinBoard.author.userName ?? room.bulletinBoard.author.username}
                            </>
                          ) : null}
                        </p>
                        <div className="room-bulletin-content">{bulletinDisplayText}</div>
                      </>
                    ) : (
                      <p className="gray-elem">На доске пока нет объявлений.</p>
                    )}
                    {membershipView.canModerate ? (
                      <div className="room-bulletin-toolbar">
                        <button type="button" className="room-bulletin-edit-btn" onClick={startBulletinEdit}>
                          {hasBulletinText ? 'Изменить доску' : 'Добавить объявление'}
                        </button>
                      </div>
                    ) : null}
                  </>
                )}
              </section>
            ) : null}
          </>
        ) : null}
      </main>
    </>
  )
}

export default RoomActivityPage
