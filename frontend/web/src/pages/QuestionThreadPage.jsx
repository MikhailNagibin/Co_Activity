import { Link, useNavigate, useParams } from 'react-router-dom'
import { useCallback, useEffect, useMemo, useState } from 'react'
import AppHeader from '../components/AppHeader.jsx'
import UserAvatar from '../components/UserAvatar.jsx'
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
import {
  ROOM_CATEGORY_OPTIONS,
  getRoomCategoryLabel,
  normalizeRoomCategory,
} from '../constants/categoryOptions.js'

const ANSWER_MAX_LENGTH = 2000
const QUESTION_MAX_LENGTH = 2000
const QUESTION_TITLE_MAX_LENGTH = 120
const QUESTION_BODY_MAX_LENGTH = 1800
const MAX_ANSWER_TREE_LEVEL = 3

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
  const raw = pickFirst(author?.id, author?.userId, null)
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
  const [replyParent, setReplyParent] = useState(null)
  const [isPostingAnswer, setIsPostingAnswer] = useState(false)

  const [isEditingQuestion, setIsEditingQuestion] = useState(false)
  const [editQuestionTitle, setEditQuestionTitle] = useState('')
  const [editQuestionBody, setEditQuestionBody] = useState('')
  const [editQuestionCategory, setEditQuestionCategory] = useState('NOT_SPECIFIED')
  const [isSavingQuestion, setIsSavingQuestion] = useState(false)

  const [editingAnswerId, setEditingAnswerId] = useState(null)
  const [editAnswerText, setEditAnswerText] = useState('')
  const [isSavingAnswer, setIsSavingAnswer] = useState(false)

  const [confirmDialog, setConfirmDialog] = useState(null)
  const [isDestructivePending, setIsDestructivePending] = useState(false)

  const currentUserId = useMemo(() => parseUserId(currentUser), [currentUser])

  const signInHref = useMemo(() => {
    if (Number.isFinite(questionId) && questionId > 0) {
      return `/sign-in?${new URLSearchParams({ next: `/questions/${questionId}` }).toString()}`
    }
    return '/sign-in'
  }, [questionId])

  const loadThread = useCallback(async (options = {}) => {
    const { silent = false } = options
    if (!silent) {
      setIsLoading(true)
      setLoadError('')
    }
    try {
      const data = await getQuestionWithAnswers(questionId)
      setPayload(data)
      if (silent) {
        setLoadError('')
      }
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
      if (!silent) {
        setPayload(null)
      }
    } finally {
      if (!silent) {
        setIsLoading(false)
      }
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
  const categoryLabel =
    categoryTag.length > 0 ? getRoomCategoryLabel(categoryValueForRequest(categoryTag[0])) : ''
  const questionAuthorName = String(
    pickFirst(questionBlock?.author?.userName, 'Неизвестный автор'),
  )
  const questionAuthorId = parseAuthorId(questionBlock?.author)
  const questionCreatedRaw = pickFirst(questionBlock?.createdAt, '')
  const questionCreatedLabel = formatDateTimeRu(questionCreatedRaw)

  const answerTree = useMemo(
    () => (Array.isArray(payload?.answers) ? payload.answers : []),
    [payload?.answers],
  )
  const answers = useMemo(() => flattenAnswers(answerTree), [answerTree])

  const isQuestionOwner =
    isAuthenticated && isSameAuthor(questionBlock?.author, currentUserId)

  const handleStartEditQuestion = () => {
    if (!isQuestionOwner) {
      return
    }
    setThreadActionError('')
    setEditQuestionTitle(displayTitle)
    setEditQuestionBody(displayBody)
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
    const trimmedTitle = editQuestionTitle.trim()
    const trimmedBody = editQuestionBody.trim()
    if (!trimmedTitle) {
      setThreadActionError('Введите заголовок вопроса')
      return
    }
    if (trimmedTitle.length > QUESTION_TITLE_MAX_LENGTH) {
      setThreadActionError(`Заголовок не длиннее ${QUESTION_TITLE_MAX_LENGTH} символов`)
      return
    }
    if (!trimmedBody) {
      setThreadActionError('Опишите вопрос подробнее')
      return
    }
    if (trimmedBody.length > QUESTION_BODY_MAX_LENGTH) {
      setThreadActionError(`Подробное описание не длиннее ${QUESTION_BODY_MAX_LENGTH} символов`)
      return
    }
    const trimmed = `${trimmedTitle}\n\n${trimmedBody}`
    if (trimmed.length > QUESTION_MAX_LENGTH) {
      setThreadActionError(`Вопрос целиком не длиннее ${QUESTION_MAX_LENGTH} символов`)
      return
    }
    setIsSavingQuestion(true)
    try {
      await updateQuestion(questionId, {
        category: editQuestionCategory,
        question: trimmed,
      })
      setIsEditingQuestion(false)
      await loadThread({ silent: true })
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
    setReplyParent(null)
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
      await loadThread({ silent: true })
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
      await loadThread({ silent: true })
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
        previousAnswerId: replyParent?.id ?? null,
      })
      setAnswerText('')
      setReplyParent(null)
      await loadThread({ silent: true })
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

  const handleStartReply = (answer, depth = 0) => {
    if (!isAuthenticated || !answer?.id || depth + 1 >= MAX_ANSWER_TREE_LEVEL) {
      return
    }
    setPostError('')
    setThreadActionError('')
    setEditingAnswerId(null)
    setEditAnswerText('')
    setReplyParent({
      id: answer.id,
      authorName: String(pickFirst(answer.author?.userName, 'участника')),
    })
  }

  const handleCancelReply = () => {
    setReplyParent(null)
    setPostError('')
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
        <div className="main-page-shell qa-thread-shell">
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

  const renderAnswerNode = (answer, depth = 0) => {
    const authorName = String(pickFirst(answer.author?.userName, 'Участник'))
    const authorId = parseAuthorId(answer.author)
    const when = formatDateTimeRu(answer.createdAt)
    const isAnswerOwner = isSameAuthor(answer.author, currentUserId)
    const isEditingThis = editingAnswerId != null && Number(editingAnswerId) === Number(answer.id)
    const childAnswers = Array.isArray(answer?.replies) ? answer.replies : []
    const canReplyToAnswer = depth + 1 < MAX_ANSWER_TREE_LEVEL
    const visualDepth = Math.min(depth, MAX_ANSWER_TREE_LEVEL - 1)

    return (
      <article className="qa-answer-branch" key={answer.id} style={{ '--qa-answer-depth': visualDepth }}>
        <div className="qa-answer-card">
          <aside className="qa-answer-sidebar" aria-label="Автор ответа">
            {isAuthenticated && authorId != null ? (
              <Link to={`/users/${authorId}`} className="qa-thread-author-link" aria-label={`Профиль ${authorName}`}>
                <UserAvatar
                  user={answer.author}
                  alt={`Аватар, ${authorName}`}
                  className="qa-thread-avatar-slot qa-thread-avatar-slot--answer"
                  size="lg"
                />
              </Link>
            ) : (
              <UserAvatar
                user={answer.author}
                alt={`Аватар, ${authorName}`}
                className="qa-thread-avatar-slot qa-thread-avatar-slot--answer"
                size="lg"
              />
            )}
            <h4 className="qa-answer-author">
              {isAuthenticated && authorId != null ? (
                <Link to={`/users/${authorId}`}>{authorName}</Link>
              ) : (
                authorName
              )}
            </h4>
            {when ? <time className="qa-thread-date">{when}</time> : null}
          </aside>
          <div className="qa-answer-body">
            <div className="qa-thread-owner-actions qa-thread-owner-actions--answer">
              {isAuthenticated && !isEditingThis && canReplyToAnswer ? (
                <button
                  type="button"
                  className="qa-thread-action-btn"
                  data-testid={`qa-thread-reply-answer-${answer.id}`}
                  onClick={() => handleStartReply(answer, depth)}
                >
                  Ответить
                </button>
              ) : null}
              {isAuthenticated && isAnswerOwner && !isEditingThis ? (
                <>
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
                </>
              ) : null}
            </div>
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
        </div>
        {childAnswers.length > 0 ? (
          <div className="qa-answer-children">
            {childAnswers.map((child) => renderAnswerNode(child, depth + 1))}
          </div>
        ) : null}
      </article>
    )
  }

  return (
    <>
      <AppHeader activeTab="qa" />
      <div className="main-page-shell qa-thread-shell">
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
                {isAuthenticated && questionAuthorId != null ? (
                  <Link to={`/users/${questionAuthorId}`} className="qa-thread-author-link" aria-label={`Профиль ${questionAuthorName}`}>
                    <UserAvatar
                      user={questionBlock?.author}
                      alt={`Аватар, ${questionAuthorName}`}
                      className="qa-thread-avatar-slot qa-thread-avatar-slot--question"
                      size="xl"
                    />
                  </Link>
                ) : (
                  <UserAvatar
                    user={questionBlock?.author}
                    alt={`Аватар, ${questionAuthorName}`}
                    className="qa-thread-avatar-slot qa-thread-avatar-slot--question"
                    size="xl"
                  />
                )}
                <h3 className="qa-thread-author-name">
                  {isAuthenticated && questionAuthorId != null ? (
                    <Link to={`/users/${questionAuthorId}`}>{questionAuthorName}</Link>
                  ) : (
                    questionAuthorName
                  )}
                </h3>
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
                    <div className="qa-thread-edit-row">
                      <label htmlFor="qa-edit-question-title">Заголовок вопроса</label>
                      <input
                        id="qa-edit-question-title"
                        className="qa-thread-edit-input"
                        type="text"
                        maxLength={QUESTION_TITLE_MAX_LENGTH}
                        value={editQuestionTitle}
                        onChange={(e) => setEditQuestionTitle(e.target.value)}
                        disabled={isSavingQuestion}
                        aria-label="Редактирование заголовка вопроса"
                      />
                      <span className="qa-thread-reply-counter">
                        {editQuestionTitle.length} / {QUESTION_TITLE_MAX_LENGTH}
                      </span>
                    </div>
                    <div className="qa-thread-edit-row">
                      <label htmlFor="qa-edit-question-text">Подробное описание</label>
                      <textarea
                        id="qa-edit-question-text"
                        className="qa-thread-reply-textarea"
                        rows={6}
                        maxLength={QUESTION_BODY_MAX_LENGTH}
                        value={editQuestionBody}
                        onChange={(e) => setEditQuestionBody(e.target.value)}
                        disabled={isSavingQuestion}
                        aria-label="Редактирование подробного описания вопроса"
                      />
                    </div>
                    <div className="qa-thread-edit-footer">
                      <span className="qa-thread-reply-counter">
                        {editQuestionBody.length} / {QUESTION_BODY_MAX_LENGTH}
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
                    <div className="qa-thread-meta-chips" aria-label="Информация о вопросе">
                      {categoryLabel ? (
                        <div className="qa-thread-meta-chip">
                          <span className="qa-thread-meta-label">Категория</span>
                          <span className="qa-thread-meta-value">{categoryLabel}</span>
                        </div>
                      ) : null}
                      {questionCreatedLabel ? (
                        <div className="qa-thread-meta-chip">
                          <span className="qa-thread-meta-label">Создан</span>
                          <span className="qa-thread-meta-value">{questionCreatedLabel}</span>
                        </div>
                      ) : null}
                      <div className="qa-thread-meta-chip qa-thread-meta-chip--accent">
                        <span className="qa-thread-meta-label">Обсуждение</span>
                        <span className="qa-thread-meta-value">
                          {answers.length} {answersWord(answers.length)}
                        </span>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </article>

            <section className="qa-thread-answers" aria-label="Ответы">
              <h2 className="qa-thread-answers-heading">
                {answers.length} {answersWord(answers.length)}
              </h2>

              <div className="qa-answer-tree">
                {answerTree.map((answer) => renderAnswerNode(answer))}
              </div>
            </section>

            {isAuthenticated ? (
              <section className="qa-thread-reply" aria-label="Ваш ответ">
                <div className="qa-thread-reply-heading-row">
                  <h2 className="qa-thread-answers-heading">
                    {replyParent ? `Ответить ${replyParent.authorName}` : 'Добавить ответ'}
                  </h2>
                  {replyParent ? (
                    <button type="button" className="auth-text-button" onClick={handleCancelReply}>
                      Отменить ответ
                    </button>
                  ) : null}
                </div>
                <form className="qa-thread-reply-form" onSubmit={handlePostAnswer}>
                  <textarea
                    className="qa-thread-reply-textarea"
                    rows={5}
                    maxLength={ANSWER_MAX_LENGTH}
                    value={answerText}
                    onChange={(e) => setAnswerText(e.target.value)}
                    disabled={isPostingAnswer}
                    placeholder={replyParent ? 'Напишите ответ на сообщение...' : 'Напишите ответ сообществу...'}
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
                <Link className="qa-thread-cta-link" to={signInHref}>
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
