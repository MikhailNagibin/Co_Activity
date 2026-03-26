import AppHeader from '../components/AppHeader.jsx'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/httpClient.js'
import { getAccessToken } from '../api/tokenStorage.js'
import { createQuestion } from '../services/qaService.js'

const CATEGORY_OPTIONS = [
  { value: 'Sport', label: 'Спорт' },
  { value: 'Music', label: 'Музыка' },
  { value: 'Art', label: 'Искусство' },
  { value: 'Entertainments', label: 'Развлечения' },
  { value: 'Business', label: 'Бизнес' },
  { value: 'Education', label: 'Образование' },
  { value: 'ActiveRecreation', label: 'Активный отдых' },
  { value: 'PassiveRecreation', label: 'Пассивный отдых' },
  { value: 'MassEvent', label: 'Массовое мероприятие' },
  { value: 'Other', label: 'Другое' },
  { value: 'NotSpecified', label: 'Не указано' },
]

const QUESTION_MAX_LENGTH = 2000

function CreateQuestionPage() {
  const navigate = useNavigate()
  const [category, setCategory] = useState('Education')
  const [question, setQuestion] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const hasToken = Boolean(getAccessToken())

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    if (!getAccessToken()) {
      setErrorMessage('Войдите в аккаунт, чтобы задать вопрос')
      return
    }

    const trimmedQuestion = question.trim()
    if (!trimmedQuestion) {
      setErrorMessage('Введите текст вопроса')
      return
    }

    if (trimmedQuestion.length > QUESTION_MAX_LENGTH) {
      setErrorMessage(`Текст вопроса не должен превышать ${QUESTION_MAX_LENGTH} символов`)
      return
    }

    setIsSubmitting(true)
    try {
      await createQuestion({ category, question: trimmedQuestion })
      navigate('/qa')
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
      } else {
        setErrorMessage('Не удалось отправить вопрос. Попробуйте снова.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <AppHeader activeTab="qa" />
      <section className="main-hero">
        <h2>Создание вопроса</h2>
        <h3 className="gray-elem">Задайте вопрос сообществу и получите ответы</h3>
      </section>

      <main className="create-room-page">
        {!hasToken ? (
          <p className="create-room-hint">
            <Link to="/sign-in">Войдите</Link>, чтобы задавать вопросы, или{' '}
            <Link to="/sign-up">зарегистрируйтесь</Link>.
          </p>
        ) : null}

        <form className="create-room-form" onSubmit={handleSubmit}>
          <div className="create-room-form-row">
            <label htmlFor="question-category">Категория</label>
            <select
              id="question-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              disabled={isSubmitting}
            >
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="create-room-form-row">
            <label htmlFor="question-text">Текст вопроса</label>
            <textarea
              id="question-text"
              rows={8}
              maxLength={QUESTION_MAX_LENGTH}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              disabled={isSubmitting}
              required
            />
            <p className="create-question-counter">
              {question.length} / {QUESTION_MAX_LENGTH}
            </p>
          </div>

          {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}

          <button type="submit" className="create-room-submit" disabled={isSubmitting || !hasToken}>
            {isSubmitting ? 'Отправка...' : 'Задать вопрос'}
          </button>
        </form>
      </main>
    </>
  )
}

export default CreateQuestionPage
