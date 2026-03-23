import { get } from '../api/httpClient.js'

export function getQuestions() {
  return get('/qa/questions')
}

export function getQuestionsByCategory(category) {
  const encodedCategory = encodeURIComponent(category)
  return get(`/qa/questions/category?category=${encodedCategory}`)
}
