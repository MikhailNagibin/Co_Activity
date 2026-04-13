import AppHeader from '../components/AppHeader.jsx'
import RoomForm from '../components/RoomForm.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  deleteRoomImage,
  getRoomById,
  getRoomMembershipStatus,
  updateRoom,
  uploadRoomImages,
} from '../services/roomsService.js'
import { getRoomMembershipView } from '../services/uiMappers.js'
import {
  buildRoomPayload,
  createRoomFormState,
  getProblemDetailsMessage,
  resolveRoomImageUrl,
  roomToFormState,
  sortRoomImages,
  validateRoomForm,
} from '../utils/roomForm.js'

function RoomEditPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const { roomId: roomIdParam } = useParams()
  const roomId = Number.parseInt(String(roomIdParam), 10)

  const [room, setRoom] = useState(null)
  const [membership, setMembership] = useState(null)
  const [formData, setFormData] = useState(() => createRoomFormState())
  const [images, setImages] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isUploadingImages, setIsUploadingImages] = useState(false)
  const [deletingImageId, setDeletingImageId] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [imageFeedback, setImageFeedback] = useState('')

  const loadRoom = useCallback(async () => {
    if (!Number.isFinite(roomId) || roomId < 1) {
      setErrorMessage('Некорректный идентификатор активности')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setErrorMessage('')

    try {
      const payload = await getRoomById(roomId)
      let membershipPayload = payload?.membershipStatus ?? null

      try {
        membershipPayload = await getRoomMembershipStatus(roomId)
      } catch (membershipError) {
        if (isUnauthorizedApiError(membershipError)) {
          redirectToSignInForExpiredSession(navigate, {
            next: `/rooms/${encodeURIComponent(String(roomId))}/edit`,
          })
          return
        }
      }

      setRoom(payload)
      setMembership(membershipPayload)
      setFormData(roomToFormState(payload))
      setImages(sortRoomImages(payload?.images))
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}/edit`,
        })
        return
      }

      if (isApiError(error)) {
        setErrorMessage(getProblemDetailsMessage(error, 'Не удалось загрузить комнату для редактирования.'))
      } else {
        setErrorMessage('Не удалось загрузить комнату для редактирования.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [navigate, roomId])

  useEffect(() => {
    if (!isAuthenticated) {
      return
    }
    loadRoom()
  }, [isAuthenticated, loadRoom])

  const membershipView = useMemo(() => getRoomMembershipView(room, membership), [membership, room])
  const canEditRoom = membershipView.canModerate

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

    const validationMessage = validateRoomForm(formData, { requireStatus: true })
    if (validationMessage) {
      setErrorMessage(validationMessage)
      return
    }

    setIsSubmitting(true)
    try {
      await updateRoom(roomId, buildRoomPayload(formData, { includeStatus: true }))
      navigate(`/rooms/${roomId}`, { replace: true })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}/edit`,
        })
        return
      }

      if (isApiError(error)) {
        setErrorMessage(getProblemDetailsMessage(error, 'Не удалось сохранить изменения комнаты.'))
      } else {
        setErrorMessage('Не удалось сохранить изменения комнаты.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleUploadImages = async (event) => {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    if (files.length === 0) {
      return
    }

    setImageFeedback('')
    setIsUploadingImages(true)

    try {
      const response = await uploadRoomImages(roomId, files)
      setImages(sortRoomImages(response))
      setImageFeedback(`Загружено файлов: ${files.length}`)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}/edit`,
        })
        return
      }

      if (isApiError(error)) {
        setImageFeedback(getProblemDetailsMessage(error, 'Не удалось загрузить изображения.'))
      } else {
        setImageFeedback('Не удалось загрузить изображения.')
      }
    } finally {
      setIsUploadingImages(false)
    }
  }

  const handleDeleteImage = async (imageId) => {
    if (!imageId) {
      return
    }

    setImageFeedback('')
    setDeletingImageId(imageId)

    try {
      const response = await deleteRoomImage(roomId, imageId)
      setImages(sortRoomImages(response))
      setImageFeedback('Изображение удалено.')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}/edit`,
        })
        return
      }

      if (isApiError(error)) {
        setImageFeedback(getProblemDetailsMessage(error, 'Не удалось удалить изображение.'))
      } else {
        setImageFeedback('Не удалось удалить изображение.')
      }
    } finally {
      setDeletingImageId(null)
    }
  }

  if (isLoading) {
    return (
      <>
        <AppHeader activeTab="main" />
        <div className="virtual-elem"></div>
        <main className="create-room-page">
          <p className="room-activity-loading">Загрузка...</p>
        </main>
      </>
    )
  }

  if (errorMessage && !room) {
    return (
      <>
        <AppHeader activeTab="main" />
        <div className="virtual-elem"></div>
        <main className="create-room-page">
          <p className="room-activity-error" role="alert">
            {errorMessage}
          </p>
          <Link className="back-link" to={`/rooms/${roomId}`}>
            ← Вернуться к активности
          </Link>
        </main>
      </>
    )
  }

  if (!canEditRoom) {
    return (
      <>
        <AppHeader activeTab="main" />
        <div className="virtual-elem"></div>
        <main className="create-room-page">
          <p className="room-activity-error" role="alert">
            Недостаточно прав для редактирования этой комнаты.
          </p>
          <Link className="back-link" to={`/rooms/${roomId}`}>
            ← Вернуться к активности
          </Link>
        </main>
      </>
    )
  }

  return (
    <>
      <AppHeader activeTab="main" />
      <div className="virtual-elem"></div>
      <div className="create-room-page room-editor-shell">
        <Link className="back-link" to={`/rooms/${roomId}`}>
          ← Вернуться к активности
        </Link>
      </div>
      <RoomForm
        title="Редактирование активности"
        subtitle="Обновите данные комнаты, статус и изображения. Изменения отразятся на детальной странице после сохранения."
        formData={formData}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        submitLabel="Сохранить изменения"
        onFieldChange={handleFieldChange}
        onSubmit={handleSubmit}
        showStatus
      >
        <section className="room-editor-images" aria-labelledby="room-editor-images-title">
          <div className="room-editor-images__header">
            <div>
              <h3 id="room-editor-images-title">Изображения комнаты</h3>
              <p className="gray-elem">Можно загружать несколько файлов сразу. Порядок показывается, если он пришёл от API.</p>
            </div>
            <label className="room-editor-images__upload">
              <input
                type="file"
                accept="image/*"
                multiple
                disabled={isUploadingImages || isSubmitting}
                onChange={handleUploadImages}
              />
              {isUploadingImages ? 'Загрузка...' : 'Загрузить файлы'}
            </label>
          </div>

          {imageFeedback ? (
            <p
              className={`room-editor-images__feedback${imageFeedback.includes('Не удалось') ? ' room-editor-images__feedback--error' : ''}`}
              role="status"
            >
              {imageFeedback}
            </p>
          ) : null}

          {images.length > 0 ? (
            <div className="room-editor-images__grid">
              {images.map((image) => (
                <article key={image.id ?? image.url} className="room-editor-images__card">
                  <img
                    src={resolveRoomImageUrl(image.url)}
                    alt={`Изображение активности ${room?.name ?? ''}`}
                    className="room-editor-images__preview"
                  />
                  <div className="room-editor-images__meta">
                    <span>ID: {image.id ?? '—'}</span>
                    <span>Порядок: {image.order ?? '—'}</span>
                  </div>
                  <button
                    type="button"
                    className="room-delete-button"
                    disabled={deletingImageId === image.id || isSubmitting || isUploadingImages}
                    onClick={() => handleDeleteImage(image.id)}
                  >
                    {deletingImageId === image.id ? 'Удаление...' : 'Удалить'}
                  </button>
                </article>
              ))}
            </div>
          ) : (
            <p className="gray-elem">Изображений пока нет.</p>
          )}
        </section>
      </RoomForm>
    </>
  )
}

export default RoomEditPage
