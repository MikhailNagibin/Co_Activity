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

function formatDate(value) {
  if (!value) {
    return 'Дата не указана'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }

  return parsed.toLocaleDateString('ru-RU')
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
    const defaultImage = roomId
      ? `https://picsum.photos/seed/room${roomId}/800/400`
      : 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800'

    return {
      id: roomId,
      title: String(pickFirst(room.name, room.title, 'Без названия')),
      description: String(pickFirst(room.description, room.summary, 'Описание отсутствует')),
      location: String(
        pickFirst(
          [room.creator?.city, room.creator?.country].filter(Boolean).join(', '),
          room.location,
          room.address,
          'Локация не указана в API',
        ),
      ),
      date: formatDate(
        pickFirst(room.dateOfStartEvent, room.date, room.eventDate, room.createdAt),
      ),
      capacity:
        capacity > 0
          ? `Набрано ${participantsCount}/${capacity}`
          : `Участников: ${participantsCount || 0}`,
      author: String(pickFirst(creatorName, 'Неизвестный автор')),
      image: String(pickFirst(room.imageUrl, room.coverImageUrl, defaultImage)),
    }
  })
}

function splitQuestionTitleBody(text) {
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
    const categoryTag =
      question.category != null && String(question.category).trim() !== ''
        ? [String(question.category)]
        : []
    const extraTags = Array.isArray(question.tags) ? question.tags.map((tag) => String(tag)) : []

    return {
      id: pickFirst(question.id, question.questionId, question.uuid, title),
      author: String(pickFirst(question.author?.userName, question.authorName, 'Неизвестный автор')),
      createdAt: formatDate(pickFirst(question.createdAt, question.createdDate)),
      title: String(title),
      description: String(body),
      tags: [...categoryTag, ...extraTags],
      answersCount: Number(pickFirst(question.answersCount, question.answerCount, 0)),
    }
  })
}
