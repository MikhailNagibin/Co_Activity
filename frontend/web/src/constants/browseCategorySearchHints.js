/**
 * Дополнительные «родственные» слова для поиска по категории (ФТ: ключевые слова связанные с категорией).
 * Учитываются только на клиенте при фильтрации списка на главной.
 */
const RELATED_TERMS_BY_CATEGORY = {
  SPORT: ['атлетика', 'футбол', 'баскетбол', 'тренировк', 'зал', 'бег', 'фитнес', 'sport', 'игра', 'команд'],
  MUSIC: ['концерт', 'песн', 'групп', 'инструмент', 'музыкант', 'sound', 'dj'],
  ART: ['рисован', 'живопис', 'галере', 'творчеств', 'скульптур', 'art'],
  ENTERTAINMENTS: ['развлечен', 'шоу', 'кино', 'театр', 'party', 'игр'],
  BUSINESS: ['бизнес', 'стартап', 'проект', 'офис', 'карьер', 'networking'],
  EDUCATION: ['курс', 'лекци', 'учеб', 'школ', 'университет', 'обучен', 'study'],
  ACTIVE_RECREATION: ['поход', 'велосипед', 'лыж', 'гор', 'активн отдых', 'outdoor'],
  PASSIVE_RECREATION: ['релакс', 'спа', 'отдых', 'чил', 'relax'],
  IS_A_MASS_EVENT: ['фестивал', 'митинг', 'конференц', 'массов', 'событ'],
  OTHER: ['прочее', 'misc', 'other'],
  NOT_SPECIFIED: [],
}

/**
 * @param {string | null | undefined} categoryKey — значение enum с API (например SPORT)
 * @returns {string}
 */
export function getCategoryRelatedSearchTerms(categoryKey) {
  const key = String(categoryKey ?? '')
    .trim()
    .toUpperCase()
  const terms = RELATED_TERMS_BY_CATEGORY[key]
  if (!terms || terms.length === 0) {
    return ''
  }
  return terms.join(' ')
}
