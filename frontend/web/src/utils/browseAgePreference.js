/** Локальная настройка (без бэкенда): показывать активности, где возраст ниже рейтинга комнаты. */
const STORAGE_KEY = 'coactivity.showActivitiesNotMeetingAgeRating'

export const BROWSE_AGE_PREFERENCE_CHANGED = 'coactivity-browse-age-pref-changed'

export function getShowActivitiesNotMeetingAgeRatingFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true' || raw === '1'
  } catch {
    return true
  }
}

export function setShowActivitiesNotMeetingAgeRatingInStorage(value) {
  try {
    localStorage.setItem(STORAGE_KEY, value ? 'true' : 'false')
  } catch {
    // ignore quota / private mode
  }
  try {
    window.dispatchEvent(new CustomEvent(BROWSE_AGE_PREFERENCE_CHANGED))
  } catch {
    // ignore
  }
}
