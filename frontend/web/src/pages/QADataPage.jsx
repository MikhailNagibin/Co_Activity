import AppHeader from '../components/AppHeader.jsx'
import QuestionPreview from '../components/QuestionPreview.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { BROWSE_CATEGORY_OPTIONS, browseFilterToQaCategoryId } from '../constants/categoryOptions.js'
import { QA_SORT_OPTIONS } from '../constants/browseFilterOptions.js'
import { sortQuestionPreviews } from '../utils/browseListFilters.js'
import { useDeferredValue, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { useAuthSession } from '../auth/authSessionContext.js'
import { getQuestions } from '../services/qaService.js'
import { mapQuestionsToPreview } from '../services/uiMappers.js'

const SEARCH_DEBOUNCE_MS = 320

function QADataPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [sortBy, setSortBy] = useState('created-desc')

  const deferredSearch = useDeferredValue(debouncedSearchQuery)

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim())
    }, SEARCH_DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [searchQuery])

  useEffect(() => {
    let isMounted = true
    const controller = new AbortController()
    const categoryId = browseFilterToQaCategoryId(categoryFilter)
    const queryForApi = deferredSearch.trim() === '' ? undefined : deferredSearch.trim()

    const loadQuestions = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const payload = await getQuestions({
          query: queryForApi,
          categoryId,
          signal: controller.signal,
        })
        if (!isMounted) {
          return
        }
        setQuestions(mapQuestionsToPreview(payload))
      } catch (error) {
        if (error?.name === 'AbortError') {
          return
        }
        if (!isMounted) {
          return
        }
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, { next: '/qa' })
          return
        }
        if (isApiError(error)) {
          setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить вопросы'))
        } else {
          setErrorMessage('Не удалось загрузить вопросы')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    loadQuestions()

    return () => {
      isMounted = false
      controller.abort()
    }
  }, [navigate, categoryFilter, deferredSearch])

  const keywordTags = useMemo(
    () =>
      Array.from(
        new Set(questions.flatMap((question) => question.tags).filter((tag) => tag.trim() !== '')),
      ).slice(0, 18),
    [questions],
  )

  const sortedQuestions = useMemo(
    () => sortQuestionPreviews(questions, sortBy),
    [questions, sortBy],
  )

  const hasActiveRefinement =
    categoryFilter !== 'all-categories' ||
    (deferredSearch != null && String(deferredSearch).trim() !== '')

  return (
    <>
      <AppHeader activeTab="qa" />
      <div className="main-page-shell">
        <section className="main-hero qa-hero">
          <h2>Вопросы и ответы</h2>
          <h3>
            {isAuthenticated
              ? 'Задавайте вопросы и делитесь опытом с сообществом'
              : 'Читайте обсуждения сообщества. Чтобы задать вопрос или ответить, войдите в аккаунт.'}
          </h3>
        </section>

        <main className="main-page-content">
          <div className="main-toolbar qa-toolbar">
            <div className="search-wrapper">
              <button className="search-button" type="button" aria-label="Поиск">
                <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
              </button>
              <input
                placeholder="Текст вопроса, тема, несколько слов через пробел…"
                type="search"
                name="qa-q"
                autoComplete="off"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                aria-label="Поиск по вопросам и ключевым словам"
              />
            </div>

            <StyledDropdown
              variant="toolbar"
              id="qa-category-filter"
              ariaLabel="Категория"
              options={BROWSE_CATEGORY_OPTIONS}
              value={categoryFilter}
              onChange={setCategoryFilter}
            />

            <StyledDropdown
              variant="toolbar"
              className="styled-dropdown--sort-browse"
              id="qa-sort"
              ariaLabel="Сортировка вопросов"
              options={QA_SORT_OPTIONS}
              value={sortBy}
              onChange={setSortBy}
            />

            {isAuthenticated ? (
              <Link className="main-create-activity-btn qa-ask-btn" to="/qa/new">
                Задать вопрос
              </Link>
            ) : null}
          </div>

          {keywordTags.length > 0 ? (
            <section className="qa-keywords-panel" aria-labelledby="qa-keywords-heading">
              <h2 id="qa-keywords-heading" className="qa-keywords-heading">
                Популярные темы
              </h2>
              <div className="qa-keywords-chips" role="list">
                {keywordTags.map((tag) => (
                  <button
                    key={tag}
                    type="button"
                    className="qa-keyword-chip"
                    onClick={() => setSearchQuery(tag)}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          <section className="qa-questions-grid" aria-label="Список вопросов">
            {isLoading ? <p className="qa-list-message">Загрузка вопросов...</p> : null}
            {!isLoading && errorMessage ? (
              <p className="qa-list-message qa-list-message--error" role="alert">
                {errorMessage}
              </p>
            ) : null}
            {!isLoading && !errorMessage && questions.length === 0 && !hasActiveRefinement ? (
              <p className="qa-list-message">Пока нет вопросов</p>
            ) : null}
            {!isLoading && !errorMessage && questions.length === 0 && hasActiveRefinement ? (
              <p className="qa-list-message">
                Ничего не найдено — измените поиск, категорию или сортировку
              </p>
            ) : null}
            {!isLoading && !errorMessage
              ? sortedQuestions.map((item, index) => (
                  <QuestionPreview
                    key={item.id ?? `${item.title}-${index}`}
                    item={item}
                    canViewProfiles={isAuthenticated}
                  />
                ))
              : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default QADataPage
