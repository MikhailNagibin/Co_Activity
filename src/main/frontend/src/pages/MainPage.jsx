import AppHeader from '../components/AppHeader.jsx'
import ActivityCard from '../components/ActivityCard.jsx'
import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ApiError } from '../api/httpClient.js'
import { getRooms } from '../services/roomsService.js'
import { mapRoomsToActivityCards } from '../services/uiMappers.js'

function MainPage() {
  const location = useLocation()
  const [activities, setActivities] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

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
      <section className="main-hero">
        <h2>Исследуйте активности</h2>
        <h3 className="gray-elem">Найдите партнеров по хобби, проектам, интересам</h3>
      </section>

      <main className="main-page-content">
        <div className="search-wrapper">
          <button className="search-button" type="button" aria-label="Поиск">
            <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
          </button>
          <input placeholder="Поиск активностей..." type="text" />
        </div>

        <select name="categories" defaultValue="all-categories">
          <option value="all-categories">Все категории</option>
          <option value="sport">Спорт</option>
          <option value="music">Музыка</option>
          <option value="art">Искусство</option>
          <option value="entertainment">Развлечения</option>
          <option value="business">Бизнес</option>
          <option value="education">Образование</option>
          <option value="active-recreation">Активный отдых</option>
          <option value="passive-recreation">Пассивный отдых</option>
          <option value="others">Другое</option>
        </select>

        <button type="button">Фильтры</button>

        <Link className="main-create-activity-btn" to="/create-room">
          Создать активность
        </Link>

        <section className="cards">
          {isLoading ? <p>Загрузка активностей...</p> : null}
          {!isLoading && errorMessage ? <p style={{ color: '#b00020' }}>{errorMessage}</p> : null}
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
    </>
  )
}

export default MainPage
