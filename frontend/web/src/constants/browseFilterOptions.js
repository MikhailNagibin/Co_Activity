/** Соответствует ФТ / requirements_for_pages: сортировка по дате создания (на бэкенде нет поля — порядок по id комнаты). */
export const MAIN_ACTIVITY_SORT_OPTIONS = [
  { value: 'created-desc', label: 'По дате создания: сначала новые' },
  { value: 'created-asc', label: 'По дате создания: сначала старые' },
  { value: 'event-asc', label: 'По дате начала события: ближайшие' },
  { value: 'event-desc', label: 'По дате начала события: позже' },
]

export const QA_SORT_OPTIONS = [
  { value: 'created-desc', label: 'По дате создания: сначала новые' },
  { value: 'created-asc', label: 'По дате создания: сначала старые' },
]

export const VISIBILITY_FILTER_OPTIONS = [
  { value: 'all', label: 'Все типы доступа' },
  { value: 'public', label: 'Только публичные' },
  { value: 'private', label: 'Только по заявке' },
]

export const AVAILABILITY_FILTER_OPTIONS = [
  { value: 'all', label: 'Все по местам' },
  { value: 'open', label: 'Есть свободные места' },
  { value: 'full', label: 'Набрано полностью' },
]

/** ageRating в API: оставляем в списке активности, у которых значение ≤ выбранного порога. */
export const AGE_CEILING_FILTER_OPTIONS = [
  { value: 'all', label: 'Любые' },
  { value: '0', label: 'Не выше 0+' },
  { value: '6', label: 'Не выше 6+' },
  { value: '12', label: 'Не выше 12+' },
  { value: '16', label: 'Не выше 16+' },
  { value: '18', label: 'Не выше 18+' },
]
