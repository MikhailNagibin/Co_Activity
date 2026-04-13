import AppHeader from '../components/AppHeader.jsx'
import RoomForm from '../components/RoomForm.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { createRoom } from '../services/roomsService.js'
import {
  buildRoomPayload,
  createRoomFormState,
  getProblemDetailsMessage,
  validateRoomForm,
} from '../utils/roomForm.js'

function CreateRoomPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [formData, setFormData] = useState(() => createRoomFormState())
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

    const validationMessage = validateRoomForm(formData)
    if (validationMessage) {
      setErrorMessage(validationMessage)
      return
    }

    const payload = buildRoomPayload(formData)

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
        setErrorMessage(getProblemDetailsMessage(error, 'Не удалось создать событие. Попробуйте снова.'))
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
      {!isAuthenticated ? (
        <main className="create-room-page">
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы создать событие, или{' '}
            <Link to="/sign-up">зарегистрируйтесь</Link>.
          </p>
        </main>
      ) : null}

      <RoomForm
        title="Новое событие"
        subtitle="Настройте комнату и появитесь в ленте активностей (если событие публичное)"
        formData={formData}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        isSubmitDisabled={!isAuthenticated}
        submitLabel="Создать событие"
        onFieldChange={handleFieldChange}
        onSubmit={handleSubmit}
      />
    </>
  )
}

export default CreateRoomPage
