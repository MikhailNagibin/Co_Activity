export function resolveUserName(user, fallback = 'Не указано') {
  const userName = user?.userName ?? user?.username
  if (typeof userName === 'string' && userName.trim() !== '') {
    return userName
  }
  return fallback
}

export function resolveUserAvatarUrl(user) {
  const avatarUrl = user?.avatarUrl
  if (typeof avatarUrl === 'string' && avatarUrl.trim() !== '') {
    return avatarUrl
  }

  const userId = user?.id ?? user?.userId
  if (userId == null || user?.avatarId == null) {
    return ''
  }

  return `/api/users/${encodeURIComponent(String(userId))}/avatar`
}

export function withCacheBust(url, version) {
  if (!url) {
    return ''
  }

  const cacheKey = Number.isFinite(version) ? `v=${version}` : `v=${Date.now()}`
  return `${url}${url.includes('?') ? '&' : '?'}${cacheKey}`
}
