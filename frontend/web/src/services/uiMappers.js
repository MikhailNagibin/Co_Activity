import { getRoomCategoryLabel } from '../constants/categoryOptions.js'

function toArray(payload) {
  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload?.items)) {
    return payload.items
  }

  if (Array.isArray(payload?.content)) {
    return payload.content
  }

  return []
}

/**
 * Снимает одну или несколько обёрток JSON-строки ("..."), если контент так сохранён/пришёл с API.
 */
export function normalizeBulletinContent(raw) {
  let s = raw == null ? '' : String(raw)
  let guard = 0
  while (guard < 4 && s.length >= 2 && s.startsWith('"') && s.endsWith('"')) {
    try {
      const parsed = JSON.parse(s)
      if (typeof parsed !== 'string') {
        break
      }
      s = parsed
      guard += 1
    } catch {
      break
    }
  }
  return s
}

export function formatDate(value) {
  if (!value) {
    return 'Дата не указана'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }

  return parsed.toLocaleDateString('ru-RU')
}

/** Дата и время для Q&A (пустая строка, если значения нет). */
export function formatDateTimeRu(value) {
  if (!value) {
    return ''
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }
  return parsed.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function pickFirst(...candidates) {
  for (const candidate of candidates) {
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return candidate
    }
  }

  return null
}

export function mapRoomsToActivityCards(payload) {
  return toArray(payload).map((room) => {
    /** Aligns with core-service RoomSummaryResponse */
    const participantsCount = Number(
      pickFirst(
        room.participantCount,
        room.participantsCount,
        room.currentParticipants,
        0,
      ),
    )
    const capacity = Number(
      pickFirst(room.maximumParticipants, room.maxParticipants, room.capacity, 0),
    )
    const creatorName = pickFirst(
      room.creator?.userName,
      room.creatorName,
      room.authorName,
      room.organizerName,
      room.owner?.userName,
      room.owner?.nickname,
    )

    const roomId = pickFirst(room.id, room.roomId, room.uuid, room.name)
    const numericId =
      roomId !== null && roomId !== undefined && String(roomId).trim() !== '' && !Number.isNaN(Number(roomId))
        ? Number(roomId)
        : null

    const categoryRaw = pickFirst(room.category, room.categoryName)
    const categoryKey =
      categoryRaw != null && String(categoryRaw).trim() !== '' ? String(categoryRaw).trim() : ''

    const rawPublic = room.isPublic
    const isPublic = rawPublic === false ? false : true

    const isFull = capacity > 0 && participantsCount >= capacity

    const startRaw = pickFirst(room.dateOfStartEvent, room.date, room.eventDate)
    let eventStartMs = null
    if (startRaw != null && String(startRaw).trim() !== '') {
      const parsed = Date.parse(startRaw)
      eventStartMs = Number.isNaN(parsed) ? null : parsed
    }

    const creatorCity = String(room.creator?.city ?? '').trim()
    const creatorCountry = String(room.creator?.country ?? '').trim()

    return {
      id: roomId,
      sortId: numericId ?? 0,
      linkTo: numericId != null ? `/rooms/${numericId}` : null,
      title: String(pickFirst(room.name, room.title, 'Без названия')),
      description: String(pickFirst(room.description, room.summary, 'Описание отсутствует')),
      categoryKey,
      category: getRoomCategoryLabel(categoryRaw),
      isPublic,
      isFull,
      ageRating: Number(room.ageRating) || 0,
      participantsCount,
      maximumParticipants: capacity,
      creatorCity,
      creatorCountry,
      eventStartMs,
      date: formatDate(
        pickFirst(room.dateOfStartEvent, room.date, room.eventDate, room.createdAt),
      ),
      capacity:
        capacity > 0
          ? `Набрано ${participantsCount}/${capacity}`
          : `Участников: ${participantsCount || 0}`,
      author: String(pickFirst(creatorName, 'Неизвестный автор')),
    }
  })
}

export function splitQuestionTitleBody(text) {
  if (!text || typeof text !== 'string') {
    return { title: 'Без названия', body: 'Описание отсутствует' }
  }
  const trimmed = text.trim()
  const maxTitle = 120
  if (trimmed.length <= maxTitle) {
    return { title: trimmed, body: trimmed }
  }
  return {
    title: `${trimmed.slice(0, maxTitle)}…`,
    body: trimmed,
  }
}

export function mapQuestionsToPreview(payload) {
  return toArray(payload).map((question) => {
    /** Aligns with core-service QuestionResponse: question, author (UserSummaryResponse), category */
    const rawText = pickFirst(question.question, question.title, question.body, '')
    const { title, body } = splitQuestionTitleBody(rawText)
    const categoryRaw = pickFirst(question.category, question.categoryName)
    const categoryKey =
      categoryRaw != null && String(categoryRaw).trim() !== '' ? String(categoryRaw).trim() : ''
    const categoryTag =
      categoryRaw != null && String(categoryRaw).trim() !== '' ? [String(categoryRaw)] : []
    const extraTags = Array.isArray(question.tags) ? question.tags.map((tag) => String(tag)) : []

    const rawId = pickFirst(question.id, question.questionId, question.uuid)
    const numericId =
      rawId !== null && rawId !== undefined && String(rawId).trim() !== '' && !Number.isNaN(Number(rawId))
        ? Number(rawId)
        : null

    const createdRaw = pickFirst(question.createdAt, question.createdDate)
    const createdIso =
      createdRaw != null && String(createdRaw).trim() !== '' ? String(createdRaw) : null
    const answersRaw = pickFirst(question.answersCount, question.answerCount, 0)
    const answersNum = Number(answersRaw)
    const answersCount = Number.isFinite(answersNum) ? answersNum : 0

    return {
      id: rawId ?? title,
      sortId: numericId ?? 0,
      author: String(pickFirst(question.author?.userName, question.authorName, 'Неизвестный автор')),
      createdAt: formatDateTimeRu(createdIso),
      createdAtIso: createdIso,
      categoryKey,
      categoryLabel: getRoomCategoryLabel(categoryRaw),
      title: String(title),
      description: String(body),
      tags: [...categoryTag, ...extraTags],
      answersCount,
      linkTo: numericId != null ? `/questions/${numericId}` : null,
    }
  })
}
