import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import ProfileRoomCard from '../components/ProfileRoomCard.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { isApiError } from '../api/httpClient.js'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import {
  AVAILABILITY_FILTER_OPTIONS,
  MAIN_ACTIVITY_SORT_OPTIONS,
  VISIBILITY_FILTER_OPTIONS,
} from '../constants/browseFilterOptions.js'
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
import { useConfirmDialog } from '../hooks/useConfirmDialog.jsx'

const DEFAULT_SORT = 'created-desc'
const MAX_AGE_FILTER = 21

const ROLE_FILTER_OPTIONS = [
  { value: 'all', label: 'Все роли' },
  { value: 'OWNER', label: 'Создатель' },
  { value: 'ADMIN', label: 'Админ' },
  { value: 'PARTICIPANT', label: 'Участник' },
]

function sanitizeAgeCeilingInput(rawValue) {
  const trimmed = String(rawValue ?? '').trim()
  if (trimmed === '') {
    return { value: '', error: '' }
  }

  if (!/^\d{1,3}$/.test(trimmed)) {
    return { value: trimmed.replace(/[^\d]/g, '').slice(0, 3), error: 'Введите целое число от 0 до 21.' }
  }

  const numeric = Number(trimmed)
  if (!Number.isFinite(numeric)) {
    return { value: '', error: 'Введите целое число от 0 до 21.' }
  }

  if (numeric < 0) {
    return { value: '0', error: 'Возрастной рейтинг не может быть отрицательным.' }
  }

  if (numeric > MAX_AGE_FILTER) {
    return { value: String(MAX_AGE_FILTER), error: 'Допустимы значения от 0 до 21.' }
  }

  return { value: String(numeric), error: '' }
}

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
  const { requestConfirm, confirmDialog } = useConfirmDialog()
  const [rooms, setRooms] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [isActingOnRoomId, setIsActingOnRoomId] = useState(null)
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState(DEFAULT_SORT)
  const [roleFilter, setRoleFilter] = useState('all')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [visibilityFilter, setVisibilityFilter] = useState('all')
  const [availabilityFilter, setAvailabilityFilter] = useState('all')
  const [ageCeiling, setAgeCeiling] = useState('')
  const [ageCeilingError, setAgeCeilingError] = useState('')
  const [organizerCity, setOrganizerCity] = useState('')
  const [organizerCountry, setOrganizerCountry] = useState('')

  const loadRooms = useCallback(async (options = {}) => {
    const { silent = false } = options
    if (!silent) {
      setIsLoading(true)
      setErrorMessage('')
    }

    try {
      const payload = await getMyRooms()
      const statuses = await loadMembershipStatuses(Array.isArray(payload) ? payload : [])
      const mergedRooms = mergeRoomsWithMembershipStatuses(payload, statuses)
      setRooms(mapRoomsToActivityCards(mergedRooms))
      if (silent) {
        setErrorMessage('')
      }
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
      if (!silent) {
        setRooms([])
      }
    } finally {
      if (!silent) {
        setIsLoading(false)
      }
    }
  }, [navigate])

  useEffect(() => {
    loadRooms()
  }, [loadRooms])

  const filteredRooms = useMemo(() => {
    const browseFiltered = filterActivityCardsForBrowse(rooms, {
      categoryFilter,
      searchQuery,
      visibilityFilter,
      availabilityFilter,
      ageCeiling,
      organizerCity,
      organizerCountry,
    })
    const roleFiltered =
      roleFilter === 'all'
        ? browseFiltered
        : browseFiltered.filter((item) => item.membershipRole === roleFilter)

    return sortActivityCards(roleFiltered, sortBy)
  }, [
    ageCeiling,
    availabilityFilter,
    categoryFilter,
    organizerCity,
    organizerCountry,
    roleFilter,
    rooms,
    searchQuery,
    sortBy,
    visibilityFilter,
  ])

  const handleAgeCeilingChange = (event) => {
    const { value, error } = sanitizeAgeCeilingInput(event.target.value)
    setAgeCeiling(value)
    setAgeCeilingError(error)
  }

  const handleResetFilters = () => {
    setCategoryFilter('all-categories')
    setSearchQuery('')
    setSortBy(DEFAULT_SORT)
    setRoleFilter('all')
    setVisibilityFilter('all')
    setAvailabilityFilter('all')
    setAgeCeiling('')
    setAgeCeilingError('')
    setOrganizerCity('')
    setOrganizerCountry('')
  }

  const handleLeaveRoom = async (room) => {
    if (!room?.id) {
      return
    }

    const ok = await requestConfirm({
      title: 'Покинуть активность',
      message: `Покинуть активность «${room.title}»?`,
      confirmLabel: 'Покинуть',
      variant: 'primary',
    })
    if (!ok) {
      return
    }

    setIsActingOnRoomId(room.id)
    setErrorMessage('')

    try {
      await leaveRoom(room.id)
      await loadRooms({ silent: true })
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

    const ok = await requestConfirm({
      title: 'Удаление активности',
      message: `Удалить активность «${room.title}» без возможности восстановления?`,
      confirmLabel: 'Удалить',
      variant: 'danger',
    })
    if (!ok) {
      return
    }

    setIsActingOnRoomId(room.id)
    setErrorMessage('')

    try {
      await deleteRoom(room.id)
      await loadRooms({ silent: true })
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
      <ProfileCabinetShell
        heroTitle="Мои активности"
        heroSubtitle="Фильтруйте свои комнаты по роли, категории и поиску"
      >
        <main className="profile-list-page">
          <div className={`main-toolbar-shell${filtersOpen ? ' main-toolbar-shell--filters-open' : ''}`}>
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

              <button
                type="button"
                className={`main-filters-btn${filtersOpen ? ' main-filters-btn--active' : ''}`}
                aria-expanded={filtersOpen}
                onClick={() => setFiltersOpen((prev) => !prev)}
              >
                Фильтры
              </button>
            </div>

            {filtersOpen ? (
              <div
                className="main-browse-filters-panel"
                role="region"
                aria-label="Дополнительные фильтры моих активностей"
              >
                <div className="main-browse-filters-grid">
                  <div className="main-browse-filters-field">
                    <span className="main-browse-filters-label">Доступ</span>
                    <StyledDropdown
                      variant="toolbar"
                      id="my-rooms-visibility-filter"
                      ariaLabel="Фильтр по типу доступа"
                      options={VISIBILITY_FILTER_OPTIONS}
                      value={visibilityFilter}
                      onChange={setVisibilityFilter}
                    />
                  </div>
                  <div className="main-browse-filters-field">
                    <span className="main-browse-filters-label">Места</span>
                    <StyledDropdown
                      variant="toolbar"
                      id="my-rooms-availability-filter"
                      ariaLabel="Фильтр по заполненности"
                      options={AVAILABILITY_FILTER_OPTIONS}
                      value={availabilityFilter}
                      onChange={setAvailabilityFilter}
                    />
                  </div>
                  <label className="main-browse-filters-field main-browse-filters-field--text">
                    <span className="main-browse-filters-label">Возрастной рейтинг активности</span>
                    <p className="main-browse-filters-hint-inline">
                      Введите верхнюю границу вручную. Сохраняются только активности не выше этого рейтинга.
                    </p>
                    <input
                      id="my-rooms-age-ceiling-filter"
                      type="number"
                      min="0"
                      max={String(MAX_AGE_FILTER)}
                      step="1"
                      className="main-browse-filters-input"
                      value={ageCeiling}
                      onChange={handleAgeCeilingChange}
                      placeholder="Например, 16"
                      inputMode="numeric"
                      aria-invalid={ageCeilingError ? 'true' : 'false'}
                    />
                    {ageCeilingError ? (
                      <p className="main-browse-filters-error" role="alert">
                        {ageCeilingError}
                      </p>
                    ) : null}
                  </label>
                  <label className="main-browse-filters-field main-browse-filters-field--text">
                    <span className="main-browse-filters-label">Город организатора</span>
                    <input
                      type="text"
                      className="main-browse-filters-input"
                      value={organizerCity}
                      onChange={(event) => setOrganizerCity(event.target.value)}
                      placeholder="Например, Москва"
                      autoComplete="address-level2"
                    />
                  </label>
                  <label className="main-browse-filters-field main-browse-filters-field--text">
                    <span className="main-browse-filters-label">Страна организатора</span>
                    <input
                      type="text"
                      className="main-browse-filters-input"
                      value={organizerCountry}
                      onChange={(event) => setOrganizerCountry(event.target.value)}
                      placeholder="Например, Россия"
                      autoComplete="country-name"
                    />
                  </label>
                </div>
                <div className="main-browse-filters-actions">
                  <p className="main-browse-filters-hint-note">
                    Дополнительные фильтры действуют на список, который уже загружен на этой странице.
                  </p>
                  <button type="button" className="main-browse-filters-reset" onClick={handleResetFilters}>
                    Сбросить поиск и фильтры
                  </button>
                </div>
              </div>
            ) : null}
          </div>

          {errorMessage ? (
            <p className="profile-list-message profile-list-message--error" role="alert">
              {errorMessage}
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
      </ProfileCabinetShell>
      {confirmDialog}
    </>
  )
}

export default MyRoomsPage
