import { useCallback, useState } from 'react'
import { resolveUserAvatarUrl } from '../utils/userProfile.js'

const SIZE_CLASS = {
  sm: 'user-avatar--sm',
  md: 'user-avatar--md',
  lg: 'user-avatar--lg',
  xl: 'user-avatar--xl',
}

function UserAvatarInner({ alt = '', className = '', size = 'md', tryUrl }) {
  const [imgFailed, setImgFailed] = useState(false)

  const handleError = useCallback(() => {
    setImgFailed(true)
  }, [])

  const sizeClass = SIZE_CLASS[size] ?? SIZE_CLASS.md
  const rootClass = ['user-avatar', sizeClass, className].filter(Boolean).join(' ')

  if (!tryUrl || imgFailed) {
    return (
      <span className={`${rootClass} user-avatar--placeholder`.trim()} aria-hidden="true">
        <i className="fa-regular fa-circle-user user-avatar__icon" />
      </span>
    )
  }

  return (
    <span className={rootClass} role="img" aria-label={alt || undefined}>
      <img className="user-avatar__picture" src={tryUrl} alt="" onError={handleError} loading="lazy" decoding="async" />
    </span>
  )
}

/**
 * @param {object} props
 * @param {object | null | undefined} props.user — поля как у UserSummaryResponse (id, avatarId, avatarUrl, …)
 * @param {string} [props.alt] — краткое описание для скринридеров (например имя)
 * @param {string} [props.className]
 * @param {'sm'|'md'|'lg'|'xl'} [props.size]
 */
export default function UserAvatar({ user, alt = '', className = '', size = 'md' }) {
  const initialUrl = user ? resolveUserAvatarUrl(user) : ''
  const userId = user?.id ?? user?.userId
  const fallbackById =
    !initialUrl && userId != null && String(userId).trim() !== ''
      ? `/api/users/${encodeURIComponent(String(userId))}/avatar`
      : ''
  const tryUrl = initialUrl || fallbackById

  return (
    <UserAvatarInner
      key={tryUrl || 'placeholder'}
      alt={alt}
      className={className}
      size={size}
      tryUrl={tryUrl}
    />
  )
}
