/** Фильтр на главной и в Q&A (значения — для будущей связки с API). */
export const BROWSE_CATEGORY_OPTIONS = [
  { value: 'all-categories', label: 'Все категории' },
  { value: 'sport', label: 'Спорт' },
  { value: 'music', label: 'Музыка' },
  { value: 'art', label: 'Искусство' },
  { value: 'entertainment', label: 'Развлечения' },
  { value: 'business', label: 'Бизнес' },
  { value: 'education', label: 'Образование' },
  { value: 'active-recreation', label: 'Активный отдых' },
  { value: 'passive-recreation', label: 'Пассивный отдых' },
  { value: 'mass-event', label: 'Массовое мероприятие' },
  { value: 'others', label: 'Другое' },
]

/** Значение фильтра UI → имя enum в ответах API (комнаты, вопросы). */
const BROWSE_FILTER_TO_API_CATEGORY = {
  sport: 'SPORT',
  music: 'MUSIC',
  art: 'ART',
  entertainment: 'ENTERTAINMENTS',
  business: 'BUSINESS',
  education: 'EDUCATION',
  'active-recreation': 'ACTIVE_RECREATION',
  'passive-recreation': 'PASSIVE_RECREATION',
  'mass-event': 'IS_A_MASS_EVENT',
  others: 'OTHER',
}

/**
 * @param {string} filterValue — value из BROWSE_CATEGORY_OPTIONS
 * @returns {string | null} — ключ enum для сравнения с полем с API, или null = без фильтра
 */
export function browseFilterToApiCategory(filterValue) {
  if (filterValue == null || filterValue === '' || filterValue === 'all-categories') {
    return null
  }
  return BROWSE_FILTER_TO_API_CATEGORY[filterValue] ?? null
}

/** Подписи для значений enum Category в ответах API (как в JSON). */
export const ROOM_CATEGORY_LABELS = {
  SPORT: 'Спорт',
  MUSIC: 'Музыка',
  ART: 'Искусство',
  ENTERTAINMENTS: 'Развлечения',
  BUSINESS: 'Бизнес',
  EDUCATION: 'Образование',
  ACTIVE_RECREATION: 'Активный отдых',
  PASSIVE_RECREATION: 'Пассивный отдых',
  IS_A_MASS_EVENT: 'Массовое мероприятие',
  OTHER: 'Другое',
  NOT_SPECIFIED: 'Не указано',
}

export function getRoomCategoryLabel(category) {
  if (category == null || String(category).trim() === '') {
    return 'Категория не указана'
  }
  const key = String(category).trim()
  return ROOM_CATEGORY_LABELS[key] ?? key
}

/** Имена категорий в API core-service (enum Category). */
export const ROOM_CATEGORY_OPTIONS = [
  { value: 'Sport', label: 'Спорт' },
  { value: 'Music', label: 'Музыка' },
  { value: 'Art', label: 'Искусство' },
  { value: 'Entertainments', label: 'Развлечения' },
  { value: 'Business', label: 'Бизнес' },
  { value: 'Education', label: 'Образование' },
  { value: 'ActiveRecreation', label: 'Активный отдых' },
  { value: 'PassiveRecreation', label: 'Пассивный отдых' },
  { value: 'MassEvent', label: 'Массовое мероприятие' },
  { value: 'Other', label: 'Другое' },
  { value: 'NotSpecified', label: 'Не указано' },
]
