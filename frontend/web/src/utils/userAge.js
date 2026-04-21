/**
 * Возраст в полных годах на «сегодня» по дате рождения (UTC-календарь).
 * @param {string|Date|undefined|null} dateOfBirth — ISO или объект Date
 * @returns {number|null}
 */
export function computeUserAgeYears(dateOfBirth) {
  if (dateOfBirth == null || dateOfBirth === '') {
    return null
  }
  const d = dateOfBirth instanceof Date ? dateOfBirth : new Date(dateOfBirth)
  if (Number.isNaN(d.getTime())) {
    return null
  }
  const today = new Date()
  let age = today.getFullYear() - d.getFullYear()
  const monthDiff = today.getMonth() - d.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < d.getDate())) {
    age -= 1
  }
  return age
}
