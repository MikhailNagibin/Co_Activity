import AppHeader from '../components/AppHeader.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { ROOM_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { createQuestion } from '../services/qaService.js'

const QUESTION_MAX_LENGTH = 2000
const QUESTION_TITLE_MAX_LENGTH = 120
const QUESTION_BODY_MAX_LENGTH = 1800

function CreateQuestionPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthSession()
  const [category, setCategory] = useState('EDUCATION')
  const [questionTitle, setQuestionTitle] = useState('')
  const [question, setQuestion] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    if (!isAuthenticated) {
      setErrorMessage('Войдите в аккаунт, чтобы задать вопрос')
      return
    }

    const trimmedTitle = questionTitle.trim()
    const trimmedQuestion = question.trim()
    if (!trimmedTitle) {
      setErrorMessage('Введите заголовок вопроса')
      return
    }

    if (trimmedTitle.length > QUESTION_TITLE_MAX_LENGTH) {
      setErrorMessage(`Заголовок не должен превышать ${QUESTION_TITLE_MAX_LENGTH} символов`)
      return
    }

    if (!trimmedQuestion) {
      setErrorMessage('Опишите вопрос подробнее')
      return
    }

    if (trimmedQuestion.length > QUESTION_BODY_MAX_LENGTH) {
      setErrorMessage(`Подробное описание не должно превышать ${QUESTION_BODY_MAX_LENGTH} символов`)
      return
    }

    const questionPayload = `${trimmedTitle}\n\n${trimmedQuestion}`
    if (questionPayload.length > QUESTION_MAX_LENGTH) {
      setErrorMessage(`Вопрос целиком не должен превышать ${QUESTION_MAX_LENGTH} символов`)
      return
    }

    setIsSubmitting(true)
    try {
      await createQuestion({ category, question: questionPayload })
      navigate('/qa')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/qa/new' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось отправить вопрос. Попробуйте снова.'))
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
          {!isAuthenticated ? (
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
            <label htmlFor="question-title">Заголовок вопроса</label>
            <input
              id="question-title"
              type="text"
              maxLength={QUESTION_TITLE_MAX_LENGTH}
              value={questionTitle}
              onChange={(event) => setQuestionTitle(event.target.value)}
              disabled={isSubmitting}
              placeholder="Коротко сформулируйте суть вопроса"
              required
            />
            <p className="create-question-counter">
              {questionTitle.length} / {QUESTION_TITLE_MAX_LENGTH}
            </p>
          </div>

          <div className="create-room-form-row">
            <label htmlFor="question-text">Подробное описание</label>
            <textarea
              id="question-text"
              rows={8}
              maxLength={QUESTION_BODY_MAX_LENGTH}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              disabled={isSubmitting}
              placeholder="Добавьте контекст, что уже пробовали и какого ответа ждёте"
              required
            />
            <p className="create-question-counter">
              {question.length} / {QUESTION_BODY_MAX_LENGTH}
            </p>
          </div>

          {errorMessage ? <p className="create-room-error">{errorMessage}</p> : null}

          <button type="submit" className="create-room-submit" disabled={isSubmitting || !isAuthenticated}>
            {isSubmitting ? 'Отправка...' : 'Задать вопрос'}
          </button>
          </form>
        </main>
      </div>
    </>
  )
}

export default CreateQuestionPage
