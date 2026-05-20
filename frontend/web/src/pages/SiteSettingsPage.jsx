import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthSession } from '../auth/authSessionContext.js'
import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import {
  getShowActivitiesNotMeetingAgeRatingFromStorage,
  setShowActivitiesNotMeetingAgeRatingInStorage,
} from '../utils/browseAgePreference.js'

function SiteSettingsPage() {
  const { isAuthenticated } = useAuthSession()
  const [showActivitiesNotMeetingAgeRating, setShowActivitiesNotMeetingAgeRating] = useState(
    () => getShowActivitiesNotMeetingAgeRatingFromStorage(),
  )

  const handleSubmit = (event) => {
    event.preventDefault()
    setShowActivitiesNotMeetingAgeRatingInStorage(showActivitiesNotMeetingAgeRating)
  }

  return (
    <ProfileCabinetShell
      heroTitle="Настройки сайта"
      heroSubtitle="Выберите, показывать ли активности с возрастным рейтингом выше вашего возраста."
    >
      <main className="profile-page">
        {!isAuthenticated ? (
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы изменить настройки сайта.
          </p>
        ) : null}

        {isAuthenticated ? (
          <section className="profile-panel">
            <p className="profile-kicker">Фильтр по возрастному рейтингу</p>
            <h3>Показывать комнаты с более высоким рейтингом</h3>
            <form onSubmit={handleSubmit} className="profile-form">
              <div className="profile-form-row profile-form-row--checkbox">
                <label className="profile-checkbox-label" htmlFor="showActivitiesNotMeetingAgeRating">
                  <input
                    id="showActivitiesNotMeetingAgeRating"
                    name="showActivitiesNotMeetingAgeRating"
                    type="checkbox"
                    checked={showActivitiesNotMeetingAgeRating}
                    onChange={(event) => setShowActivitiesNotMeetingAgeRating(event.target.checked)}
                  />
                  <span>Согласен показывать комнаты, где возрастной рейтинг выше моего возраста</span>
                </label>
              </div>
              <button type="submit" className="create-room-submit">
                Сохранить
              </button>
            </form>
          </section>
        ) : null}
      </main>
    </ProfileCabinetShell>
  )
}

export default SiteSettingsPage
