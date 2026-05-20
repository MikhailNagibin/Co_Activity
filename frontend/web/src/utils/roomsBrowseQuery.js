import { browseFilterToApiCategory } from '../constants/categoryOptions.js'

/**
 * Собирает query для `GET /api/rooms` (RoomFilter + sortBy в core-service).
 *
 * Ограничения относительно UI «Обзор» на главной:
 * - `query` на сервере уже поддерживается, но MainPage пока не отправляет его:
 *   UI-поиск шире серверного и ищет не только по имени/описанию, но и по автору/ключевым словам/локации организатора.
 *   Чтобы не терять релевантные карточки, текстовый поиск на главной остаётся клиентским.
 * - `city` / `country` в API — локация комнаты (поля комнаты), а не город/страна организатора из профиля; эти поля фильтра на MainPage остаются только клиентскими.
 * - Нет серверных аналогов фильтров «места» (свободно/полностью) и возрастного потолка — только на клиенте.
 * - Сортировки `created-asc`, `event-desc` на бэкенде не задаются отдельным enum; список приходит с сортировкой по умолчанию, порядок добивается в {@link sortActivityCards}.
 *
 * @param {object} input
 * @param {string} input.categoryFilter
 * @param {string} input.searchQuery
 * @param {string} input.visibilityFilter — 'all' | 'public' | 'private'
 * @param {string} input.sortBy — значения MAIN_ACTIVITY_SORT_OPTIONS
 * @returns {Record<string, string>}
 */
export function buildRoomsListQueryParams({ categoryFilter, searchQuery, visibilityFilter, sortBy }) {
  /** @type {Record<string, string>} */
  const params = {}

  const apiCategory = browseFilterToApiCategory(categoryFilter)
  if (apiCategory) {
    params.category = apiCategory
  }

  void searchQuery

  if (visibilityFilter === 'public') {
    params.isPublic = 'true'
  } else if (visibilityFilter === 'private') {
    params.isPublic = 'false'
  }

  const serverSort = mapUiSortToRoomSortParam(sortBy)
  if (serverSort) {
    params.sortBy = serverSort
  }

  return params
}

/**
 * @param {string | undefined} sortBy
 * @returns {string | null}
 */
function mapUiSortToRoomSortParam(sortBy) {
  switch (sortBy) {
    case 'created-desc':
      return 'NEWEST'
    case 'event-asc':
      return 'UPCOMING'
    default:
      return null
  }
}
