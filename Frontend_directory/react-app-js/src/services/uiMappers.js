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
    const participantsCount = Number(pickFirst(room.participantsCount, room.currentParticipants, 0))
    const capacity = Number(pickFirst(room.capacity, room.maxParticipants, 0))

    return {
      id: pickFirst(room.id, room.roomId, room.uuid, room.title),
      title: String(pickFirst(room.title, room.name, 'Без названия')),
      description: String(pickFirst(room.description, room.summary, 'Описание отсутствует')),
      location: String(pickFirst(room.location, room.address, 'Локация не указана')),
      date: formatDate(pickFirst(room.date, room.eventDate, room.createdAt)),
      capacity:
        capacity > 0
          ? `Набрано ${participantsCount}/${capacity}`
          : `Участников: ${participantsCount || 0}`,
      author: String(
        pickFirst(
          room.authorName,
          room.creatorName,
          room.organizerName,
          room.owner?.nickname,
          'Неизвестный автор',
        ),
      ),
      image: String(
        pickFirst(
          room.imageUrl,
          room.coverImageUrl,
          'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800',
        ),
      ),
    }
  })
}

export function mapQuestionsToPreview(payload) {
  return toArray(payload).map((question) => ({
    id: pickFirst(question.id, question.questionId, question.uuid, question.title),
    author: String(
      pickFirst(
        question.authorName,
        question.ownerName,
        question.user?.nickname,
        question.author?.nickname,
        'Неизвестный автор',
      ),
    ),
    createdAt: formatDate(pickFirst(question.createdAt, question.createdDate)),
    title: String(pickFirst(question.title, 'Без названия')),
    description: String(pickFirst(question.description, question.body, 'Описание отсутствует')),
    tags: Array.isArray(question.tags) ? question.tags.map((tag) => String(tag)) : [],
    answersCount: Number(pickFirst(question.answersCount, question.answerCount, 0)),
  }))
}
