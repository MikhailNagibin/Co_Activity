import AppHeader from '../components/AppHeader.jsx'
import RoomForm from '../components/RoomForm.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { createRoom, uploadRoomImages } from '../services/roomsService.js'
import {
  buildRoomPayload,
  createRoomFormState,
  getProblemDetailsMessage,
  validateRoomForm,
} from '../utils/roomForm.js'
import { getUnsupportedImageFiles, revokeObjectUrl } from '../utils/mediaFiles.js'

function getCreatedRoomId(payload) {
  const parsed = Number.parseInt(String(payload?.roomId ?? payload?.id ?? ''), 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function CreateRoomPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [formData, setFormData] = useState(() => createRoomFormState())
  const [selectedImages, setSelectedImages] = useState([])
  const [imagePreviewUrls, setImagePreviewUrls] = useState([])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    return () => {
      imagePreviewUrls.forEach(revokeObjectUrl)
    }
  }, [imagePreviewUrls])

  const handleFieldChange = (event) => {
    const { name, value, type, checked } = event.target
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const resetSelectedImages = () => {
    imagePreviewUrls.forEach(revokeObjectUrl)
    setSelectedImages([])
    setImagePreviewUrls([])
  }

  const handleImagesChange = (event) => {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    setErrorMessage('')

    if (files.length === 0) {
      resetSelectedImages()
      return
    }

    const unsupportedFiles = getUnsupportedImageFiles(files)
    if (unsupportedFiles.length > 0) {
      resetSelectedImages()
      setErrorMessage('Изображения комнаты должны быть в формате PNG, JPEG или WEBP')
      return
    }

    imagePreviewUrls.forEach(revokeObjectUrl)
    setSelectedImages(files)
    setImagePreviewUrls(files.map((file) => URL.createObjectURL(file)))
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
      const createdRoom = await createRoom(payload)
      const roomId = getCreatedRoomId(createdRoom)

      if (selectedImages.length > 0 && roomId != null) {
        try {
          await uploadRoomImages(roomId, selectedImages)
        } catch (uploadError) {
          const uploadMessage = isApiError(uploadError)
            ? getProblemDetailsMessage(uploadError, 'Комната создана, но изображения не загрузились.')
            : 'Комната создана, но изображения не загрузились.'
          window.alert(`${uploadMessage} Вы можете повторить загрузку на странице редактирования.`)
          navigate(`/rooms/${roomId}/edit`, { replace: true })
          return
        }
      }

      resetSelectedImages()
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
      <div className="virtual-elem" aria-hidden="true"></div>
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
        subtitle=""
        showHero={false}
        formData={formData}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        isSubmitDisabled={!isAuthenticated}
        shellExtraClassName="create-room-form-shell"
        submitLabel="Создать событие"
        onFieldChange={handleFieldChange}
        onSubmit={handleSubmit}
      >
        <section className="room-create-images" aria-labelledby="room-create-images-title">
          <div className="room-create-images__header">
            <div>
              <h3 id="room-create-images-title">Изображения комнаты</h3>
            </div>
            <label className="room-editor-images__upload">
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp"
                multiple
                disabled={isSubmitting}
                onChange={handleImagesChange}
              />
              Выбрать изображения
            </label>
          </div>

          {imagePreviewUrls.length > 0 ? (
            <div className="room-create-images__grid">
              {imagePreviewUrls.map((previewUrl, index) => (
                <article
                  key={`${previewUrl}-${selectedImages[index]?.name ?? index}`}
                  className="room-create-images__card"
                >
                  <img
                    src={previewUrl}
                    alt={`Предпросмотр изображения ${index + 1}`}
                    className="room-create-images__preview"
                  />
                  <div className="room-create-images__meta">
                    <strong>{selectedImages[index]?.name ?? `Файл ${index + 1}`}</strong>
                    <span>
                      {selectedImages[index]?.size
                        ? `${Math.max(1, Math.round(selectedImages[index].size / 1024))} КБ`
                        : 'Размер неизвестен'}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <p className="gray-elem">Изображения пока не выбраны.</p>
          )}

          {selectedImages.length > 0 ? (
            <button
              type="button"
              className="profile-delete-account-link room-create-images__reset"
              onClick={resetSelectedImages}
              disabled={isSubmitting}
            >
              Очистить выбор
            </button>
          ) : null}
        </section>
      </RoomForm>
    </>
  )
}

export default CreateRoomPage
