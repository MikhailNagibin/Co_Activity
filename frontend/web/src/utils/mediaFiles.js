const IMAGE_MIME_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp'])

export function isSupportedImageFile(file) {
  if (!file) {
    return false
  }

  const mimeType = String(file.type ?? '').trim().toLowerCase()
  return IMAGE_MIME_TYPES.has(mimeType)
}

export function getUnsupportedImageFiles(files) {
  return (files ?? []).filter((file) => !isSupportedImageFile(file))
}

export function revokeObjectUrl(url) {
  if (typeof url !== 'string' || !url.startsWith('blob:')) {
    return
  }

  URL.revokeObjectURL(url)
}
