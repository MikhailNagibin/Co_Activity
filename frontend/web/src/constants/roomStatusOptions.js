export const ROOM_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Активна' },
  { value: 'INACTIVE', label: 'Неактивна' },
  { value: 'COMPLETED', label: 'Завершена' },
]

const ROOM_STATUS_LABELS = {
  ACTIVE: 'Активна',
  INACTIVE: 'Неактивна',
  COMPLETED: 'Завершена',
}

export function getRoomStatusLabel(status) {
  if (status == null || String(status).trim() === '') {
    return 'Статус не указан'
  }

  const normalized = String(status).trim().toUpperCase()
  return ROOM_STATUS_LABELS[normalized] ?? normalized
}
