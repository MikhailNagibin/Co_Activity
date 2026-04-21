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
import { getRoomMembershipView, normalizeMembershipRole } from '../services/uiMappers.js'
import {
  buildRoomUpdatePayloadFromSnapshot,
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
  /** PUT /api/rooms/{id} только для OWNER; админ не попадает на эту форму. */
  const canEditRoom = normalizeMembershipRole(membershipView.membershipRole) === 'OWNER'

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
      await updateRoom(roomId, buildRoomUpdatePayloadFromSnapshot(room, formData, { includeStatus: true }))
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
      setImageFeedback('')
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
      setImageFeedback('')
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
      <RoomForm
        lead={
          <div className="room-editor-backline">
            <Link className="back-link" to={`/rooms/${roomId}`}>
              ← Вернуться к активности
            </Link>
            <p className="gray-elem room-editor-static-hint">
              Название, категория, описание, тип доступа и возрастной рейтинг после создания не меняются.
            </p>
          </div>
        }
        title="Редактирование активности"
        formData={formData}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        submitLabel="Сохранить изменения"
        onFieldChange={handleFieldChange}
        onSubmit={handleSubmit}
        showStatus
        staticActivityFieldsLocked
      >
        <section className="room-editor-image-tab" aria-labelledby="room-editor-image-tab-title">
          <h3 className="room-editor-image-tab__tab" id="room-editor-image-tab-title">
            Изображения комнаты
          </h3>
          <div className="room-editor-image-tab__body">
            <div className="room-editor-images__header room-editor-image-tab__header">
              <p className="gray-elem room-editor-image-tab__hint">
                Можно выбрать несколько файлов за раз.
              </p>
              <label className="room-editor-images__upload room-editor-image-tab__upload">
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
                className="room-editor-images__feedback room-editor-images__feedback--error room-editor-image-tab__feedback"
                role="alert"
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
                    <button
                      type="button"
                      className="room-delete-button room-editor-image-tab__delete"
                      disabled={deletingImageId === image.id || isSubmitting || isUploadingImages}
                      onClick={() => handleDeleteImage(image.id)}
                    >
                      {deletingImageId === image.id ? 'Удаление...' : 'Удалить'}
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              <p className="gray-elem room-editor-image-tab__empty">Изображений пока нет.</p>
            )}
          </div>
        </section>
      </RoomForm>
    </>
  )
}

export default RoomEditPage
