import AppHeader from '../components/AppHeader.jsx'
import QuestionPreview from '../components/QuestionPreview.jsx'
import { useEffect, useState } from 'react'
import { ApiError } from '../api/httpClient.js'
import { getQuestions } from '../services/qaService.js'
import { mapQuestionsToPreview } from '../services/uiMappers.js'

function QADataPage() {
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

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

  const keywordTags = Array.from(
    new Set(questions.flatMap((question) => question.tags).filter((tag) => tag.trim() !== '')),
  ).slice(0, 15)

  return (
    <>
      <AppHeader activeTab="qa" />
      <section className="main-hero">
        <h2>Форум для самых любознательных</h2>
        <h3 className="gray-elem">
          Задавайте вопросы и делитесь своими знаниями и опытом с сообществом
        </h3>
      </section>

      <main className="main-page-content qa-page-content">
        <div className="search-wrapper">
          <button className="search-button" type="button" aria-label="Поиск">
            <i className="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
          </button>
          <input placeholder="Поиск активностей..." type="text" />
        </div>

        <select name="categories" defaultValue="all-categories">
          <option value="all-categories">Все категории</option>
          <option value="sport">Спорт</option>
          <option value="music">Музыка</option>
          <option value="art">Искусство</option>
          <option value="entertainment">Развлечения</option>
          <option value="business">Бизнес</option>
          <option value="education">Образование</option>
          <option value="active-recreation">Активный отдых</option>
          <option value="passive-recreation">Пассивный отдых</option>
          <option value="others">Другое</option>
        </select>
        <button type="button">Фильтры</button>

        <div className="keywords-row">
          <h2>Ключевые слова:</h2>
          <section className="tags">
            {keywordTags.length === 0 ? (
              <em>Нет данных по ключевым словам</em>
            ) : (
              keywordTags.map((tag) => (
                <button key={tag} type="button">
                  {tag}
                </button>
              ))
            )}
          </section>
        </div>

        <section className="questions">
          {isLoading ? <p>Загрузка вопросов...</p> : null}
          {!isLoading && errorMessage ? <p style={{ color: '#b00020' }}>{errorMessage}</p> : null}
          {!isLoading && !errorMessage && questions.length === 0 ? <p>Пока нет вопросов</p> : null}
          {!isLoading && !errorMessage
            ? questions.map((item, index) => (
                <QuestionPreview key={item.id ?? `${item.title}-${index}`} item={item} />
              ))
            : null}
        </section>
      </main>
    </>
  )
}

export default QADataPage
