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
  { value: 'others', label: 'Другое' },
]

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
