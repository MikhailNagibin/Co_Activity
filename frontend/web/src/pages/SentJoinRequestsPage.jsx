import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { isApiError } from '../api/httpClient.js'
import {
  cancelSentJoinRequest,
  getSentJoinRequests,
} from '../services/profileService.js'
import { getRoomMembershipStatus } from '../services/roomsService.js'
import {
  mapSentJoinRequestsToCards,
  normalizeMembershipStatus,
} from '../services/uiMappers.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'

const SORT_OPTIONS = [
  { value: 'created-desc', label: 'Сначала новые' },
  { value: 'created-asc', label: 'Сначала старые' },
  { value: 'name-asc', label: 'По названию А-Я' },
]

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
  const [requests, setRequests] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState('created-desc')
  const [activeRequestId, setActiveRequestId] = useState(null)

  const loadRequests = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

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
      setRequests([])
    } finally {
      setIsLoading(false)
    }
  }, [navigate])

  useEffect(() => {
    loadRequests()
  }, [loadRequests])

  const filteredRequests = useMemo(() => {
    const query = normalizeSearch(searchQuery)
    const visible = query
      ? requests.filter((request) =>
          [request.roomName, request.statusLabel, request.createdAt].some((part) =>
            String(part ?? '')
              .toLowerCase()
              .includes(query),
          ),
        )
      : requests

    return sortRequests(visible, sortBy)
  }, [requests, searchQuery, sortBy])

  const handleCancelRequest = async (request) => {
    if (!request?.requestId) {
      return
    }

    const confirmed = window.confirm(`Отозвать заявку в «${request.roomName}»?`)
    if (!confirmed) {
      return
    }

    setActiveRequestId(request.requestId)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await cancelSentJoinRequest(request.requestId)
      setSuccessMessage(`Заявка в «${request.roomName}» отозвана`)
      await loadRequests()
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

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-list-shell">
        <section className="main-hero">
          <h2>Отправленные заявки</h2>
          <h3 className="gray-elem">Следите за статусом заявок и отзывайте ожидающие</h3>
        </section>

        <main className="profile-list-page">
          <div className="profile-list-backline">
            <Link className="back-link" to="/profile">
              ← Назад в профиль
            </Link>
          </div>

          <div className="profile-list-toolbar">
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

          {errorMessage ? (
            <p className="profile-list-message profile-list-message--error" role="alert">
              {errorMessage}
            </p>
          ) : null}
          {successMessage ? (
            <p className="profile-list-message profile-list-message--success" role="status">
              {successMessage}
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

                    {request.status === 'PENDING' ? (
                      <div className="sent-request-card__actions">
                        <button
                          type="button"
                          className="profile-room-card__action profile-room-card__action--secondary"
                          onClick={() => handleCancelRequest(request)}
                          disabled={activeRequestId === request.requestId}
                        >
                          {activeRequestId === request.requestId ? 'Отзыв...' : 'Отозвать заявку'}
                        </button>
                      </div>
                    ) : null}
                  </article>
                ))
              : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default SentJoinRequestsPage
