import AppHeader from '../components/AppHeader.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { ROOM_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { createRoom } from '../services/roomsService.js'

function localDateTimeToInstantIso(value) {
  if (!value || typeof value !== 'string') {
    return null
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return null
  }
  return parsed.toISOString()
}

function CreateRoomPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: 'Sport',
    isPublic: true,
    maximumNumberOfPeople: 10,
    chatLink: '',
    dateOfStartEvent: '',
    dateOfEndEvent: '',
    ageRating: 0,
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleFieldChange = (event) => {
    const { name, value, type, checked } = event.target
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    if (!isAuthenticated) {
      setErrorMessage('Войдите в аккаунт, чтобы создать событие')
      return
    }

    const name = formData.name.trim()
    const description = formData.description.trim()
    if (name.length < 3 || name.length > 100) {
      setErrorMessage('Название: от 3 до 100 символов')
      return
    }
    if (!description) {
      setErrorMessage('Пожалуйста, заполните описание')
      return
    }
    if (description.length > 2000) {
      setErrorMessage('Описание не длиннее 2000 символов')
      return
    }

    const maxPeople = Number.parseInt(String(formData.maximumNumberOfPeople), 10)
    if (!Number.isFinite(maxPeople) || maxPeople < 2 || maxPeople > 100000) {
      setErrorMessage('Максимум участников: от 2 до 100 000')
      return
    }

    const dateOfStartEvent = formData.dateOfStartEvent
      ? localDateTimeToInstantIso(formData.dateOfStartEvent)
      : null
    const dateOfEndEvent = formData.dateOfEndEvent
      ? localDateTimeToInstantIso(formData.dateOfEndEvent)
      : null

    if (dateOfStartEvent && dateOfEndEvent) {
      if (new Date(dateOfEndEvent) <= new Date(dateOfStartEvent)) {
        setErrorMessage('Окончание должно быть позже начала')
        return
      }
    }

    const nowMs = Date.now()
    const slackMs = 60_000
    if (dateOfStartEvent && new Date(dateOfStartEvent).getTime() < nowMs - slackMs) {
      setErrorMessage('Дата и время начала не могут быть в прошлом')
      return
    }
    if (dateOfEndEvent && new Date(dateOfEndEvent).getTime() <= nowMs) {
      setErrorMessage('Дата окончания должна быть в будущем')
      return
    }

    const chatLink = formData.chatLink.trim()
    const ageRating = Number.parseInt(String(formData.ageRating), 10)
    const safeAge = Number.isFinite(ageRating) ? Math.min(21, Math.max(0, ageRating)) : 0

    const payload = {
      isPublic: formData.isPublic,
      category: formData.category,
      name,
      description,
      maximumNumberOfPeople: maxPeople,
      chatLink: chatLink || null,
      dateOfStartEvent,
      dateOfEndEvent,
      frequency: null,
      ageRating: safeAge,
    }

    setIsSubmitting(true)
    try {
      await createRoom(payload)
      navigate('/main')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/create-room' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось создать событие. Попробуйте снова.'))
      } else {
        setErrorMessage('Не удалось создать событие. Попробуйте снова.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <AppHeader activeTab="main" authActionLabel="Войти" authActionTo="/sign-in" />
      <section className="main-hero">
        <h2>Новое событие</h2>
        <h3 className="gray-elem">Настройте комнату и появитесь в ленте активностей (если событие публичное)</h3>
      </section>

      <main className="create-room-page">
        {!isAuthenticated ? (
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы создать событие, или{' '}
            <Link to="/sign-up">зарегистрируйтесь</Link>.
          </p>
        ) : null}

        <form className="create-room-form" onSubmit={handleSubmit}>
          <div className="create-room-form-row">
            <label htmlFor="name">Название</label>
            <input
              id="name"
              name="name"
              type="text"
              autoComplete="off"
              minLength={3}
              maxLength={100}
              value={formData.name}
              onChange={handleFieldChange}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="create-room-form-row">
            <label htmlFor="category">Категория</label>
            <StyledDropdown
              variant="form"
              id="category"
              ariaLabel="Категория активности"
              options={ROOM_CATEGORY_OPTIONS}
              value={formData.category}
              onChange={(next) =>
                setFormData((prev) => ({
                  ...prev,
                  category: next,
                }))
              }
              disabled={isSubmitting}
            />
          </div>

          <div className="create-room-form-row">
            <label htmlFor="description">Описание</label>
            <textarea
              id="description"
              name="description"
              rows={6}
              maxLength={2000}
              value={formData.description}
              onChange={handleFieldChange}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="create-room-form-row split-fields">
            <div>
              <label htmlFor="maximumNumberOfPeople">Максимум участников</label>
              <input
                id="maximumNumberOfPeople"
                name="maximumNumberOfPeople"
                type="number"
                min={2}
                max={100000}
                value={formData.maximumNumberOfPeople}
                onChange={handleFieldChange}
                disabled={isSubmitting}
                required
              />
            </div>
            <div>
              <label htmlFor="ageRating">Возрастной рейтинг (0–21)</label>
              <input
                id="ageRating"
                name="ageRating"
                type="number"
                min={0}
                max={21}
                value={formData.ageRating}
                onChange={handleFieldChange}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="create-room-form-row split-fields">
            <div>
              <label htmlFor="dateOfStartEvent">Начало (необязательно)</label>
              <input
                id="dateOfStartEvent"
                name="dateOfStartEvent"
                type="datetime-local"
                value={formData.dateOfStartEvent}
                onChange={handleFieldChange}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label htmlFor="dateOfEndEvent">Окончание (необязательно)</label>
              <input
                id="dateOfEndEvent"
                name="dateOfEndEvent"
                type="datetime-local"
                value={formData.dateOfEndEvent}
                onChange={handleFieldChange}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="create-room-form-row">
            <label htmlFor="chatLink">Ссылка на чат (необязательно)</label>
            <input
              id="chatLink"
              name="chatLink"
              type="url"
              placeholder="https://..."
              value={formData.chatLink}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            />
          </div>

          <div className="create-room-form-row create-room-checkbox-row">
            <label htmlFor="isPublic">
              <input
                id="isPublic"
                name="isPublic"
                type="checkbox"
                checked={formData.isPublic}
                onChange={handleFieldChange}
                disabled={isSubmitting}
              />
              Публичное событие (видно в общей ленте)
            </label>
          </div>

          {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}

          <button
            type="submit"
            className="create-room-submit"
            disabled={isSubmitting || !isAuthenticated}
          >
            {isSubmitting ? 'Создание...' : 'Создать событие'}
          </button>
        </form>
      </main>
    </>
  )
}

export default CreateRoomPage
