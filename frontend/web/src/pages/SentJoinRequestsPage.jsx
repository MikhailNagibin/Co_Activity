import { useCallback, useEffect, useLayoutEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { isApiError } from '../api/httpClient.js'
import {
  cancelSentJoinRequest,
  getSentJoinRequests,
} from '../services/profileService.js'
import { getRoomMembershipStatus, joinRoom } from '../services/roomsService.js'
import {
  mapSentJoinRequestsToCards,
  normalizeMembershipStatus,
} from '../services/uiMappers.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import { useConfirmDialog } from '../hooks/useConfirmDialog.jsx'

const SORT_OPTIONS = [
  { value: 'created-desc', label: 'Сначала новые' },
  { value: 'created-asc', label: 'Сначала старые' },
  { value: 'name-asc', label: 'По названию А-Я' },
]

const DISMISSED_STORAGE_NS = 'coactivity.sentRequests.dismissed'
const MAX_STORED_DISMISSED_IDS = 200

function resolveSessionUserId(user) {
  const raw = user?.id ?? user?.userId
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

function dismissedIdsStorageKey(userId) {
  return `${DISMISSED_STORAGE_NS}:${userId}`
}

function readStoredDismissedIds(userId) {
  if (userId == null) {
    return []
  }
  try {
    const raw = localStorage.getItem(dismissedIdsStorageKey(userId))
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.map((id) => String(id)).filter(Boolean)
  } catch {
    return []
  }
}

function writeStoredDismissedIds(userId, ids) {
  if (userId == null) {
    return
  }
  try {
    const unique = [...new Set(ids.map((id) => String(id)).filter(Boolean))]
    const capped =
      unique.length > MAX_STORED_DISMISSED_IDS
        ? unique.slice(unique.length - MAX_STORED_DISMISSED_IDS)
        : unique
    localStorage.setItem(dismissedIdsStorageKey(userId), JSON.stringify(capped))
  } catch {
    // ignore quota / private mode
  }
}

function normalizeSearch(value) {
  return String(value ?? '').trim().toLowerCase()
}

function getStatusTone(status) {
  switch (normalizeMembershipStatus(status)) {
    case 'PENDING':
      return 'neutral'
    case 'ACCEPTED':
      return 'success'
    case 'REFUSED_WITH_BAN':
      return 'danger'
    case 'REFUSED':
      return 'warning'
    default:
      return 'neutral'
  }
}

function getMembershipHint(status) {
  switch (normalizeMembershipStatus(status)) {
    case 'PARTICIPANT':
      return 'Сейчас вы уже участник этой комнаты.'
    case 'BANNED':
      return 'Сейчас вступление недоступно: вы в бане.'
    case 'PENDING':
      return 'Текущее состояние комнаты: заявка ещё ожидает решения.'
    case 'NOT_JOINED':
      return 'Сейчас вы не состоите в этой комнате.'
    default:
      return ''
  }
}

function canDismissRequest(status) {
  return normalizeMembershipStatus(status) !== 'PENDING'
}

function canRepeatJoinRequest(request) {
  if (
    normalizeMembershipStatus(request.status) !== 'REFUSED' ||
    normalizeMembershipStatus(request.membershipStatus) !== 'NOT_JOINED' ||
    !Number.isFinite(Number(request.roomId))
  ) {
    return false
  }
  if (request.roomIsFull === true) {
    return false
  }
  return true
}

function sortRequests(items, sortBy) {
  const copy = [...items]

  switch (sortBy) {
    case 'created-asc':
      copy.sort((a, b) => String(a.createdAtIso ?? '').localeCompare(String(b.createdAtIso ?? '')))
      return copy
    case 'name-asc':
      copy.sort((a, b) => a.roomName.localeCompare(b.roomName, 'ru'))
      return copy
    case 'created-desc':
    default:
      copy.sort((a, b) => String(b.createdAtIso ?? '').localeCompare(String(a.createdAtIso ?? '')))
      return copy
  }
}

async function loadMembershipStatuses(requests) {
  const uniqueRoomIds = [...new Set(requests.map((item) => item.roomId).filter((id) => id != null))]
  const settled = await Promise.allSettled(uniqueRoomIds.map((roomId) => getRoomMembershipStatus(roomId)))
  return new Map(
    settled
      .filter((result) => result.status === 'fulfilled')
      .map((result) => [result.value.roomId, result.value]),
  )
}

function SentJoinRequestsPage() {
  const navigate = useNavigate()
  const { requestConfirm, confirmDialog } = useConfirmDialog()
  const { currentUser } = useAuthSession()
  const sessionUserId = useMemo(() => resolveSessionUserId(currentUser), [currentUser])

  const [requests, setRequests] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState('created-desc')
  const [activeRequestId, setActiveRequestId] = useState(null)
  const [dismissedRequestIds, setDismissedRequestIds] = useState([])

  useLayoutEffect(() => {
    if (sessionUserId == null) {
      setDismissedRequestIds([])
      return
    }
    setDismissedRequestIds(readStoredDismissedIds(sessionUserId))
  }, [sessionUserId])

  const loadRequests = useCallback(async (options = {}) => {
    const { silent = false } = options
    if (!silent) {
      setIsLoading(true)
      setErrorMessage('')
    }

    try {
      const payload = await getSentJoinRequests()
      const mappedRequests = mapSentJoinRequestsToCards(payload)
      const membershipStatuses = await loadMembershipStatuses(mappedRequests)
      setRequests(
        mappedRequests.map((request) => ({
          ...request,
          membershipStatus:
            request.roomId != null ? membershipStatuses.get(request.roomId)?.status ?? '' : '',
        })),
      )
      if (silent) {
        setErrorMessage('')
      }
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/sent-requests' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить отправленные заявки'))
      } else {
        setErrorMessage('Не удалось загрузить отправленные заявки')
      }
      if (!silent) {
        setRequests([])
      }
    } finally {
      if (!silent) {
        setIsLoading(false)
      }
    }
  }, [navigate])

  useEffect(() => {
    loadRequests()
  }, [loadRequests])

  const filteredRequests = useMemo(() => {
    const dismissed = new Set(dismissedRequestIds.map(String))
    const visibleRequests = requests.filter((request) => !dismissed.has(String(request.id)))
    const query = normalizeSearch(searchQuery)
    const visible = query
      ? visibleRequests.filter((request) =>
          [request.roomName, request.statusLabel, request.createdAt].some((part) =>
            String(part ?? '')
              .toLowerCase()
              .includes(query),
          ),
        )
      : visibleRequests

    return sortRequests(visible, sortBy)
  }, [dismissedRequestIds, requests, searchQuery, sortBy])

  const handleCancelRequest = async (request) => {
    if (!request?.requestId) {
      return
    }

    const ok = await requestConfirm({
      title: 'Заявка',
      message: `Отозвать заявку в «${request.roomName}»?`,
      confirmLabel: 'Отозвать',
      variant: 'danger',
    })
    if (!ok) {
      return
    }

    setActiveRequestId(request.requestId)
    setErrorMessage('')

    try {
      await cancelSentJoinRequest(request.requestId)
      await loadRequests({ silent: true })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/sent-requests' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось отозвать заявку'))
      } else {
        setErrorMessage('Не удалось отозвать заявку')
      }
    } finally {
      setActiveRequestId(null)
    }
  }

  const handleDismissRequest = (request) => {
    if (!request?.id || !canDismissRequest(request.status)) {
      return
    }
    const idStr = String(request.id)
    setDismissedRequestIds((prev) => {
      if (prev.includes(idStr)) {
        return prev
      }
      const next = [...prev, idStr]
      if (sessionUserId != null) {
        writeStoredDismissedIds(sessionUserId, next)
      }
      return next
    })
  }

  const handleRepeatRequest = async (request) => {
    if (!canRepeatJoinRequest(request)) {
      return
    }

    const ok = await requestConfirm({
      title: 'Заявка',
      message: `Повторно отправить заявку в «${request.roomName}»?`,
      confirmLabel: 'Отправить',
      variant: 'primary',
    })
    if (!ok) {
      return
    }

    setActiveRequestId(request.requestId ?? request.id)
    setErrorMessage('')

    try {
      await joinRoom(request.roomId)
      await loadRequests({ silent: true })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/sent-requests' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось отправить заявку повторно'))
      } else {
        setErrorMessage('Не удалось отправить заявку повторно')
      }
    } finally {
      setActiveRequestId(null)
    }
  }

  return (
    <>
      <ProfileCabinetShell
        heroTitle="Отправленные заявки"
        heroSubtitle="Следите за статусом заявок и отзывайте ожидающие"
      >
        <main className="profile-list-page">
          <div className="main-toolbar-shell">
            <div className="main-toolbar profile-list-toolbar profile-list-toolbar--rich">
              <div className="search-wrapper">
                <button className="search-button" type="button" aria-label="Поиск">
                  <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                </button>
                <input
                  placeholder="Комната, статус, дата…"
                  type="search"
                  name="q"
                  autoComplete="off"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  aria-label="Поиск по отправленным заявкам"
                />
              </div>

              <StyledDropdown
                variant="toolbar"
                id="sent-requests-sort"
                ariaLabel="Сортировка"
                options={SORT_OPTIONS}
                value={sortBy}
                onChange={setSortBy}
              />
            </div>
          </div>

          {errorMessage ? (
            <p className="profile-list-message profile-list-message--error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <section className="profile-list-grid" aria-label="Список отправленных заявок">
            {isLoading ? <p className="profile-list-message">Загрузка заявок...</p> : null}
            {!isLoading && !errorMessage && requests.length === 0 ? (
              <p className="profile-list-message">Вы ещё не отправляли заявки.</p>
            ) : null}
            {!isLoading && !errorMessage && requests.length > 0 && filteredRequests.length === 0 ? (
              <p className="profile-list-message">По текущему поиску ничего не найдено.</p>
            ) : null}
            {!isLoading && !errorMessage
              ? filteredRequests.map((request) => (
                  <article key={request.id} className="sent-request-card">
                    <div className="sent-request-card__header">
                      {request.linkTo ? (
                        <Link className="sent-request-card__title" to={request.linkTo}>
                          {request.roomName}
                        </Link>
                      ) : (
                        <h2 className="sent-request-card__title">{request.roomName}</h2>
                      )}
                      <span
                        className={`sent-request-card__status sent-request-card__status--${getStatusTone(request.status)}`}
                      >
                        {request.statusLabel}
                      </span>
                    </div>

                    <div className="sent-request-card__meta">
                      <span>Отправлено: {request.createdAt || 'Дата неизвестна'}</span>
                      {request.requestId ? <span>Заявка #{request.requestId}</span> : null}
                    </div>

                    {getMembershipHint(request.membershipStatus) ? (
                      <p className="sent-request-card__hint">{getMembershipHint(request.membershipStatus)}</p>
                    ) : null}

                    <div className="sent-request-card__actions">
                      {request.status === 'PENDING' ? (
                        <button
                          type="button"
                          className="profile-room-card__action profile-room-card__action--secondary"
                          onClick={() => handleCancelRequest(request)}
                          disabled={activeRequestId === request.requestId}
                        >
                          {activeRequestId === request.requestId ? 'Отзыв...' : 'Отозвать заявку'}
                        </button>
                      ) : null}
                      {canRepeatJoinRequest(request) ? (
                        <button
                          type="button"
                          className="profile-room-card__action profile-room-card__action--secondary"
                          onClick={() => handleRepeatRequest(request)}
                          disabled={activeRequestId === (request.requestId ?? request.id)}
                        >
                          {activeRequestId === (request.requestId ?? request.id)
                            ? 'Отправка...'
                            : 'Отправить заявку снова'}
                        </button>
                      ) : null}
                      {canDismissRequest(request.status) ? (
                        <button
                          type="button"
                          className="profile-room-card__action profile-room-card__action--secondary"
                          onClick={() => handleDismissRequest(request)}
                        >
                          Убрать из списка
                        </button>
                      ) : null}
                    </div>
                  </article>
                ))
              : null}
          </section>
        </main>
      </ProfileCabinetShell>
      {confirmDialog}
    </>
  )
}

export default SentJoinRequestsPage
