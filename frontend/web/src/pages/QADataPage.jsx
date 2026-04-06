import AppHeader from '../components/AppHeader.jsx'
import QuestionPreview from '../components/QuestionPreview.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { QA_SORT_OPTIONS } from '../constants/browseFilterOptions.js'
import {
  filterQuestionPreviewsForBrowse,
  sortQuestionPreviews,
} from '../utils/browseListFilters.js'
import { useEffect, useMemo, useState } from 'react'
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

function QADataPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const [sortBy, setSortBy] = useState('created-desc')
  useEffect(() => {
    let isMounted = true

    const loadQuestions = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const payload = await getQuestions()
        if (!isMounted) {
          return
        }
        setQuestions(mapQuestionsToPreview(payload))
      } catch (error) {
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
    }
  }, [navigate])

  const keywordTags = useMemo(
    () =>
      Array.from(
        new Set(questions.flatMap((question) => question.tags).filter((tag) => tag.trim() !== '')),
      ).slice(0, 18),
    [questions],
  )

  const filteredQuestions = useMemo(() => {
    const filtered = filterQuestionPreviewsForBrowse(questions, {
      categoryFilter,
      searchQuery,
    })
    return sortQuestionPreviews(filtered, sortBy)
  }, [questions, categoryFilter, searchQuery, sortBy])

  return (
    <>
      <AppHeader activeTab="qa" />
      <div className="main-page-shell">
        <section className="main-hero qa-hero">
          <h2>Вопросы и ответы</h2>
          <h3>Задавайте вопросы и делитесь опытом с сообществом</h3>
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
            {!isLoading && !errorMessage && questions.length === 0 ? (
              <p className="qa-list-message">Пока нет вопросов</p>
            ) : null}
            {!isLoading && !errorMessage && questions.length > 0 && filteredQuestions.length === 0 ? (
              <p className="qa-list-message">
                Ничего не найдено — измените поиск, категорию или сортировку
              </p>
            ) : null}
            {!isLoading && !errorMessage
              ? filteredQuestions.map((item, index) => (
                  <QuestionPreview key={item.id ?? `${item.title}-${index}`} item={item} />
                ))
              : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default QADataPage
