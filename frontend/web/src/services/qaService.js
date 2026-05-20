import { del, get, post, put } from '../api/httpClient.js'

/**
 * @param {object} [params]
 * @param {string} [params.query]
 * @param {number} [params.categoryId]
 * @param {AbortSignal} [params.signal]
 */
export function getQuestions(params = {}) {
  const { query, categoryId, signal } = params
  const sp = new URLSearchParams()
  const trimmedQuery = query != null ? String(query).trim() : ''
  if (trimmedQuery !== '') {
    sp.set('query', trimmedQuery)
  }
  if (categoryId != null && categoryId !== '' && Number.isFinite(Number(categoryId))) {
    sp.set('categoryId', String(Number(categoryId)))
  }
  const qs = sp.toString()
  return get(qs ? `/qa/questions?${qs}` : '/qa/questions', { signal })
}

export function getQuestionWithAnswers(questionId) {
  const id = encodeURIComponent(String(questionId))
  return get(`/qa/questions/${id}`)
}

export function createQuestion(payload, options = {}) {
  return post('/qa/questions', payload, options)
}

export function postAnswer(payload, options = {}) {
  return post('/qa/answers', payload, options)
}

export function updateQuestion(questionId, payload, options = {}) {
  const id = encodeURIComponent(String(questionId))
  return put(`/qa/questions/${id}`, payload, options)
}

export function deleteQuestion(questionId, options = {}) {
  const id = encodeURIComponent(String(questionId))
  return del(`/qa/questions/${id}`, options)
}

export function updateAnswer(answerId, payload, options = {}) {
  const id = encodeURIComponent(String(answerId))
  return put(`/qa/answers/${id}`, payload, options)
}

export function deleteAnswer(answerId, options = {}) {
  const id = encodeURIComponent(String(answerId))
  return del(`/qa/answers/${id}`, options)
}

/**
 * @param {number} categoryId - matches core-service QAControllerImpl: ?categoryId=
 * @deprecated Prefer getQuestions({ categoryId })
 */
export function getQuestionsByCategory(categoryId) {
  const id = encodeURIComponent(String(categoryId))
  return get(`/qa/questions/category?categoryId=${id}`)
}
