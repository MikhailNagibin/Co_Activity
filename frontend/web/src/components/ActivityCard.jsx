import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import UserAvatar from './UserAvatar.jsx'
import { resolveRoomImageUrl } from '../utils/roomForm.js'

function ActivityCard({ item }) {
  const images = Array.isArray(item?.images)
    ? item.images
        .map((image) => resolveRoomImageUrl(image?.url))
        .filter((url) => typeof url === 'string' && url.trim() !== '')
    : []
  const [activeImageIndex, setActiveImageIndex] = useState(0)
  const [isCyclingImages, setIsCyclingImages] = useState(false)
  const hasMultipleImages = images.length > 1

  useEffect(() => {
    if (!hasMultipleImages || !isCyclingImages) {
      return undefined
    }
    const intervalId = window.setInterval(() => {
      setActiveImageIndex((prev) => (prev + 1) % images.length)
    }, 850)
    return () => window.clearInterval(intervalId)
  }, [hasMultipleImages, images.length, isCyclingImages])

  const handleImageCycleStart = () => {
    if (!hasMultipleImages) {
      return
    }
    setIsCyclingImages(true)
  }

  const handleImageCycleStop = () => {
    setIsCyclingImages(false)
    setActiveImageIndex(0)
  }

  const safeActiveImageIndex = images.length > 0 ? activeImageIndex % images.length : 0
  const activeImageUrl = images[safeActiveImageIndex] ?? ''
  const imageInteractionProps = {
    onMouseEnter: handleImageCycleStart,
    onMouseLeave: handleImageCycleStop,
    onFocus: handleImageCycleStart,
    onBlur: handleImageCycleStop,
  }

  const scheduleBlock = (() => {
    const start = item?.scheduleStartLabel
    const end = item?.scheduleEndLabel
    const fallback = item?.scheduleFallbackLabel
    if (start || end) {
      return (
        <div className="activity-card-schedule" aria-label="Даты проведения">
          {start ? (
            <p className="activity-card-schedule-line">
              <span className="activity-card-schedule-label">Начало:</span> {start}
            </p>
          ) : null}
          {end ? (
            <p className="activity-card-schedule-line">
              <span className="activity-card-schedule-label">Окончание:</span> {end}
            </p>
          ) : null}
        </div>
      )
    }
    if (fallback) {
      return (
        <p className="activity-card-schedule-line activity-card-schedule-line--fallback" aria-label="Дата">
          {fallback}
        </p>
      )
    }
    return item?.date ? <p>{item.date}</p> : null
  })()

  const body = (
    <>
      <div
        className="activity-card-image-shell"
        tabIndex={hasMultipleImages ? 0 : undefined}
        role={hasMultipleImages ? 'group' : undefined}
        aria-label={hasMultipleImages ? 'Фотографии активности, наведите для просмотра' : undefined}
        {...imageInteractionProps}
      >
        {activeImageUrl ? (
          <img
            src={activeImageUrl}
            alt={`Изображение активности ${item.title}`}
            className="activity-card-image"
            loading="lazy"
            decoding="async"
          />
        ) : (
          <div className="activity-card-image activity-card-image--placeholder" aria-hidden="true">
            <i className="fa-regular fa-image" />
          </div>
        )}
      </div>
      <h2 className="activity-card-title">{item.title}</h2>
      <p>{item.description}</p>
      <hr />
      <p>{item.category}</p>
      {scheduleBlock}
      <p>{item.capacity}</p>
      <div className="activity-card-author">
        <UserAvatar user={item.creatorUser} alt={`Аватар, ${item.author}`} size="sm" />
        <h5>{item.author}</h5>
      </div>
    </>
  )

  if (item.linkTo) {
    return (
      <Link className="activity-card-outer-link" to={item.linkTo}>
        <article className="activity-card">{body}</article>
      </Link>
    )
  }

  return <article className="activity-card">{body}</article>
}

export default ActivityCard
