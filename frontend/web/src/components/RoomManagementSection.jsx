import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  assignRoomAdmin,
  banRoomUser,
  demoteRoomAdmin,
  getRoomBans,
  getRoomParticipants,
  removeRoomParticipant,
  transferRoomOwnership,
  unbanRoomUser,
} from '../services/roomsService.js'
import { normalizeMembershipRole } from '../services/uiMappers.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import { resolveUserAvatarUrl, resolveUserName } from '../utils/userProfile.js'

const MANAGEMENT_TABS = [
  { id: 'participants', label: 'Участники' },
  { id: 'bans', label: 'Баны' },
  { id: 'roles', label: 'Роли' },
]

function getRoleLabel(role) {
  switch (normalizeMembershipRole(role)) {
    case 'OWNER':
      return 'Создатель'
    case 'ADMIN':
      return 'Админ'
    case 'PARTICIPANT':
      return 'Участник'
    default:
      return 'Не указано'
  }
}

function getRoleRank(role) {
  switch (normalizeMembershipRole(role)) {
    case 'OWNER':
      return 0
    case 'ADMIN':
      return 1
    case 'PARTICIPANT':
      return 2
    default:
      return 3
  }
}

function getGovernanceUserName(user, fallback = 'Пользователь') {
  return resolveUserName(user, user?.name ?? fallback)
}

function getUserLocation(user) {
  return [user?.country, user?.city]
    .map((part) => String(part ?? '').trim())
    .filter(Boolean)
    .join(', ')
}

function getUserDescription(user) {
  return String(user?.description ?? '').trim()
}

function compareUsers(a, b) {
  const roleDiff = getRoleRank(a?.role) - getRoleRank(b?.role)
  if (roleDiff !== 0) {
    return roleDiff
  }

  return getGovernanceUserName(a).localeCompare(getGovernanceUserName(b), 'ru')
}

function compareNames(a, b) {
  return getGovernanceUserName(a).localeCompare(getGovernanceUserName(b), 'ru')
}

function GovernanceUserCard({
  user,
  roleLabel,
  subtitle,
  description,
  actions,
  isBusy,
}) {
  const avatarUrl = resolveUserAvatarUrl(user)
  const displayName = getGovernanceUserName(user)

  return (
    <article className="room-governance-card">
      <div className="room-governance-card__main">
        <div className="room-governance-card__identity">
          {avatarUrl ? (
            <img
              src={avatarUrl}
              alt={`Аватар пользователя ${displayName}`}
              className="room-governance-card__avatar"
            />
          ) : (
            <div className="room-governance-card__avatar room-governance-card__avatar--placeholder" aria-hidden="true">
              <i className="fa-regular fa-circle-user"></i>
            </div>
          )}
          <div className="room-governance-card__text">
            {user?.id ? (
              <Link to={`/users/${user.id}`} className="room-governance-card__name">
                {displayName}
              </Link>
            ) : (
              <span className="room-governance-card__name">{displayName}</span>
            )}
            {roleLabel ? <span className="room-governance-card__role">{roleLabel}</span> : null}
            {subtitle ? <p className="room-governance-card__subtitle">{subtitle}</p> : null}
            {description ? <p className="room-governance-card__description">{description}</p> : null}
          </div>
        </div>
        {actions.length > 0 ? (
          <div className="room-governance-card__actions">
            {actions.map((action) => (
              <button
                key={action.key}
                type="button"
                className={action.tone === 'danger' ? 'room-governance-action room-governance-action--danger' : 'room-governance-action'}
                onClick={action.onClick}
                disabled={Boolean(action.disabled) || isBusy}
              >
                {isBusy && action.isCurrent ? action.pendingLabel ?? 'Сохранение...' : action.label}
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </article>
  )
}

function RoomManagementSection({
  roomId,
  roomName,
  currentUserId,
  currentUserRole,
  onRefreshRoom,
}) {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('participants')
  const [participants, setParticipants] = useState([])
  const [bans, setBans] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [feedbackTone, setFeedbackTone] = useState('success')
  const [pendingActionKey, setPendingActionKey] = useState('')

  const normalizedCurrentRole = normalizeMembershipRole(currentUserRole)
  const canModerate = normalizedCurrentRole === 'OWNER' || normalizedCurrentRole === 'ADMIN'
  const canManageRoles = normalizedCurrentRole === 'OWNER'

  const loadGovernanceData = useCallback(async (options = {}) => {
    const { preserveFeedback = false } = options

    if (!canModerate) {
      setParticipants([])
      setBans([])
      setErrorMessage('')
      setIsLoading(false)
      return false
    }

    setIsLoading(true)
    setErrorMessage('')
    if (!preserveFeedback) {
      setFeedback('')
    }

    try {
      const [participantsPayload, bansPayload] = await Promise.all([
        getRoomParticipants(roomId),
        getRoomBans(roomId),
      ])
      setParticipants(Array.isArray(participantsPayload) ? participantsPayload : [])
      setBans(Array.isArray(bansPayload) ? bansPayload : [])
      return true
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return false
      }

      setParticipants([])
      setBans([])
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить управление активностью.'))
      } else {
        setErrorMessage('Не удалось загрузить управление активностью.')
      }
      return false
    } finally {
      setIsLoading(false)
    }
  }, [canModerate, navigate, roomId])

  useEffect(() => {
    loadGovernanceData()
  }, [loadGovernanceData])

  const sortedParticipants = useMemo(
    () => [...participants].sort(compareUsers),
    [participants],
  )
  const sortedBans = useMemo(() => [...bans].sort(compareNames), [bans])

  const runGovernanceAction = useCallback(async ({
    actionKey,
    confirmMessage,
    action,
    successMessage,
    errorMessage: fallbackMessage,
  }) => {
    if (confirmMessage && !window.confirm(confirmMessage)) {
      return
    }

    setPendingActionKey(actionKey)
    setFeedback('')
    setFeedbackTone('success')

    try {
      await action()

      if (typeof onRefreshRoom === 'function') {
        await onRefreshRoom({ preserveJoinFeedback: true })
      }
      await loadGovernanceData({ preserveFeedback: true })

      setFeedback(successMessage)
      setFeedbackTone('success')
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return
      }

      if (isApiError(error)) {
        setFeedback(getUserFacingApiMessage(error, fallbackMessage))
      } else {
        setFeedback(fallbackMessage)
      }
      setFeedbackTone('error')
    } finally {
      setPendingActionKey('')
    }
  }, [loadGovernanceData, navigate, onRefreshRoom, roomId])

  if (!canModerate) {
    return null
  }

  const renderParticipantsTab = () => {
    if (sortedParticipants.length === 0) {
      return <p className="gray-elem">Участники пока не найдены.</p>
    }

    return (
      <div className="room-governance-list">
        {sortedParticipants.map((participant) => {
          const role = normalizeMembershipRole(participant?.role)
          const userId = Number(participant?.id)
          const isSelf = Number.isFinite(userId) && Number(userId) === Number(currentUserId)
          const canRemove =
            Number.isFinite(userId) &&
            !isSelf &&
            role !== 'OWNER' &&
            (canManageRoles || role === 'PARTICIPANT')
          const canBan =
            Number.isFinite(userId) &&
            !isSelf &&
            role !== 'OWNER' &&
            (canManageRoles || role === 'PARTICIPANT')
          const location = getUserLocation(participant)
          const description = getUserDescription(participant)

          const actions = [
            canRemove
              ? {
                  key: `remove-${userId}`,
                  label: 'Удалить из комнаты',
                  pendingLabel: 'Удаление...',
                  tone: 'danger',
                  isCurrent: pendingActionKey === `remove-${userId}`,
                  onClick: () => runGovernanceAction({
                    actionKey: `remove-${userId}`,
                    confirmMessage: `Удалить пользователя «${getGovernanceUserName(participant)}» из активности «${roomName || 'Без названия'}»?`,
                    action: () => removeRoomParticipant(roomId, userId),
                    successMessage: 'Участник удалён из активности.',
                    errorMessage: 'Не удалось удалить участника из активности.',
                  }),
                }
              : null,
            canBan
              ? {
                  key: `ban-${userId}`,
                  label: 'Забанить',
                  pendingLabel: 'Блокировка...',
                  tone: 'danger',
                  isCurrent: pendingActionKey === `ban-${userId}`,
                  onClick: () => runGovernanceAction({
                    actionKey: `ban-${userId}`,
                    confirmMessage: `Забанить пользователя «${getGovernanceUserName(participant)}» в активности «${roomName || 'Без названия'}»?`,
                    action: () => banRoomUser(roomId, userId),
                    successMessage: 'Пользователь забанен.',
                    errorMessage: 'Не удалось забанить пользователя.',
                  }),
                }
              : null,
          ].filter(Boolean)

          return (
            <GovernanceUserCard
              key={participant?.id ?? `${participant?.name}-${participant?.role}`}
              user={participant}
              roleLabel={getRoleLabel(role)}
              subtitle={location || (isSelf ? 'Это вы' : '')}
              description={description}
              actions={actions}
              isBusy={Boolean(pendingActionKey)}
            />
          )
        })}
      </div>
    )
  }

  const renderBansTab = () => {
    if (sortedBans.length === 0) {
      return <p className="gray-elem">Забаненных пользователей пока нет.</p>
    }

    return (
      <div className="room-governance-list">
        {sortedBans.map((user) => {
          const userId = Number(user?.id)
          const location = getUserLocation(user)
          const actions = Number.isFinite(userId)
            ? [
                {
                  key: `unban-${userId}`,
                  label: 'Снять бан',
                  pendingLabel: 'Разбан...',
                  isCurrent: pendingActionKey === `unban-${userId}`,
                  onClick: () => runGovernanceAction({
                    actionKey: `unban-${userId}`,
                    confirmMessage: `Снять бан с пользователя «${getGovernanceUserName(user)}»?`,
                    action: () => unbanRoomUser(roomId, userId),
                    successMessage: 'Бан снят.',
                    errorMessage: 'Не удалось снять бан.',
                  }),
                },
              ]
            : []

          return (
            <GovernanceUserCard
              key={user?.id ?? getGovernanceUserName(user)}
              user={user}
              roleLabel="Забанен"
              subtitle={location}
              description=""
              actions={actions}
              isBusy={Boolean(pendingActionKey)}
            />
          )
        })}
      </div>
    )
  }

  const renderRolesTab = () => {
    if (sortedParticipants.length === 0) {
      return <p className="gray-elem">Нет участников для управления ролями.</p>
    }

    return (
      <>
        {!canManageRoles ? (
          <p className="gray-elem">
            Назначение администраторов и передача ownership доступны только создателю комнаты.
          </p>
        ) : null}

        <div className="room-governance-list">
          {sortedParticipants.map((participant) => {
            const role = normalizeMembershipRole(participant?.role)
            const userId = Number(participant?.id)
            const isSelf = Number.isFinite(userId) && Number(userId) === Number(currentUserId)
            const location = getUserLocation(participant)
            const actions = [
              canManageRoles && role === 'PARTICIPANT' && !isSelf && Number.isFinite(userId)
                ? {
                    key: `promote-${userId}`,
                    label: 'Назначить админом',
                    pendingLabel: 'Назначение...',
                    isCurrent: pendingActionKey === `promote-${userId}`,
                    onClick: () => runGovernanceAction({
                      actionKey: `promote-${userId}`,
                      confirmMessage: `Назначить пользователя «${getGovernanceUserName(participant)}» администратором активности «${roomName || 'Без названия'}»?`,
                      action: () => assignRoomAdmin(roomId, userId),
                      successMessage: 'Роль администратора назначена.',
                      errorMessage: 'Не удалось назначить администратора.',
                    }),
                  }
                : null,
              canManageRoles && role === 'ADMIN' && !isSelf && Number.isFinite(userId)
                ? {
                    key: `demote-${userId}`,
                    label: 'Снять админа',
                    pendingLabel: 'Снятие...',
                    tone: 'danger',
                    isCurrent: pendingActionKey === `demote-${userId}`,
                    onClick: () => runGovernanceAction({
                      actionKey: `demote-${userId}`,
                      confirmMessage: `Снять роль администратора с пользователя «${getGovernanceUserName(participant)}»?`,
                      action: () => demoteRoomAdmin(roomId, userId),
                      successMessage: 'Роль администратора снята.',
                      errorMessage: 'Не удалось снять роль администратора.',
                    }),
                  }
                : null,
              canManageRoles && role !== 'OWNER' && !isSelf && Number.isFinite(userId)
                ? {
                    key: `transfer-${userId}`,
                    label: 'Передать ownership',
                    pendingLabel: 'Передача...',
                    tone: 'danger',
                    isCurrent: pendingActionKey === `transfer-${userId}`,
                    onClick: () => runGovernanceAction({
                      actionKey: `transfer-${userId}`,
                      confirmMessage: `Передать ownership активности «${roomName || 'Без названия'}» пользователю «${getGovernanceUserName(participant)}»?`,
                      action: () => transferRoomOwnership(roomId, userId),
                      successMessage: 'Ownership передан.',
                      errorMessage: 'Не удалось передать ownership.',
                    }),
                  }
                : null,
            ].filter(Boolean)

            return (
              <GovernanceUserCard
                key={`role-${participant?.id ?? `${participant?.name}-${participant?.role}`}`}
                user={participant}
                roleLabel={getRoleLabel(role)}
                subtitle={location || (isSelf ? 'Это вы' : '')}
                description=""
                actions={actions}
                isBusy={Boolean(pendingActionKey)}
              />
            )
          })}
        </div>
      </>
    )
  }

  return (
    <section className="room-panel room-panel-soft room-governance-section" aria-labelledby="room-management-heading">
      <div className="room-governance-header">
        <div>
          <h2 id="room-management-heading" className="room-section-heading">
            Управление
          </h2>
          <p className="room-governance-copy">
            Списки и роли обновляются после каждого действия. Доступ зависит от вашей роли в комнате.
          </p>
        </div>
      </div>

      <div className="room-governance-tabs" role="tablist" aria-label="Разделы управления комнатой">
        {MANAGEMENT_TABS.map((tab) => (
          <button
            key={tab.id}
            id={`room-management-tab-${tab.id}`}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-controls={`room-management-panel-${tab.id}`}
            className={
              activeTab === tab.id
                ? 'room-governance-tab room-governance-tab--active'
                : 'room-governance-tab'
            }
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {feedback ? (
        <p
          className={
            feedbackTone === 'error'
              ? 'room-governance-feedback room-governance-feedback--error'
              : 'room-governance-feedback room-governance-feedback--success'
          }
          role="status"
        >
          {feedback}
        </p>
      ) : null}

      {errorMessage ? (
        <p className="room-activity-error" role="alert">
          {errorMessage}
        </p>
      ) : null}

      {isLoading ? <p className="gray-elem">Загрузка управления...</p> : null}

      {!isLoading && !errorMessage ? (
        <div
          id={`room-management-panel-${activeTab}`}
          role="tabpanel"
          aria-labelledby={`room-management-tab-${activeTab}`}
          className="room-governance-panel"
        >
          {activeTab === 'participants' ? renderParticipantsTab() : null}
          {activeTab === 'bans' ? renderBansTab() : null}
          {activeTab === 'roles' ? renderRolesTab() : null}
        </div>
      ) : null}
    </section>
  )
}

export default RoomManagementSection
