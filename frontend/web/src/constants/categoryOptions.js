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

/**
 * ID категории в БД (порядок INSERT в V1__baseline_schema.sql: Sport…NotSpecified).
 * Для `GET /api/qa/questions?categoryId=`.
 */
const BROWSE_FILTER_TO_QA_CATEGORY_ID = {
  sport: 1,
  music: 2,
  art: 3,
  entertainment: 4,
  business: 5,
  education: 6,
  'active-recreation': 7,
  'passive-recreation': 8,
  'mass-event': 9,
  others: 10,
}

/**
 * @param {string} filterValue — value из BROWSE_CATEGORY_OPTIONS
 * @returns {number | undefined}
 */
export function browseFilterToQaCategoryId(filterValue) {
  if (filterValue == null || filterValue === '' || filterValue === 'all-categories') {
    return undefined
  }
  const id = BROWSE_FILTER_TO_QA_CATEGORY_ID[filterValue]
  return Number.isFinite(id) ? id : undefined
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

const ROOM_CATEGORY_NORMALIZATION_MAP = {
  SPORT: 'SPORT',
  SPORTS: 'SPORT',
  Sport: 'SPORT',
  MUSIC: 'MUSIC',
  Music: 'MUSIC',
  ART: 'ART',
  Art: 'ART',
  ENTERTAINMENTS: 'ENTERTAINMENTS',
  Entertainments: 'ENTERTAINMENTS',
  BUSINESS: 'BUSINESS',
  Business: 'BUSINESS',
  EDUCATION: 'EDUCATION',
  Education: 'EDUCATION',
  ACTIVE_RECREATION: 'ACTIVE_RECREATION',
  ActiveRecreation: 'ACTIVE_RECREATION',
  PASSIVE_RECREATION: 'PASSIVE_RECREATION',
  PassiveRecreation: 'PASSIVE_RECREATION',
  IS_A_MASS_EVENT: 'IS_A_MASS_EVENT',
  MassEvent: 'IS_A_MASS_EVENT',
  OTHER: 'OTHER',
  Other: 'OTHER',
  NOT_SPECIFIED: 'NOT_SPECIFIED',
  NotSpecified: 'NOT_SPECIFIED',
}

export function normalizeRoomCategory(category) {
  if (category == null || String(category).trim() === '') {
    return 'NOT_SPECIFIED'
  }
  return ROOM_CATEGORY_NORMALIZATION_MAP[String(category).trim()] ?? String(category).trim()
}

/** Имена категорий в API core-service (enum Category). */
export const ROOM_CATEGORY_OPTIONS = [
  { value: 'SPORT', label: 'Спорт' },
  { value: 'MUSIC', label: 'Музыка' },
  { value: 'ART', label: 'Искусство' },
  { value: 'ENTERTAINMENTS', label: 'Развлечения' },
  { value: 'BUSINESS', label: 'Бизнес' },
  { value: 'EDUCATION', label: 'Образование' },
  { value: 'ACTIVE_RECREATION', label: 'Активный отдых' },
  { value: 'PASSIVE_RECREATION', label: 'Пассивный отдых' },
  { value: 'IS_A_MASS_EVENT', label: 'Массовое мероприятие' },
  { value: 'OTHER', label: 'Другое' },
  { value: 'NOT_SPECIFIED', label: 'Не указано' },
]
