import { Link, useNavigate, useParams } from 'react-router-dom'
import { useCallback, useEffect, useMemo, useState } from 'react'
import AppHeader from '../components/AppHeader.jsx'
import StyledDropdown from '../components/StyledDropdown.jsx'
import { isApiError } from '../api/httpClient.js'
import { useAuthSession } from '../auth/authSessionContext.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  deleteAnswer,
  deleteQuestion,
  getQuestionWithAnswers,
  postAnswer,
  updateAnswer,
  updateQuestion,
} from '../services/qaService.js'
import { formatDateTimeRu, splitQuestionTitleBody } from '../services/uiMappers.js'
import { ROOM_CATEGORY_OPTIONS, normalizeRoomCategory } from '../constants/categoryOptions.js'

const ANSWER_MAX_LENGTH = 2000
const QUESTION_MAX_LENGTH = 2000

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

const allowedCategoryValues = new Set(ROOM_CATEGORY_OPTIONS.map((o) => o.value))

function categoryValueForRequest(rawCategory) {
  const normalized = normalizeRoomCategory(rawCategory ?? '')
  return allowedCategoryValues.has(normalized) ? normalized : 'NOT_SPECIFIED'
}

function parseUserId(user) {
  const raw = pickFirst(user?.id, user?.userId, null)
  if (raw === null || raw === undefined || String(raw).trim() === '') {
    return null
  }
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

function parseAuthorId(author) {
  const raw = author?.id
  if (raw === null || raw === undefined || String(raw).trim() === '') {
    return null
  }
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

function isSameAuthor(author, currentUserId) {
  if (currentUserId == null) {
    return false
  }
  const aid = parseAuthorId(author)
  if (aid == null) {
    return false
  }
  return aid === currentUserId
}

function flattenAnswers(nodes) {
  const out = []
  const walk = (arr) => {
    if (!Array.isArray(arr)) {
      return
    }
    for (const item of arr) {
      if (item && item.id != null) {
        out.push(item)
      }
      walk(item?.replies)
    }
  }
  walk(nodes)
  return out
}

function QuestionThreadPage() {
  const navigate = useNavigate()
  const { isAuthenticated, currentUser } = useAuthSession()
  const { questionId: questionIdParam } = useParams()
  const questionId = Number(questionIdParam)
  const [payload, setPayload] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [postError, setPostError] = useState('')
  const [threadActionError, setThreadActionError] = useState('')
  const [answerText, setAnswerText] = useState('')
  const [isPostingAnswer, setIsPostingAnswer] = useState(false)

  const [isEditingQuestion, setIsEditingQuestion] = useState(false)
  const [editQuestionText, setEditQuestionText] = useState('')
  const [editQuestionCategory, setEditQuestionCategory] = useState('NOT_SPECIFIED')
  const [isSavingQuestion, setIsSavingQuestion] = useState(false)

  const [editingAnswerId, setEditingAnswerId] = useState(null)
  const [editAnswerText, setEditAnswerText] = useState('')
  const [isSavingAnswer, setIsSavingAnswer] = useState(false)

  const [confirmDialog, setConfirmDialog] = useState(null)
  const [isDestructivePending, setIsDestructivePending] = useState(false)

  const currentUserId = useMemo(() => parseUserId(currentUser), [currentUser])

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

  const answers = useMemo(
    () => flattenAnswers(Array.isArray(payload?.answers) ? payload.answers : []),
    [payload?.answers],
  )

  const isQuestionOwner =
    isAuthenticated && isSameAuthor(questionBlock?.author, currentUserId)

  const handleStartEditQuestion = () => {
    if (!isQuestionOwner) {
      return
    }
    setThreadActionError('')
    setEditQuestionText(String(rawQuestionText ?? ''))
    setEditQuestionCategory(categoryValueForRequest(questionBlock?.category))
    setIsEditingQuestion(true)
  }

  const handleCancelEditQuestion = () => {
    setIsEditingQuestion(false)
    setThreadActionError('')
  }

  const handleSaveQuestion = async () => {
    if (!isQuestionOwner) {
      return
    }
    setThreadActionError('')
    const trimmed = editQuestionText.trim()
    if (!trimmed) {
      setThreadActionError('Введите текст вопроса')
      return
    }
    if (trimmed.length > QUESTION_MAX_LENGTH) {
      setThreadActionError(`Текст вопроса не длиннее ${QUESTION_MAX_LENGTH} символов`)
      return
    }
    setIsSavingQuestion(true)
    try {
      await updateQuestion(questionId, {
        category: editQuestionCategory,
        question: trimmed,
      })
      setIsEditingQuestion(false)
      await loadThread()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setThreadActionError(getUserFacingApiMessage(error, 'Не удалось сохранить вопрос'))
      } else {
        setThreadActionError('Не удалось сохранить вопрос')
      }
    } finally {
      setIsSavingQuestion(false)
    }
  }

  const handleRequestDeleteQuestion = () => {
    if (!isQuestionOwner) {
      return
    }
    setThreadActionError('')
    setConfirmDialog({ type: 'question' })
  }

  const handleConfirmDeleteQuestion = async () => {
    if (!isQuestionOwner) {
      setConfirmDialog(null)
      return
    }
    setIsDestructivePending(true)
    setThreadActionError('')
    try {
      await deleteQuestion(questionId)
      setConfirmDialog(null)
      navigate('/qa', { replace: true })
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setThreadActionError(getUserFacingApiMessage(error, 'Не удалось удалить вопрос'))
      } else {
        setThreadActionError('Не удалось удалить вопрос')
      }
    } finally {
      setIsDestructivePending(false)
      setConfirmDialog(null)
    }
  }

  const handleStartEditAnswer = (answer) => {
    if (!isAuthenticated || !isSameAuthor(answer?.author, currentUserId)) {
      return
    }
    setThreadActionError('')
    setEditingAnswerId(Number(answer.id))
    setEditAnswerText(String(answer.answer ?? ''))
  }

  const handleCancelEditAnswer = () => {
    setEditingAnswerId(null)
    setEditAnswerText('')
    setThreadActionError('')
  }

  const handleSaveAnswer = async () => {
    if (editingAnswerId == null) {
      return
    }
    setThreadActionError('')
    const trimmed = editAnswerText.trim()
    if (!trimmed) {
      setThreadActionError('Введите текст ответа')
      return
    }
    if (trimmed.length > ANSWER_MAX_LENGTH) {
      setThreadActionError(`Ответ не длиннее ${ANSWER_MAX_LENGTH} символов`)
      return
    }
    setIsSavingAnswer(true)
    try {
      await updateAnswer(editingAnswerId, { answer: trimmed })
      setEditingAnswerId(null)
      setEditAnswerText('')
      await loadThread()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setThreadActionError(getUserFacingApiMessage(error, 'Не удалось сохранить ответ'))
      } else {
        setThreadActionError('Не удалось сохранить ответ')
      }
    } finally {
      setIsSavingAnswer(false)
    }
  }

  const handleRequestDeleteAnswer = (answerId) => {
    const answer = answers.find((a) => Number(a.id) === Number(answerId))
    if (!answer || !isSameAuthor(answer?.author, currentUserId)) {
      return
    }
    setThreadActionError('')
    setConfirmDialog({ type: 'answer', answerId: Number(answerId) })
  }

  const handleConfirmDeleteAnswer = async () => {
    const aid = confirmDialog?.type === 'answer' ? confirmDialog.answerId : null
    if (aid == null) {
      setConfirmDialog(null)
      return
    }
    const answer = answers.find((a) => Number(a.id) === Number(aid))
    if (!answer || !isSameAuthor(answer?.author, currentUserId)) {
      setConfirmDialog(null)
      return
    }
    setIsDestructivePending(true)
    setThreadActionError('')
    try {
      await deleteAnswer(aid)
      setConfirmDialog(null)
      await loadThread()
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/questions/${encodeURIComponent(String(questionId))}`,
        })
        return
      }
      if (isApiError(error)) {
        setThreadActionError(getUserFacingApiMessage(error, 'Не удалось удалить ответ'))
      } else {
        setThreadActionError('Не удалось удалить ответ')
      }
    } finally {
      setIsDestructivePending(false)
      setConfirmDialog(null)
    }
  }

  const handlePostAnswer = async (event) => {
    event.preventDefault()
    setPostError('')
    if (!isAuthenticated) {
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

  const handleCloseConfirm = () => {
    if (isDestructivePending) {
      return
    }
    setConfirmDialog(null)
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

  const confirmTitle =
    confirmDialog?.type === 'question'
      ? 'Удалить вопрос?'
      : confirmDialog?.type === 'answer'
        ? 'Удалить ответ?'
        : ''

  const confirmBody =
    confirmDialog?.type === 'question'
      ? 'Вопрос и все ответы будут удалены без возможности восстановления.'
      : confirmDialog?.type === 'answer'
        ? 'Ответ будет удалён без возможности восстановления.'
        : ''

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

        {threadActionError ? (
          <p className="qa-list-message qa-list-message--error" role="alert">
            {threadActionError}
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
                {isQuestionOwner ? (
                  <div className="qa-thread-owner-actions">
                    {!isEditingQuestion ? (
                      <>
                        <button
                          type="button"
                          className="qa-thread-action-btn"
                          data-testid="qa-thread-edit-question"
                          onClick={handleStartEditQuestion}
                        >
                          Редактировать
                        </button>
                        <button
                          type="button"
                          className="qa-thread-action-btn qa-thread-action-btn--danger"
                          data-testid="qa-thread-delete-question"
                          onClick={handleRequestDeleteQuestion}
                        >
                          Удалить
                        </button>
                      </>
                    ) : null}
                  </div>
                ) : null}

                {isEditingQuestion ? (
                  <div className="qa-thread-edit-panel" data-testid="qa-thread-question-editor">
                    <div className="qa-thread-edit-row">
                      <label htmlFor="qa-edit-question-category">Категория</label>
                      <StyledDropdown
                        variant="form"
                        id="qa-edit-question-category"
                        ariaLabel="Категория вопроса"
                        options={ROOM_CATEGORY_OPTIONS}
                        value={editQuestionCategory}
                        onChange={setEditQuestionCategory}
                        disabled={isSavingQuestion}
                      />
                    </div>
                    <label htmlFor="qa-edit-question-text">Текст вопроса</label>
                    <textarea
                      id="qa-edit-question-text"
                      className="qa-thread-reply-textarea"
                      rows={6}
                      maxLength={QUESTION_MAX_LENGTH}
                      value={editQuestionText}
                      onChange={(e) => setEditQuestionText(e.target.value)}
                      disabled={isSavingQuestion}
                      aria-label="Редактирование текста вопроса"
                    />
                    <div className="qa-thread-edit-footer">
                      <span className="qa-thread-reply-counter">
                        {editQuestionText.length} / {QUESTION_MAX_LENGTH}
                      </span>
                      <div className="qa-thread-edit-buttons">
                        <button
                          type="button"
                          className="qa-thread-action-btn"
                          onClick={handleCancelEditQuestion}
                          disabled={isSavingQuestion}
                        >
                          Отмена
                        </button>
                        <button
                          type="button"
                          className="main-create-activity-btn qa-thread-reply-submit"
                          data-testid="qa-thread-save-question"
                          onClick={handleSaveQuestion}
                          disabled={isSavingQuestion}
                        >
                          {isSavingQuestion ? 'Сохранение...' : 'Сохранить'}
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <>
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
                  </>
                )}
              </div>
            </article>

            <section className="qa-thread-answers" aria-label="Ответы">
              <h2 className="qa-thread-answers-heading">
                {answers.length} {answersWord(answers.length)}
              </h2>

              {answers.map((answer) => {
                const authorName = String(pickFirst(answer.author?.userName, 'Участник'))
                const when = formatDateTimeRu(answer.createdAt)
                const isAnswerOwner = isSameAuthor(answer.author, currentUserId)
                const isEditingThis = editingAnswerId != null && Number(editingAnswerId) === Number(answer.id)

                return (
                  <article className="qa-answer-card" key={answer.id}>
                    <aside className="qa-answer-sidebar" aria-label="Автор ответа">
                      <i className="fa-regular fa-circle-user qa-thread-avatar" aria-hidden="true"></i>
                      <h4 className="qa-answer-author">{authorName}</h4>
                      {when ? <time className="qa-thread-date">{when}</time> : null}
                    </aside>
                    <div className="qa-answer-body">
                      {isAuthenticated && isAnswerOwner && !isEditingThis ? (
                        <div className="qa-thread-owner-actions qa-thread-owner-actions--answer">
                          <button
                            type="button"
                            className="qa-thread-action-btn"
                            data-testid={`qa-thread-edit-answer-${answer.id}`}
                            onClick={() => handleStartEditAnswer(answer)}
                          >
                            Редактировать
                          </button>
                          <button
                            type="button"
                            className="qa-thread-action-btn qa-thread-action-btn--danger"
                            data-testid={`qa-thread-delete-answer-${answer.id}`}
                            onClick={() => handleRequestDeleteAnswer(answer.id)}
                          >
                            Удалить
                          </button>
                        </div>
                      ) : null}
                      {isEditingThis ? (
                        <div className="qa-thread-edit-panel" data-testid={`qa-thread-answer-editor-${answer.id}`}>
                          <textarea
                            className="qa-thread-reply-textarea"
                            rows={4}
                            maxLength={ANSWER_MAX_LENGTH}
                            value={editAnswerText}
                            onChange={(e) => setEditAnswerText(e.target.value)}
                            disabled={isSavingAnswer}
                            aria-label="Редактирование ответа"
                          />
                          <div className="qa-thread-edit-footer">
                            <span className="qa-thread-reply-counter">
                              {editAnswerText.length} / {ANSWER_MAX_LENGTH}
                            </span>
                            <div className="qa-thread-edit-buttons">
                              <button
                                type="button"
                                className="qa-thread-action-btn"
                                onClick={handleCancelEditAnswer}
                                disabled={isSavingAnswer}
                              >
                                Отмена
                              </button>
                              <button
                                type="button"
                                className="main-create-activity-btn qa-thread-reply-submit"
                                data-testid={`qa-thread-save-answer-${answer.id}`}
                                onClick={handleSaveAnswer}
                                disabled={isSavingAnswer}
                              >
                                {isSavingAnswer ? 'Сохранение...' : 'Сохранить'}
                              </button>
                            </div>
                          </div>
                        </div>
                      ) : (
                        <p>{answer.answer}</p>
                      )}
                    </div>
                  </article>
                )
              })}
            </section>

            {isAuthenticated ? (
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

      {confirmDialog ? (
        <div
          className="qa-modal-overlay"
          role="presentation"
          onClick={handleCloseConfirm}
          onKeyDown={(e) => {
            if (e.key === 'Escape') {
              handleCloseConfirm()
            }
          }}
        >
          <div
            className="qa-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="qa-confirm-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="qa-confirm-title" className="qa-modal-title">
              {confirmTitle}
            </h2>
            <p className="qa-modal-body">{confirmBody}</p>
            <div className="qa-modal-actions">
              <button
                type="button"
                className="qa-thread-action-btn"
                onClick={handleCloseConfirm}
                disabled={isDestructivePending}
              >
                Отмена
              </button>
              {confirmDialog.type === 'question' ? (
                <button
                  type="button"
                  className="qa-thread-action-btn qa-thread-action-btn--danger"
                  data-testid="qa-thread-confirm-delete-question"
                  onClick={handleConfirmDeleteQuestion}
                  disabled={isDestructivePending}
                >
                  {isDestructivePending ? 'Удаление...' : 'Удалить'}
                </button>
              ) : (
                <button
                  type="button"
                  className="qa-thread-action-btn qa-thread-action-btn--danger"
                  data-testid="qa-thread-confirm-delete-answer"
                  onClick={handleConfirmDeleteAnswer}
                  disabled={isDestructivePending}
                >
                  {isDestructivePending ? 'Удаление...' : 'Удалить'}
                </button>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}

export default QuestionThreadPage
