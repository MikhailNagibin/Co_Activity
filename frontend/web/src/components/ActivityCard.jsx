import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
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
  const interactionProps = {
    onMouseEnter: handleImageCycleStart,
    onMouseLeave: handleImageCycleStop,
    onFocus: handleImageCycleStart,
    onBlur: handleImageCycleStop,
  }

  const body = (
    <>
      <div className="activity-card-image-shell">
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
      <p>{item.date}</p>
      <p>{item.capacity}</p>
      <div className="activity-card-author">
        <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
        <h5>{item.author}</h5>
      </div>
    </>
  )

  if (item.linkTo) {
    return (
      <Link className="activity-card-outer-link" to={item.linkTo} {...interactionProps}>
        <article className="activity-card">{body}</article>
      </Link>
    )
  }

  return <article className="activity-card" {...interactionProps}>{body}</article>
}

export default ActivityCard
