import AppHeader from '../components/AppHeader.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { ROOM_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/httpClient.js'
import { getAccessToken } from '../api/tokenStorage.js'
import { createQuestion } from '../services/qaService.js'

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
      <div className="main-page-shell">
        <section className="main-hero">
          <h2>Новый вопрос</h2>
          <h3>Задайте вопрос сообществу и получите ответы</h3>
        </section>

        <main className="main-page-content qa-create-main">
          {!hasToken ? (
            <p className="create-room-hint">
              <Link to="/sign-in">Войдите</Link>, чтобы задавать вопросы, или{' '}
              <Link to="/sign-up">зарегистрируйтесь</Link>.
            </p>
          ) : null}

          <form className="create-room-form qa-create-form" onSubmit={handleSubmit}>
          <div className="create-room-form-row">
            <label htmlFor="question-category">Категория</label>
            <StyledDropdown
              variant="form"
              id="question-category"
              ariaLabel="Категория вопроса"
              options={ROOM_CATEGORY_OPTIONS}
              value={category}
              onChange={setCategory}
              disabled={isSubmitting}
            />
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
      </div>
    </>
  )
}

export default CreateQuestionPage
