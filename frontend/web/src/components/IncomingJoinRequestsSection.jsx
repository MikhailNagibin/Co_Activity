import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  getPendingJoinRequests,
  getPendingJoinRequestsForRoom,
  processJoinRequest,
} from '../services/roomsService.js'
import {
  groupIncomingJoinRequestsByRoom,
  mapIncomingJoinRequests,
} from '../services/uiMappers.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'

const REQUEST_ACTIONS = [
  {
    key: 'ACCEPTED',
    label: 'Принять',
    pendingLabel: 'Принятие...',
    successMessage: 'Заявка принята.',
    errorMessage: 'Не удалось принять заявку.',
    buildConfirmMessage: (request) =>
      `Принять заявку пользователя «${request.username}» в активность «${request.roomName}»?`,
  },
  {
    key: 'REFUSED',
    label: 'Отклонить',
    pendingLabel: 'Отклонение...',
    successMessage: 'Заявка отклонена.',
    errorMessage: 'Не удалось отклонить заявку.',
    buildConfirmMessage: (request) =>
      `Отклонить заявку пользователя «${request.username}» в активности «${request.roomName}»?`,
  },
  {
    key: 'REFUSED_WITH_BAN',
    label: 'Отклонить с баном',
    pendingLabel: 'Блокировка...',
    successMessage: 'Заявка отклонена, пользователь заблокирован.',
    errorMessage: 'Не удалось отклонить заявку с баном.',
    tone: 'danger',
    buildConfirmMessage: (request) =>
      `Отклонить заявку пользователя «${request.username}» и заблокировать его в активности «${request.roomName}»?`,
  },
]

function buildNextPath(roomId, profilePath) {
  if (roomId != null) {
    return `/rooms/${encodeURIComponent(String(roomId))}`
  }
  return profilePath
}

function IncomingJoinRequestCard({
  request,
  disabled,
  pendingAction,
  onAction,
}) {
  return (
    <article className="incoming-request-card">
      <div className="incoming-request-card__header">
        <div className="incoming-request-card__title-group">
          <span className="profile-room-card__eyebrow">Заявка на вступление</span>
          {request.userLink ? (
            <Link className="sent-request-card__title" to={request.userLink}>
              {request.username}
            </Link>
          ) : (
            <h3 className="sent-request-card__title">{request.username}</h3>
          )}
        </div>
        <span className="sent-request-card__status sent-request-card__status--neutral">
          {request.statusLabel}
        </span>
      </div>

      <div className="sent-request-card__meta">
        <span>Отправлено: {request.createdAt || 'Дата неизвестна'}</span>
        {request.requestId ? <span>Заявка #{request.requestId}</span> : null}
      </div>

      <div className="incoming-request-card__actions">
        {REQUEST_ACTIONS.map((action) => (
          <button
            key={`${request.id}-${action.key}`}
            type="button"
            className={
              action.tone === 'danger'
                ? 'room-governance-action room-governance-action--danger'
                : 'room-governance-action'
            }
            disabled={disabled || !request.requestId}
            onClick={() => onAction(request, action)}
          >
            {pendingAction === action.key ? action.pendingLabel : action.label}
          </button>
        ))}
      </div>
    </article>
  )
}

function IncomingJoinRequestsSection({
  roomId = null,
  title = 'Входящие заявки',
  description = 'Ожидающие заявки в комнаты, которые вы администрируете.',
  emptyMessage = 'Ожидающих заявок пока нет.',
  nextPath = '/profile/incoming-requests',
  groupByRoom = false,
  canManageActions = true,
  onAfterAction,
}) {
  const navigate = useNavigate()
  const [requests, setRequests] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [feedbackTone, setFeedbackTone] = useState('success')
  const [pendingActionKey, setPendingActionKey] = useState('')

  const loadRequests = useCallback(async (options = {}) => {
    const { preserveFeedback = false } = options
    setIsLoading(true)
    setErrorMessage('')
    if (!preserveFeedback) {
      setFeedback('')
      setFeedbackTone('success')
    }

    try {
      const payload =
        roomId != null
          ? await getPendingJoinRequestsForRoom(roomId)
          : await getPendingJoinRequests()
      setRequests(mapIncomingJoinRequests(payload))
      return true
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: buildNextPath(roomId, nextPath),
        })
        return false
      }

      setRequests([])
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить входящие заявки.'))
      } else {
        setErrorMessage('Не удалось загрузить входящие заявки.')
      }
      return false
    } finally {
      setIsLoading(false)
    }
  }, [navigate, nextPath, roomId])

  useEffect(() => {
    loadRequests()
  }, [loadRequests])

  const groupedRequests = useMemo(() => groupIncomingJoinRequestsByRoom(requests), [requests])

  const handleAction = async (request, action) => {
    if (!request?.requestId || !action?.key || !canManageActions) {
      return
    }

    const confirmed = window.confirm(action.buildConfirmMessage(request))
    if (!confirmed) {
      return
    }

    setPendingActionKey(`${request.requestId}:${action.key}`)
    setFeedback('')
    setFeedbackTone('success')

    try {
      await processJoinRequest(request.requestId, action.key)
      if (typeof onAfterAction === 'function') {
        await onAfterAction()
      }
      await loadRequests({ preserveFeedback: true })
      setFeedback(action.successMessage)
      setFeedbackTone('success')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: buildNextPath(roomId, nextPath),
        })
        return
      }

      if (isApiError(error)) {
        setFeedback(getUserFacingApiMessage(error, action.errorMessage))
      } else {
        setFeedback(action.errorMessage)
      }
      setFeedbackTone('error')
    } finally {
      setPendingActionKey('')
    }
  }

  const renderCard = (request) => (
    <IncomingJoinRequestCard
      key={request.id}
      request={request}
      disabled={
        !canManageActions ||
        request.canManage === false ||
        request.status !== 'PENDING' ||
        Boolean(pendingActionKey)
      }
      pendingAction={
        pendingActionKey.startsWith(`${request.requestId}:`)
          ? pendingActionKey.split(':')[1]
          : ''
      }
      onAction={handleAction}
    />
  )

  return (
    <section className="room-panel room-panel-soft incoming-requests-section" aria-labelledby="incoming-requests-heading">
      <div className="room-governance-header">
        <div>
          <h2 id="incoming-requests-heading" className="room-section-heading">
            {title}
          </h2>
          <p className="room-governance-copy">{description}</p>
        </div>
      </div>

      {feedback ? (
        <p
          className={
            feedbackTone === 'error'
              ? 'room-governance-feedback room-governance-feedback--error'
              : 'room-governance-feedback room-governance-feedback--success'
          }
          role="status"
        >
          {feedback}
        </p>
      ) : null}

      {errorMessage ? (
        <p className="room-activity-error" role="alert">
          {errorMessage}
        </p>
      ) : null}

      {isLoading ? <p className="gray-elem">Загрузка заявок...</p> : null}

      {!isLoading && !errorMessage && requests.length === 0 ? (
        <p className="gray-elem">{emptyMessage}</p>
      ) : null}

      {!isLoading && !errorMessage && requests.length > 0 && groupByRoom ? (
        <div className="incoming-request-groups">
          {groupedRequests.map((group) => (
            <section key={group.roomId ?? group.roomName} className="incoming-request-group">
              <div className="incoming-request-group__header">
                <div>
                  <span className="profile-room-card__eyebrow">Комната</span>
                  {group.roomLink ? (
                    <Link className="profile-room-card__title" to={group.roomLink}>
                      {group.roomName}
                    </Link>
                  ) : (
                    <h3 className="profile-room-card__title">{group.roomName}</h3>
                  )}
                </div>
                <span className="room-governance-card__role">
                  {group.requests.length} шт.
                </span>
              </div>
              <div className="incoming-request-group__list">{group.requests.map(renderCard)}</div>
            </section>
          ))}
        </div>
      ) : null}

      {!isLoading && !errorMessage && requests.length > 0 && !groupByRoom ? (
        <div className="incoming-request-group__list">{requests.map(renderCard)}</div>
      ) : null}
    </section>
  )
}

export default IncomingJoinRequestsSection
