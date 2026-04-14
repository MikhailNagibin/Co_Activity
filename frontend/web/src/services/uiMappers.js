import { getRoomCategoryLabel } from '../constants/categoryOptions.js'
import { sortRoomImages } from '../utils/roomForm.js'

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

export function normalizeMembershipStatus(status) {
  const normalized = String(status ?? '')
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '_')

  if (normalized === 'CONSIDERATION') {
    return 'PENDING'
  }

  return normalized
}

export function normalizeMembershipRole(role) {
  return String(role ?? '')
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '_')
}

export function getRoomMembershipView(room, membershipOverride = null) {
  const membership = membershipOverride ?? room?.membershipStatus ?? null
  const membershipStatus = normalizeMembershipStatus(
    membership?.status ?? (room?.isCurrentUserParticipant === true ? 'PARTICIPANT' : ''),
  )
  const membershipRole = normalizeMembershipRole(membership?.role)
  const pendingRequestId = membership?.pendingRequestId ?? null
  const isParticipant = membershipStatus === 'PARTICIPANT'
  const isBanned = membershipStatus === 'BANNED'
  const hasPendingRequest = membershipStatus === 'PENDING' || pendingRequestId != null
  const hasProtectedAccess =
    membershipOverride != null || room?.membershipStatus != null
      ? isParticipant
      : room?.hasProtectedAccess === true
  const canJoin =
    typeof membership?.canJoin === 'boolean'
      ? membership.canJoin
      : !isParticipant && !isBanned && !hasPendingRequest

  return {
    membershipStatus,
    membershipRole,
    pendingRequestId,
    isParticipant,
    isBanned,
    hasPendingRequest,
    hasProtectedAccess,
    canJoin,
    canLeave: isParticipant && membershipRole !== 'OWNER',
    canDeleteRoom: membershipRole === 'OWNER',
    canModerate: membershipRole === 'OWNER' || membershipRole === 'ADMIN',
  }
}

export function mergeRoomsWithMembershipStatuses(payload, membershipStatuses) {
  const statusMap = new Map(
    toArray(membershipStatuses)
      .filter((status) => status?.roomId != null)
      .map((status) => [Number(status.roomId), status]),
  )

  return toArray(payload).map((room) => {
    const roomId = Number(pickFirst(room?.id, room?.roomId, room?.uuid))
    if (!Number.isFinite(roomId)) {
      return room
    }

    const membershipStatus = statusMap.get(roomId)
    if (!membershipStatus) {
      return room
    }

    const membershipView = getRoomMembershipView(room, membershipStatus)
    return {
      ...room,
      membershipStatus,
      isCurrentUserParticipant: membershipView.isParticipant,
      hasProtectedAccess: membershipView.hasProtectedAccess,
    }
  })
}

export function formatJoinRequestStatus(status) {
  switch (normalizeMembershipStatus(status)) {
    case 'PENDING':
      return 'На рассмотрении'
    case 'ACCEPTED':
      return 'Принято'
    case 'REFUSED':
      return 'Отклонено'
    case 'REFUSED_WITH_BAN':
      return 'Отклонено с баном'
    default:
      return 'Статус неизвестен'
  }
}

export function mapRoomsToActivityCards(payload) {
  return toArray(payload).map((room) => {
    /** Aligns with core-service RoomSummaryResponse */
    const membershipView = getRoomMembershipView(room)
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
    const images = sortRoomImages(room.images)

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
      roomStatus: String(pickFirst(room.status, room.state, room.roomStatus, 'ACTIVE')),
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
      membershipStatus: membershipView.membershipStatus,
      membershipRole: membershipView.membershipRole,
      pendingRequestId: membershipView.pendingRequestId,
      canJoin: membershipView.canJoin,
      canLeave: membershipView.canLeave,
      canDeleteRoom: membershipView.canDeleteRoom,
      hasProtectedAccess: membershipView.hasProtectedAccess,
      images,
    }
  })
}

export function mapSentJoinRequestsToCards(payload) {
  return toArray(payload).map((request) => {
    const roomId = pickFirst(request.roomId, request.room?.id)
    const numericRoomId =
      roomId !== null &&
      roomId !== undefined &&
      String(roomId).trim() !== '' &&
      !Number.isNaN(Number(roomId))
        ? Number(roomId)
        : null

    const createdRaw = pickFirst(request.createdAt, request.createdDate)
    const createdIso =
      createdRaw != null && String(createdRaw).trim() !== '' ? String(createdRaw) : null

    return {
      id: pickFirst(request.requestId, request.id, `${roomId ?? 'room'}-${createdIso ?? 'request'}`),
      requestId: Number(pickFirst(request.requestId, request.id)) || null,
      roomId: numericRoomId,
      roomName: String(pickFirst(request.roomName, request.room?.name, 'Без названия')),
      username: String(pickFirst(request.username, request.userName, '')),
      status: normalizeMembershipStatus(request.status),
      statusLabel: formatJoinRequestStatus(request.status),
      createdAt: formatDateTimeRu(createdIso),
      createdAtIso: createdIso,
      linkTo: numericRoomId != null ? `/rooms/${numericRoomId}` : null,
    }
  })
}

export function mapIncomingJoinRequests(payload) {
  return toArray(payload)
    .map((request) => {
      const roomId = pickFirst(request.roomId, request.room?.id)
      const numericRoomId =
        roomId !== null &&
        roomId !== undefined &&
        String(roomId).trim() !== '' &&
        !Number.isNaN(Number(roomId))
          ? Number(roomId)
          : null

      const userId = pickFirst(request.userId, request.user?.id)
      const numericUserId =
        userId !== null &&
        userId !== undefined &&
        String(userId).trim() !== '' &&
        !Number.isNaN(Number(userId))
          ? Number(userId)
          : null

      const createdRaw = pickFirst(request.createdAt, request.createdDate)
      const createdIso =
        createdRaw != null && String(createdRaw).trim() !== '' ? String(createdRaw) : null

      return {
        id: pickFirst(
          request.requestId,
          request.id,
          `${numericRoomId ?? 'room'}-${numericUserId ?? 'user'}-${createdIso ?? 'request'}`,
        ),
        requestId: Number(pickFirst(request.requestId, request.id)) || null,
        roomId: numericRoomId,
        roomName: String(pickFirst(request.roomName, request.room?.name, 'Без названия')),
        roomLink: numericRoomId != null ? `/rooms/${numericRoomId}` : null,
        userId: numericUserId,
        username: String(pickFirst(request.username, request.userName, request.user?.username, 'Пользователь')),
        userLink: numericUserId != null ? `/users/${numericUserId}` : null,
        status: normalizeMembershipStatus(request.status),
        statusLabel: formatJoinRequestStatus(request.status),
        canManage: request.canManage !== false,
        createdAt: formatDateTimeRu(createdIso),
        createdAtIso: createdIso,
      }
    })
    .sort((a, b) => String(b.createdAtIso ?? '').localeCompare(String(a.createdAtIso ?? '')))
}

export function groupIncomingJoinRequestsByRoom(requests) {
  const groups = new Map()

  for (const request of requests) {
    const groupKey = request.roomId ?? request.roomName ?? request.id
    if (!groups.has(groupKey)) {
      groups.set(groupKey, {
        roomId: request.roomId,
        roomName: request.roomName,
        roomLink: request.roomLink,
        requests: [],
      })
    }
    groups.get(groupKey).requests.push(request)
  }

  return [...groups.values()].sort((a, b) =>
    String(a.roomName ?? '').localeCompare(String(b.roomName ?? ''), 'ru'),
  )
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
