import AppHeader from '../components/AppHeader.jsx'
import QuestionPreview from '../components/QuestionPreview.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { BROWSE_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/httpClient.js'
import { getAccessToken } from '../api/tokenStorage.js'
import { getQuestions } from '../services/qaService.js'
import { mapQuestionsToPreview } from '../services/uiMappers.js'

function QADataPage() {
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('all-categories')
  const hasToken = Boolean(getAccessToken())

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
        if (error instanceof ApiError) {
          setErrorMessage(error.message)
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
  }, [])

  const keywordTags = useMemo(
    () =>
      Array.from(
        new Set(questions.flatMap((question) => question.tags).filter((tag) => tag.trim() !== '')),
      ).slice(0, 18),
    [questions],
  )

  const filteredQuestions = useMemo(() => {
    const q = searchQuery.trim().toLowerCase()
    if (!q) {
      return questions
    }
    return questions.filter(
      (item) =>
        String(item.title).toLowerCase().includes(q) ||
        String(item.description).toLowerCase().includes(q) ||
        item.tags.some((tag) => String(tag).toLowerCase().includes(q)),
    )
  }, [questions, searchQuery])

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
                placeholder="Поиск по вопросам и темам..."
                type="search"
                name="qa-q"
                autoComplete="off"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                aria-label="Поиск по вопросам"
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

            <button type="button" className="main-filters-btn">
              Фильтры
            </button>

            {hasToken ? (
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
              <p className="qa-list-message">Ничего не найдено — попробуйте другой запрос</p>
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
