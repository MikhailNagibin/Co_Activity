import AppHeader from '../components/AppHeader.jsx'
import ActivityCard from '../components/ActivityCard.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import {
  AGE_CEILING_FILTER_OPTIONS,
  AVAILABILITY_FILTER_OPTIONS,
  MAIN_ACTIVITY_SORT_OPTIONS,
  VISIBILITY_FILTER_OPTIONS,
} from '../constants/browseFilterOptions.js'
import {
  filterActivityCardsForBrowse,
  sortActivityCards,
} from '../utils/browseListFilters.js'
import { buildRoomsListQueryParams } from '../utils/roomsBrowseQuery.js'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { useAuthSession } from '../auth/authSessionContext.js'
import { getRooms } from '../services/roomsService.js'
import { mapRoomsToActivityCards } from '../services/uiMappers.js'

const DEFAULT_SORT = 'created-desc'

function MainPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [activities, setActivities] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState(DEFAULT_SORT)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [visibilityFilter, setVisibilityFilter] = useState('all')
  const [availabilityFilter, setAvailabilityFilter] = useState('all')
  const [ageCeiling, setAgeCeiling] = useState('all')
  const [organizerCity, setOrganizerCity] = useState('')
  const [organizerCountry, setOrganizerCountry] = useState('')
  const serverQuery = useMemo(
    () => buildRoomsListQueryParams({ categoryFilter, searchQuery, visibilityFilter, sortBy }),
    [categoryFilter, searchQuery, visibilityFilter, sortBy],
  )

  useEffect(() => {
    let isMounted = true

    const loadRooms = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const payload = await getRooms({ query: serverQuery })
        if (!isMounted) {
          return
        }
        setActivities(mapRoomsToActivityCards(payload))
      } catch (error) {
        if (!isMounted) {
          return
        }
        if (isUnauthorizedApiError(error)) {
          const next = `${location.pathname}${location.search || ''}`
          redirectToSignInForExpiredSession(navigate, {
            next: next.startsWith('/') && !next.startsWith('//') ? next : '/main',
          })
          return
        }
        if (isApiError(error)) {
          setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить активности'))
        } else {
          setErrorMessage('Не удалось загрузить активности')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    loadRooms()

    return () => {
      isMounted = false
    }
  }, [location.key, location.pathname, location.search, navigate, serverQuery])

  const filteredActivities = useMemo(() => {
    const filtered = filterActivityCardsForBrowse(activities, {
      categoryFilter,
      searchQuery,
      visibilityFilter,
      availabilityFilter,
      ageCeiling,
      organizerCity,
      organizerCountry,
    })
    return sortActivityCards(filtered, sortBy)
  }, [
    activities,
    categoryFilter,
    searchQuery,
    visibilityFilter,
    availabilityFilter,
    ageCeiling,
    organizerCity,
    organizerCountry,
    sortBy,
  ])

  const resetBrowseFilters = () => {
    setCategoryFilter('all-categories')
    setSearchQuery('')
    setSortBy(DEFAULT_SORT)
    setVisibilityFilter('all')
    setAvailabilityFilter('all')
    setAgeCeiling('all')
    setOrganizerCity('')
    setOrganizerCountry('')
  }

  return (
    <>
      <AppHeader activeTab="main" />
      <div className="main-page-shell">
        <section className="main-hero">
          <h2>Исследуйте активности</h2>
          <h3>Найдите партнеров по хобби, проектам, интересам</h3>
        </section>

        <main className="main-page-content">
          <div className={`main-toolbar-shell${filtersOpen ? ' main-toolbar-shell--filters-open' : ''}`}>
            <div className="main-toolbar">
              <div className="search-wrapper">
                <button className="search-button" type="button" aria-label="Поиск">
                  <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                </button>
                <input
                  placeholder="Название, ключевые слова, город организатора…"
                  type="search"
                  name="q"
                  autoComplete="off"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  aria-label="Поиск активностей по названию и ключевым словам"
                />
              </div>

              <StyledDropdown
                variant="toolbar"
                id="main-category-filter"
                ariaLabel="Категория"
                options={BROWSE_CATEGORY_OPTIONS}
                value={categoryFilter}
                onChange={setCategoryFilter}
              />

              <StyledDropdown
                variant="toolbar"
                className="styled-dropdown--sort-browse"
                id="main-sort"
                ariaLabel="Сортировка"
                options={MAIN_ACTIVITY_SORT_OPTIONS}
                value={sortBy}
                onChange={setSortBy}
              />

              <button
                type="button"
                className={`main-filters-btn${filtersOpen ? ' main-filters-btn--active' : ''}`}
                aria-expanded={filtersOpen}
                onClick={() => setFiltersOpen((v) => !v)}
              >
                Фильтры
              </button>

              {isAuthenticated ? (
                <Link className="main-create-activity-btn" to="/create-room">
                  Создать активность
                </Link>
              ) : null}
            </div>

            {filtersOpen ? (
              <div
                className="main-browse-filters-panel"
                role="region"
                aria-label="Дополнительные фильтры активностей"
              >
                <div className="main-browse-filters-grid">
                  <div className="main-browse-filters-field">
                    <span className="main-browse-filters-label">Доступ</span>
                    <StyledDropdown
                      variant="toolbar"
                      id="main-visibility-filter"
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
                      id="main-availability-filter"
                      ariaLabel="Фильтр по заполненности"
                      options={AVAILABILITY_FILTER_OPTIONS}
                      value={availabilityFilter}
                      onChange={setAvailabilityFilter}
                    />
                  </div>
                  <div className="main-browse-filters-field">
                    <span className="main-browse-filters-label">Возрастной рейтинг активности</span>
                    <p className="main-browse-filters-hint-inline" id="main-age-ceiling-hint">
                      Как в карточке активности: в списке остаются только те, у кого этот рейтинг{' '}
                      <strong>не больше</strong> выбранного значения (например, «Не выше 12+» скрывает 16+ и 18+).
                    </p>
                    <StyledDropdown
                      variant="toolbar"
                      id="main-age-ceiling-filter"
                      ariaLabel="Фильтр по возрастному рейтингу активности"
                      ariaDescribedBy="main-age-ceiling-hint"
                      options={AGE_CEILING_FILTER_OPTIONS}
                      value={ageCeiling}
                      onChange={setAgeCeiling}
                    />
                  </div>
                  <label className="main-browse-filters-field main-browse-filters-field--text">
                    <span className="main-browse-filters-label">Город организатора</span>
                    <input
                      type="text"
                      className="main-browse-filters-input"
                      value={organizerCity}
                      onChange={(e) => setOrganizerCity(e.target.value)}
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
                      onChange={(e) => setOrganizerCountry(e.target.value)}
                      placeholder="Например, Россия"
                      autoComplete="country-name"
                    />
                  </label>
                </div>
                <div className="main-browse-filters-actions">
                  <p className="main-browse-filters-server-note">
                    Категория, доступ и часть сортировок применяются на сервере; текстовый поиск, фильтры по
                    заполненности, возрастному потолку и по городу/стране организатора остаются клиентскими.
                  </p>
                  <button type="button" className="main-browse-filters-reset" onClick={resetBrowseFilters}>
                    Сбросить поиск и фильтры
                  </button>
                </div>
              </div>
            ) : null}
          </div>

          <section className="cards" aria-label="Список активностей">
            {isLoading ? <p>Загрузка активностей...</p> : null}
            {!isLoading && errorMessage ? (
              <p className="main-activities-error" role="alert">
                {errorMessage}
              </p>
            ) : null}
            {!isLoading && !errorMessage && activities.length === 0 ? (
              <p>Пока нет активностей</p>
            ) : null}
            {!isLoading && !errorMessage && activities.length > 0 && filteredActivities.length === 0 ? (
              <p>Ничего не найдено — измените поиск, категорию, сортировку или фильтры</p>
            ) : null}
            {!isLoading && !errorMessage
              ? filteredActivities.map((item, index) => (
                  <ActivityCard key={item.id ?? `${item.title}-${index}`} item={item} />
                ))
              : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default MainPage
