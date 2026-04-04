import AppHeader from '../components/AppHeader.jsx'
import ActivityCard from '../components/ActivityCard.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ApiError } from '../api/httpClient.js'
import { getAccessToken } from '../api/tokenStorage.js'
import { getRooms } from '../services/roomsService.js'
import { mapRoomsToActivityCards } from '../services/uiMappers.js'

function MainPage() {
  const location = useLocation()
  const isAuthenticated = Boolean(getAccessToken())
  const [activities, setActivities] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')

  useEffect(() => {
    let isMounted = true

    const loadRooms = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const payload = await getRooms()
        if (!isMounted) {
          return
        }
        setActivities(mapRoomsToActivityCards(payload))
      } catch (error) {
        if (!isMounted) {
          return
        }
        if (error instanceof ApiError) {
          setErrorMessage(error.message)
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
  }, [location.key])

  return (
    <>
      <AppHeader activeTab="main" />
      <div className="main-page-shell">
        <section className="main-hero">
          <h2>Исследуйте активности</h2>
          <h3>Найдите партнеров по хобби, проектам, интересам</h3>
        </section>

        <main className="main-page-content">
          <div className="main-toolbar">
            <div className="search-wrapper">
              <button className="search-button" type="button" aria-label="Поиск">
                <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
              </button>
              <input placeholder="Поиск активностей..." type="search" name="q" autoComplete="off" />
            </div>

            <StyledDropdown
              variant="toolbar"
              id="main-category-filter"
              ariaLabel="Категория"
              options={BROWSE_CATEGORY_OPTIONS}
              value={categoryFilter}
              onChange={setCategoryFilter}
            />

            <button type="button" className="main-filters-btn">
              Фильтры
            </button>

            {isAuthenticated ? (
              <Link className="main-create-activity-btn" to="/create-room">
                Создать активность
              </Link>
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
          {!isLoading && !errorMessage
            ? activities.map((item, index) => (
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
