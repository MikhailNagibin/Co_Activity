import { get } from '../api/httpClient.js'

export function getQuestions() {
  return get('/qa/questions')
}

/**
 * @param {number} categoryId - matches core-service QAControllerImpl: ?categoryId=
 */
export function getQuestionsByCategory(categoryId) {
  const id = encodeURIComponent(String(categoryId))
  return get(`/qa/questions/category?categoryId=${id}`)
}
