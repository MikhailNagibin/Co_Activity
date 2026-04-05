import { Link, useNavigate, useParams } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import AppHeader from '../components/AppHeader.jsx'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import { getAccessToken } from '../api/tokenStorage.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getQuestionWithAnswers, postAnswer } from '../services/qaService.js'
import { formatDateTimeRu, splitQuestionTitleBody } from '../services/uiMappers.js'

const ANSWER_MAX_LENGTH = 2000

function pickFirst(...candidates) {
  for (const candidate of candidates) {
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return candidate
    }
  }
  return null
}

function answersWord(n) {
  const x = Math.abs(Number(n)) % 100
  const x1 = x % 10
  if (x > 10 && x < 20) {
    return 'ответов'
  }
  if (x1 > 1 && x1 < 5) {
    return 'ответа'
  }
  if (x1 === 1) {
    return 'ответ'
  }
  return 'ответов'
}

function QuestionThreadPage() {
  const navigate = useNavigate()
  const { questionId: questionIdParam } = useParams()
  const questionId = Number(questionIdParam)
  const [payload, setPayload] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [postError, setPostError] = useState('')
  const [answerText, setAnswerText] = useState('')
  const [isPostingAnswer, setIsPostingAnswer] = useState(false)

  const hasToken = Boolean(getAccessToken())

  const loadThread = useCallback(async () => {
    setIsLoading(true)
    setLoadError('')
    try {
      const data = await getQuestionWithAnswers(questionId)
      setPayload(data)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setLoadError(getUserFacingApiMessage(error, 'Не удалось загрузить вопрос'))
      } else {
        setLoadError('Не удалось загрузить вопрос')
      }
      setPayload(null)
    } finally {
      setIsLoading(false)
    }
  }, [questionId, navigate])

  useEffect(() => {
    if (!Number.isFinite(questionId) || questionId <= 0) {
      setLoadError('Некорректная ссылка на вопрос')
      setIsLoading(false)
      return
    }
    loadThread()
  }, [questionId, loadThread])

  const questionBlock = payload?.question
  const rawQuestionText = pickFirst(questionBlock?.question, '')
  const { title: displayTitle, body: displayBody } = splitQuestionTitleBody(String(rawQuestionText))
  const categoryTag =
    questionBlock?.category != null && String(questionBlock.category).trim() !== ''
      ? [String(questionBlock.category)]
      : []
  const questionAuthorName = String(
    pickFirst(questionBlock?.author?.userName, 'Неизвестный автор'),
  )
  const questionCreatedRaw = pickFirst(questionBlock?.createdAt, '')
  const questionCreatedLabel = formatDateTimeRu(questionCreatedRaw)

  const answers = Array.isArray(payload?.answers) ? payload.answers : []

  const handlePostAnswer = async (event) => {
    event.preventDefault()
    setPostError('')
    if (!hasToken) {
      return
    }
    const trimmed = answerText.trim()
    if (!trimmed) {
      setPostError('Введите текст ответа')
      return
    }
    if (trimmed.length > ANSWER_MAX_LENGTH) {
      setPostError(`Ответ не длиннее ${ANSWER_MAX_LENGTH} символов`)
      return
    }
    setIsPostingAnswer(true)
    try {
      await postAnswer({
        questionId,
        answer: trimmed,
        previousAnswerId: null,
      })
      setAnswerText('')
      await loadThread()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setPostError(getUserFacingApiMessage(error, 'Не удалось отправить ответ'))
      } else {
        setPostError('Не удалось отправить ответ')
      }
    } finally {
      setIsPostingAnswer(false)
    }
  }

  if (!Number.isFinite(questionId) || questionId <= 0) {
    return (
      <>
        <AppHeader activeTab="qa" />
        <div className="main-page-shell qa-thread-shell qa-thread-shell--legacy">
          <p className="qa-list-message qa-list-message--error" role="alert">
            {loadError || 'Некорректная ссылка на вопрос'}
          </p>
          <Link className="back-link qa-thread-back" to="/qa">
            ← Назад к вопросам
          </Link>
        </div>
      </>
    )
  }

  return (
    <>
      <AppHeader activeTab="qa" />
      <div className="main-page-shell qa-thread-shell qa-thread-shell--legacy">
        <Link className="back-link qa-thread-back" to="/qa">
          ← Назад к вопросам
        </Link>

        {isLoading ? <p className="qa-list-message">Загрузка...</p> : null}

        {!isLoading && loadError && !questionBlock ? (
          <p className="qa-list-message qa-list-message--error" role="alert">
            {loadError}
          </p>
        ) : null}

        {!isLoading && questionBlock ? (
          <>
            <article className="qa-thread-op">
              <aside className="qa-thread-op-sidebar" aria-label="Автор вопроса">
                <i
                  className="fa-regular fa-circle-user qa-thread-avatar qa-thread-avatar--lg"
                  aria-hidden="true"
                ></i>
                <h3 className="qa-thread-author-name">{questionAuthorName}</h3>
                {questionCreatedLabel ? (
                  <time className="qa-thread-date" dateTime={String(questionCreatedRaw)}>
                    {questionCreatedLabel}
                  </time>
                ) : null}
              </aside>

              <div className="qa-thread-op-main">
                <h1 className="qa-thread-title">{displayTitle}</h1>
                <p className="qa-thread-body">{displayBody}</p>
                {categoryTag.length > 0 ? (
                  <div className="qa-thread-op-tags">
                    {categoryTag.map((tag) => (
                      <span key={tag} className="qa-tag">
                        {tag}
                      </span>
                    ))}
                  </div>
                ) : null}
              </div>
            </article>

            <section className="qa-thread-answers" aria-label="Ответы">
              <h2 className="qa-thread-answers-heading">
                {answers.length} {answersWord(answers.length)}
              </h2>

              {answers.map((answer) => {
                const authorName = String(pickFirst(answer.author?.userName, 'Участник'))
                const when = formatDateTimeRu(answer.createdAt)

                return (
                  <article className="qa-answer-card" key={answer.id}>
                    <aside className="qa-answer-sidebar" aria-label="Автор ответа">
                      <i className="fa-regular fa-circle-user qa-thread-avatar" aria-hidden="true"></i>
                      <h4 className="qa-answer-author">{authorName}</h4>
                      {when ? <time className="qa-thread-date">{when}</time> : null}
                    </aside>
                    <div className="qa-answer-body">
                      <p>{answer.answer}</p>
                    </div>
                  </article>
                )
              })}
            </section>

            {hasToken ? (
              <section className="qa-thread-reply" aria-label="Ваш ответ">
                <h2 className="qa-thread-answers-heading">Добавить ответ</h2>
                <form className="qa-thread-reply-form" onSubmit={handlePostAnswer}>
                  <textarea
                    className="qa-thread-reply-textarea"
                    rows={5}
                    maxLength={ANSWER_MAX_LENGTH}
                    value={answerText}
                    onChange={(e) => setAnswerText(e.target.value)}
                    disabled={isPostingAnswer}
                    placeholder="Напишите ответ сообществу..."
                    aria-label="Текст ответа"
                  />
                  <div className="qa-thread-reply-footer">
                    <span className="qa-thread-reply-counter">
                      {answerText.length} / {ANSWER_MAX_LENGTH}
                    </span>
                    <button
                      type="submit"
                      className="main-create-activity-btn qa-thread-reply-submit"
                      disabled={isPostingAnswer}
                    >
                      {isPostingAnswer ? 'Отправка...' : 'Отправить ответ'}
                    </button>
                  </div>
                </form>
                {postError ? (
                  <p className="qa-list-message qa-list-message--error" role="alert">
                    {postError}
                  </p>
                ) : null}
              </section>
            ) : (
              <div className="qa-thread-cta">
                <Link className="main-create-activity-btn qa-thread-cta-link" to="/sign-in">
                  Войти, чтобы ответить на вопрос
                </Link>
              </div>
            )}
          </>
        ) : null}
      </div>
    </>
  )
}

export default QuestionThreadPage
