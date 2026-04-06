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
import { getBannedRooms, getMyProfile, getSentJoinRequests } from '../services/profileService.js'
import {
  getRoomById,
  getRoomParticipants,
  joinRoom,
  updateRoomBulletin,
} from '../services/roomsService.js'
import { formatDate, normalizeBulletinContent } from '../services/uiMappers.js'

function toArray(payload) {
  if (Array.isArray(payload)) {
    return payload
  }
  if (Array.isArray(payload?.items)) {
    return payload.items
  }
  return []
}

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

function isOwnerOrAdminRole(role) {
  const normalized = String(role ?? '')
    .toUpperCase()
    .replace(/\s+/g, '_')
  return normalized === 'OWNER' || normalized === 'ADMIN'
}

function RoomActivityPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const { roomId: roomIdParam } = useParams()
  const roomId = Number.parseInt(String(roomIdParam), 10)

  const [room, setRoom] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [isBanned, setIsBanned] = useState(false)
  const [hasPendingRequest, setHasPendingRequest] = useState(false)
  const [joinSubmitting, setJoinSubmitting] = useState(false)
  const [joinFeedback, setJoinFeedback] = useState('')
  const [canModerateBulletin, setCanModerateBulletin] = useState(false)
  const [bulletinEditing, setBulletinEditing] = useState(false)
  const [bulletinDraft, setBulletinDraft] = useState('')
  const [bulletinSaving, setBulletinSaving] = useState(false)
  const [bulletinFeedback, setBulletinFeedback] = useState('')

  const loadRoom = useCallback(async () => {
    if (!Number.isFinite(roomId) || roomId < 1) {
      setErrorMessage('Некорректный идентификатор активности')
      setRoom(null)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setErrorMessage('')
    setJoinFeedback('')

    try {
      const payload = await getRoomById(roomId)
      setRoom(payload)

      const [bannedPayload, sentPayload] = await Promise.all([
        getBannedRooms().catch(() => []),
        getSentJoinRequests().catch(() => []),
      ])
      const bannedIds = new Set(
        toArray(bannedPayload)
          .map((r) => r.id ?? r.roomId)
          .filter((id) => id != null),
      )
      setIsBanned(bannedIds.has(roomId))

      const pending = toArray(sentPayload).some(
        (req) =>
          (req.roomId === roomId || req.roomId === String(roomId)) &&
          String(req.status).toUpperCase() === 'CONSIDERATION',
      )
      setHasPendingRequest(pending)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить активность'))
      } else {
        setErrorMessage('Не удалось загрузить активность')
      }
      setRoom(null)
    } finally {
      setIsLoading(false)
    }
  }, [roomId, navigate])

  useEffect(() => {
    loadRoom()
  }, [loadRoom])

  useEffect(() => {
    let cancelled = false
    setBulletinEditing(false)
    setBulletinDraft('')
    setBulletinFeedback('')

    if (!room || !isAuthenticated || room.hasProtectedAccess !== true) {
      setCanModerateBulletin(false)
      return () => {
        cancelled = true
      }
    }

    ;(async () => {
      try {
        const [participants, profile] = await Promise.all([
          getRoomParticipants(roomId),
          getMyProfile(),
        ])
        const list = Array.isArray(participants) ? participants : []
        const self = list.find((p) => p.id === profile?.id)
        if (!cancelled) {
          setCanModerateBulletin(isOwnerOrAdminRole(self?.role))
        }
      } catch {
        if (!cancelled) {
          setCanModerateBulletin(false)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [room, roomId, isAuthenticated])

  const participantCount = room?.participantCount ?? 0
  const maxPeople = room?.maximumParticipants ?? 0
  const isFull = maxPeople > 0 && participantCount >= maxPeople
  const isParticipant = room?.isCurrentUserParticipant === true
  const isPublic = room?.isPublic !== false
  const hasProtectedAccess = room?.hasProtectedAccess === true

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
    if (!isAuthenticated || !room || isParticipant || isFull || isBanned) {
      return
    }
    setJoinSubmitting(true)
    setJoinFeedback('')
    try {
      await joinRoom(roomId)
      if (isPublic) {
        setJoinFeedback('Вы присоединились к активности.')
      } else {
        setJoinFeedback('Заявка отправлена и ожидает решения организаторов.')
      }
      await loadRoom()
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
    } finally {
      setJoinSubmitting(false)
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

    if (isParticipant) {
      return (
        <div className="room-join-block room-panel-soft">
          <h2 className="room-section-heading">Участие</h2>
          <p className="room-join-status">Вы участник этой активности.</p>
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

        {!isLoading && !errorMessage && room ? (
          <>
            <header className="room-activity-hero">
              <div className="room-activity-title-row">
                <h1 className="room-activity-title">{room.name || 'Без названия'}</h1>
                {isFull ? (
                  <span className="room-activity-status room-activity-status--full">Мест нет</span>
                ) : null}
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
                      <span className="organizer-name">{room.creator?.userName || 'Не указано'}</span>
                    </div>
                  </div>
                </article>

                {renderJoinBlock()}
              </div>

              {joinFeedback ? (
                <p
                  className={
                    joinFeedback.includes('Не удалось')
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
                    {canModerateBulletin ? (
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
