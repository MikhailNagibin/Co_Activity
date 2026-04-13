import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import ProfileRoomCard from '../components/ProfileRoomCard.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { isApiError } from '../api/httpClient.js'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { MAIN_ACTIVITY_SORT_OPTIONS } from '../constants/browseFilterOptions.js'
import {
  deleteRoom,
  getMyRooms,
  getRoomMembershipStatus,
  leaveRoom,
} from '../services/roomsService.js'
import {
  mapRoomsToActivityCards,
  mergeRoomsWithMembershipStatuses,
} from '../services/uiMappers.js'
import { filterActivityCardsForBrowse, sortActivityCards } from '../utils/browseListFilters.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'

const DEFAULT_SORT = 'created-desc'

const ROLE_FILTER_OPTIONS = [
  { value: 'all', label: 'Все роли' },
  { value: 'OWNER', label: 'Создатель' },
  { value: 'ADMIN', label: 'Админ' },
  { value: 'PARTICIPANT', label: 'Участник' },
]

async function loadMembershipStatuses(items) {
  const requests = items
    .map((item) => item?.id)
    .filter((id) => Number.isFinite(Number(id)))
    .map((roomId) => getRoomMembershipStatus(roomId))

  const settled = await Promise.allSettled(requests)
  return settled
    .filter((result) => result.status === 'fulfilled')
    .map((result) => result.value)
}

function MyRoomsPage() {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isActingOnRoomId, setIsActingOnRoomId] = useState(null)
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState(DEFAULT_SORT)
  const [roleFilter, setRoleFilter] = useState('all')

  const loadRooms = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const payload = await getMyRooms()
      const statuses = await loadMembershipStatuses(Array.isArray(payload) ? payload : [])
      const mergedRooms = mergeRoomsWithMembershipStatuses(payload, statuses)
      setRooms(mapRoomsToActivityCards(mergedRooms))
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/my-rooms' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить ваши активности'))
      } else {
        setErrorMessage('Не удалось загрузить ваши активности')
      }
      setRooms([])
    } finally {
      setIsLoading(false)
    }
  }, [navigate])

  useEffect(() => {
    loadRooms()
  }, [loadRooms])

  const filteredRooms = useMemo(() => {
    const browseFiltered = filterActivityCardsForBrowse(rooms, {
      categoryFilter,
      searchQuery,
    })
    const roleFiltered =
      roleFilter === 'all'
        ? browseFiltered
        : browseFiltered.filter((item) => item.membershipRole === roleFilter)

    return sortActivityCards(roleFiltered, sortBy)
  }, [categoryFilter, roleFilter, rooms, searchQuery, sortBy])

  const handleLeaveRoom = async (room) => {
    if (!room?.id) {
      return
    }

    const confirmed = window.confirm(`Покинуть активность «${room.title}»?`)
    if (!confirmed) {
      return
    }

    setIsActingOnRoomId(room.id)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await leaveRoom(room.id)
      setSuccessMessage(`Вы покинули активность «${room.title}»`)
      await loadRooms()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/my-rooms' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось покинуть активность'))
      } else {
        setErrorMessage('Не удалось покинуть активность')
      }
    } finally {
      setIsActingOnRoomId(null)
    }
  }

  const handleDeleteRoom = async (room) => {
    if (!room?.id) {
      return
    }

    const confirmed = window.confirm(`Удалить активность «${room.title}» без возможности восстановления?`)
    if (!confirmed) {
      return
    }

    setIsActingOnRoomId(room.id)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await deleteRoom(room.id)
      setSuccessMessage(`Активность «${room.title}» удалена`)
      await loadRooms()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/my-rooms' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось удалить активность'))
      } else {
        setErrorMessage('Не удалось удалить активность')
      }
    } finally {
      setIsActingOnRoomId(null)
    }
  }

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-list-shell">
        <section className="main-hero">
          <h2>Мои активности</h2>
          <h3 className="gray-elem">Фильтруйте свои комнаты по роли, категории и поиску</h3>
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
                placeholder="Название, описание, организатор…"
                type="search"
                name="q"
                autoComplete="off"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                aria-label="Поиск по моим активностям"
              />
            </div>

            <StyledDropdown
              variant="toolbar"
              id="my-rooms-role-filter"
              ariaLabel="Фильтр по роли"
              options={ROLE_FILTER_OPTIONS}
              value={roleFilter}
              onChange={setRoleFilter}
            />

            <StyledDropdown
              variant="toolbar"
              id="my-rooms-category-filter"
              ariaLabel="Категория"
              options={BROWSE_CATEGORY_OPTIONS}
              value={categoryFilter}
              onChange={setCategoryFilter}
            />

            <StyledDropdown
              variant="toolbar"
              id="my-rooms-sort"
              ariaLabel="Сортировка"
              options={MAIN_ACTIVITY_SORT_OPTIONS}
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

          <section className="profile-list-grid" aria-label="Список моих активностей">
            {isLoading ? <p className="profile-list-message">Загрузка активностей...</p> : null}
            {!isLoading && !errorMessage && rooms.length === 0 ? (
              <p className="profile-list-message">У вас пока нет активностей.</p>
            ) : null}
            {!isLoading && !errorMessage && rooms.length > 0 && filteredRooms.length === 0 ? (
              <p className="profile-list-message">По текущим фильтрам ничего не найдено.</p>
            ) : null}
            {!isLoading && !errorMessage
              ? filteredRooms.map((room) => (
                  <ProfileRoomCard
                    key={room.id}
                    item={room}
                    eyebrow="Моя активность"
                    statusLabel={
                      room.membershipRole === 'OWNER'
                        ? 'Создатель'
                        : room.membershipRole === 'ADMIN'
                          ? 'Админ'
                          : 'Участник'
                    }
                    statusTone={room.membershipRole === 'OWNER' ? 'danger' : 'neutral'}
                    actions={[
                      ...(room.canLeave
                        ? [
                            {
                              key: `leave-${room.id}`,
                              label:
                                isActingOnRoomId === room.id ? 'Выход...' : 'Покинуть комнату',
                              onClick: () => handleLeaveRoom(room),
                              disabled: isActingOnRoomId === room.id,
                              variant: 'secondary',
                            },
                          ]
                        : []),
                      ...(room.canDeleteRoom
                        ? [
                            {
                              key: `delete-${room.id}`,
                              label:
                                isActingOnRoomId === room.id ? 'Удаление...' : 'Удалить комнату',
                              onClick: () => handleDeleteRoom(room),
                              disabled: isActingOnRoomId === room.id,
                              variant: 'danger',
                            },
                          ]
                        : []),
                    ]}
                  />
                ))
              : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default MyRoomsPage
