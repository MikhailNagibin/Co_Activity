import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import ProfileRoomCard from '../components/ProfileRoomCard.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { isApiError } from '../api/httpClient.js'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { MAIN_ACTIVITY_SORT_OPTIONS } from '../constants/browseFilterOptions.js'
import { getBannedRooms } from '../services/profileService.js'
import { getRoomMembershipStatus } from '../services/roomsService.js'
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

async function loadMembershipStatuses(items) {
  const settled = await Promise.allSettled(
    items
      .map((item) => item?.id)
      .filter((id) => Number.isFinite(Number(id)))
      .map((roomId) => getRoomMembershipStatus(roomId)),
  )

  return settled
    .filter((result) => result.status === 'fulfilled')
    .map((result) => result.value)
}

function BannedRoomsPage() {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState(DEFAULT_SORT)

  const loadRooms = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const payload = await getBannedRooms()
      const statuses = await loadMembershipStatuses(Array.isArray(payload) ? payload : [])
      const mergedRooms = mergeRoomsWithMembershipStatuses(payload, statuses)
      setRooms(mapRoomsToActivityCards(mergedRooms))
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/profile/banned-rooms' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить список банов'))
      } else {
        setErrorMessage('Не удалось загрузить список банов')
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
    return sortActivityCards(browseFiltered, sortBy)
  }, [categoryFilter, rooms, searchQuery, sortBy])

  return (
    <ProfileCabinetShell
      heroTitle="Забанен в комнатах"
      heroSubtitle="Список активностей, в которые вы больше не можете вступить"
    >
      <main className="profile-list-page">
        <div className="main-toolbar-shell">
            <div className="main-toolbar profile-list-toolbar profile-list-toolbar--rich">
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
                  aria-label="Поиск по забаненным комнатам"
                />
              </div>

              <StyledDropdown
                variant="toolbar"
                id="banned-rooms-category-filter"
                ariaLabel="Категория"
                options={BROWSE_CATEGORY_OPTIONS}
                value={categoryFilter}
                onChange={setCategoryFilter}
              />

              <StyledDropdown
                variant="toolbar"
                id="banned-rooms-sort"
                ariaLabel="Сортировка"
                options={MAIN_ACTIVITY_SORT_OPTIONS}
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

          <section className="profile-list-grid" aria-label="Список банов по комнатам">
            {isLoading ? <p className="profile-list-message">Загрузка списка...</p> : null}
            {!isLoading && !errorMessage && rooms.length === 0 ? (
              <p className="profile-list-message">У вас нет банов в комнатах.</p>
            ) : null}
            {!isLoading && !errorMessage && rooms.length > 0 && filteredRooms.length === 0 ? (
              <p className="profile-list-message">По текущим фильтрам ничего не найдено.</p>
            ) : null}
            {!isLoading && !errorMessage
              ? filteredRooms.map((room) => (
                  <ProfileRoomCard
                    key={room.id}
                    item={room}
                    eyebrow="Ограничение на вступление"
                    statusLabel="Забанен"
                    statusTone="danger"
                  />
                ))
              : null}
          </section>
      </main>
    </ProfileCabinetShell>
  )
}

export default BannedRoomsPage
