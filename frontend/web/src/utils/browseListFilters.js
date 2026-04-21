import { browseFilterToApiCategory } from '../constants/categoryOptions.js'
import { getCategoryRelatedSearchTerms } from '../constants/browseCategorySearchHints.js'

function normalizeSearch(q) {
  return String(q ?? '')
    .trim()
    .toLowerCase()
}

/** Несколько слов в строке поиска — каждое должно встретиться (имитация ключевых слов). */
function searchTokens(q) {
  return normalizeSearch(q)
    .split(/[\s,.;:+]+/)
    .map((t) => t.trim())
    .filter((t) => t.length > 0)
}

function matchesTokenHaystack(haystack, searchQuery) {
  const tokens = searchTokens(searchQuery)
  if (tokens.length === 0) {
    return true
  }
  const h = haystack.toLowerCase()
  return tokens.every((t) => h.includes(t))
}

export function sortByNumericId(items, direction) {
  const copy = [...items]
  const mul = direction === 'asc' ? 1 : -1
  copy.sort((a, b) => mul * ((a.sortId ?? 0) - (b.sortId ?? 0)))
  return copy
}

/**
 * @param {Array<{ sortId?: number, eventStartMs?: number | null }>} items
 */
export function sortActivityCards(items, sortBy) {
  const copy = [...items]
  switch (sortBy) {
    case 'created-asc':
      return sortByNumericId(copy, 'asc')
    case 'event-asc':
      copy.sort((a, b) => {
        const ta = a.eventStartMs
        const tb = b.eventStartMs
        if (ta == null && tb == null) {
          return 0
        }
        if (ta == null) {
          return 1
        }
        if (tb == null) {
          return -1
        }
        return ta - tb
      })
      return copy
    case 'event-desc':
      copy.sort((a, b) => {
        const ta = a.eventStartMs
        const tb = b.eventStartMs
        if (ta == null && tb == null) {
          return 0
        }
        if (ta == null) {
          return 1
        }
        if (tb == null) {
          return -1
        }
        return tb - ta
      })
      return copy
    case 'created-desc':
    default:
      return sortByNumericId(copy, 'desc')
  }
}

/**
 * @param {object} criteria
 * @param {string} criteria.categoryFilter
 * @param {string} criteria.searchQuery
 * @param {string} [criteria.visibilityFilter]
 * @param {string} [criteria.availabilityFilter]
 * @param {string} [criteria.ageCeiling] — 'all' | '0'|'6'|...
 * @param {string} [criteria.organizerCity]
 * @param {string} [criteria.organizerCountry]
 * @param {boolean} [criteria.showActivitiesNotMeetingAgeRating] — если false и задан userAgeYears, скрываем комнаты с рейтингом выше возраста пользователя
 * @param {number|null} [criteria.userAgeYears]
 */
export function filterActivityCardsForBrowse(items, criteria) {
  const {
    categoryFilter,
    searchQuery,
    visibilityFilter = 'all',
    availabilityFilter = 'all',
    ageCeiling = 'all',
    organizerCity = '',
    organizerCountry = '',
    showActivitiesNotMeetingAgeRating = true,
    userAgeYears = null,
  } = criteria

  const apiCat = browseFilterToApiCategory(categoryFilter)
  let list = items
  if (apiCat) {
    list = list.filter((item) => item.categoryKey === apiCat)
  }

  if (visibilityFilter === 'public') {
    list = list.filter((item) => item.isPublic !== false)
  } else if (visibilityFilter === 'private') {
    list = list.filter((item) => item.isPublic === false)
  }

  if (availabilityFilter === 'open') {
    list = list.filter((item) => !item.isFull)
  } else if (availabilityFilter === 'full') {
    list = list.filter((item) => item.isFull)
  }

  if (ageCeiling !== 'all' && ageCeiling !== '' && ageCeiling != null) {
    const maxAge = Number(ageCeiling)
    if (Number.isFinite(maxAge)) {
      list = list.filter((item) => Number(item.ageRating) <= maxAge)
    }
  }

  if (showActivitiesNotMeetingAgeRating === false && userAgeYears != null) {
    const age = Number(userAgeYears)
    if (Number.isFinite(age) && age >= 0) {
      list = list.filter((item) => {
        const rating = Number(item.ageRating)
        if (!Number.isFinite(rating) || rating <= 0) {
          return true
        }
        return age >= rating
      })
    }
  }

  const cityQ = organizerCity.trim().toLowerCase()
  if (cityQ) {
    list = list.filter((item) => String(item.creatorCity || '').toLowerCase().includes(cityQ))
  }
  const countryQ = organizerCountry.trim().toLowerCase()
  if (countryQ) {
    list = list.filter((item) => String(item.creatorCountry || '').toLowerCase().includes(countryQ))
  }

  return list.filter((item) => {
    const related = getCategoryRelatedSearchTerms(item.categoryKey)
    return matchesTokenHaystack(
      [
        item.title,
        item.description,
        item.author,
        item.category,
        item.categoryKey,
        item.creatorCity,
        item.creatorCountry,
        related,
      ]
        .filter(Boolean)
        .join(' '),
      searchQuery,
    )
  })
}

/**
 * @param {object} criteria
 * @param {string} criteria.categoryFilter
 * @param {string} criteria.searchQuery
 */
export function filterQuestionPreviewsForBrowse(items, criteria) {
  const { categoryFilter, searchQuery } = criteria

  const apiCat = browseFilterToApiCategory(categoryFilter)
  let list = items
  if (apiCat) {
    list = list.filter((item) => item.categoryKey === apiCat)
  }

  return list.filter((item) =>
    matchesTokenHaystack(
      [
        item.title,
        item.description,
        item.categoryLabel,
        ...(item.tags || []),
      ]
        .filter(Boolean)
        .join(' '),
      searchQuery,
    ),
  )
}

export function sortQuestionPreviews(items, sortBy) {
  return sortByNumericId([...items], sortBy === 'created-asc' ? 'asc' : 'desc')
}
