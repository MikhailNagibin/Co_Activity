import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { isApiError } from '../api/httpClient.js'
import {
  assignRoomAdmin,
  banRoomUser,
  demoteRoomAdmin,
  getRoomBans,
  getRoomParticipants,
  inviteUserToRoom,
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
import { useConfirmDialog } from '../hooks/useConfirmDialog.jsx'

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
  const { requestConfirm, confirmDialog } = useConfirmDialog()
  const [activeTab, setActiveTab] = useState('participants')
  const [participants, setParticipants] = useState([])
  const [bans, setBans] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [feedbackTone, setFeedbackTone] = useState('success')
  const [pendingActionKey, setPendingActionKey] = useState('')
  const [inviteUserIdRaw, setInviteUserIdRaw] = useState('')

  const normalizedCurrentRole = normalizeMembershipRole(currentUserRole)
  const canModerate = normalizedCurrentRole === 'OWNER' || normalizedCurrentRole === 'ADMIN'
  const canManageRoles = normalizedCurrentRole === 'OWNER'

  const managementTabs = useMemo(
    () =>
      canManageRoles
        ? [...MANAGEMENT_TABS, { id: 'invites', label: 'Пригласить' }]
        : MANAGEMENT_TABS,
    [canManageRoles],
  )

  const loadGovernanceData = useCallback(async (options = {}) => {
    const { silent = false } = options

    if (!canModerate) {
      setParticipants([])
      setBans([])
      setErrorMessage('')
      setFeedback('')
      setIsLoading(false)
      return false
    }

    if (!silent) {
      setIsLoading(true)
      setErrorMessage('')
    }
    setFeedback('')

    try {
      const [participantsPayload, bansPayload] = await Promise.all([
        getRoomParticipants(roomId),
        getRoomBans(roomId),
      ])
      setParticipants(Array.isArray(participantsPayload) ? participantsPayload : [])
      setBans(Array.isArray(bansPayload) ? bansPayload : [])
      if (silent) {
        setErrorMessage('')
      }
      return true
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, {
          next: `/rooms/${encodeURIComponent(String(roomId))}`,
        })
        return false
      }

      if (!silent) {
        setParticipants([])
        setBans([])
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось загрузить управление активностью.'))
      } else {
        setErrorMessage('Не удалось загрузить управление активностью.')
      }
      return false
    } finally {
      if (!silent) {
        setIsLoading(false)
      }
    }
  }, [canModerate, navigate, roomId])

  useEffect(() => {
    loadGovernanceData()
  }, [loadGovernanceData])

  useEffect(() => {
    if (!canManageRoles && activeTab === 'invites') {
      setActiveTab('participants')
    }
  }, [activeTab, canManageRoles])

  const sortedParticipants = useMemo(
    () => [...participants].sort(compareUsers),
    [participants],
  )
  const sortedBans = useMemo(() => [...bans].sort(compareNames), [bans])

  const runGovernanceAction = useCallback(async ({
    actionKey,
    confirmMessage,
    action,
    errorMessage: fallbackMessage,
  }) => {
    if (confirmMessage) {
      const ok = await requestConfirm({
        title: 'Подтверждение',
        message: confirmMessage,
        confirmLabel: 'Продолжить',
        variant: 'danger',
      })
      if (!ok) {
        return
      }
    }

    setPendingActionKey(actionKey)
    setFeedback('')
    setFeedbackTone('success')

    try {
      await action()

      if (typeof onRefreshRoom === 'function') {
        await onRefreshRoom({ silent: true })
      }
      await loadGovernanceData({ silent: true })

      setFeedback('')
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
  }, [loadGovernanceData, navigate, onRefreshRoom, requestConfirm, roomId])

  const handleInviteSubmit = useCallback(
    async (event) => {
      event.preventDefault()

      const trimmed = inviteUserIdRaw.trim()
      const targetUserId = Number.parseInt(trimmed, 10)
      if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
        setFeedback('Укажите корректный числовой ID пользователя (как в адресе профиля /users/…).')
        setFeedbackTone('error')
        return
      }
      if (Number(currentUserId) === targetUserId) {
        setFeedback('Нельзя отправить приглашение самому себе.')
        setFeedbackTone('error')
        return
      }

      setPendingActionKey('invite')
      setFeedback('')
      setFeedbackTone('success')

      try {
        await inviteUserToRoom(roomId, targetUserId)
        setFeedback('')
        setFeedbackTone('success')
        setInviteUserIdRaw('')
      } catch (error) {
        if (isUnauthorizedApiError(error)) {
          redirectToSignInForExpiredSession(navigate, {
            next: `/rooms/${encodeURIComponent(String(roomId))}`,
          })
          return
        }
        if (isApiError(error)) {
          setFeedback(getUserFacingApiMessage(error, 'Не удалось отправить приглашение.'))
        } else {
          setFeedback('Не удалось отправить приглашение.')
        }
        setFeedbackTone('error')
      } finally {
        setPendingActionKey('')
      }
    },
    [currentUserId, inviteUserIdRaw, navigate, roomId],
  )

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
            Назначение администраторов и передача роли создателя доступны только владельцу активности.
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
                      errorMessage: 'Не удалось снять роль администратора.',
                    }),
                  }
                : null,
              canManageRoles && role !== 'OWNER' && !isSelf && Number.isFinite(userId)
                ? {
                    key: `transfer-${userId}`,
                    label: 'Передать роль создателя',
                    pendingLabel: 'Передача...',
                    tone: 'danger',
                    isCurrent: pendingActionKey === `transfer-${userId}`,
                    onClick: () => runGovernanceAction({
                      actionKey: `transfer-${userId}`,
                      confirmMessage: `Передать роль создателя активности «${roomName || 'Без названия'}» пользователю «${getGovernanceUserName(participant)}»?`,
                      action: () => transferRoomOwnership(roomId, userId),
                      errorMessage: 'Не удалось передать роль создателя.',
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

  const renderInvitesTab = () => (
    <div className="room-invite-panel">
      <p className="gray-elem">
        Укажите ID пользователя (его можно посмотреть в профиле по ссылке «/users/…»). На почту придёт
        письмо с названием активности и ссылкой. Для{' '}
        <strong>приватной</strong> активности приглашённый сможет вступить сразу после входа в аккаунт;
        для публичной письмо служит уведомлением.
      </p>
      <form className="room-invite-form" onSubmit={handleInviteSubmit}>
        <div className="room-invite-form__row">
          <label htmlFor="room-invite-user-id">ID пользователя</label>
          <input
            id="room-invite-user-id"
            name="inviteUserId"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            className="room-invite-form__input"
            value={inviteUserIdRaw}
            onChange={(e) => setInviteUserIdRaw(e.target.value)}
            placeholder="Например, 42"
            disabled={pendingActionKey === 'invite'}
          />
        </div>
        <button
          type="submit"
          className="room-governance-action"
          disabled={Boolean(pendingActionKey)}
        >
          {pendingActionKey === 'invite' ? 'Отправка…' : 'Отправить приглашение'}
        </button>
      </form>
    </div>
  )

  return (
    <>
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
        {managementTabs.map((tab) => (
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

      {feedback && feedbackTone === 'error' ? (
        <p className="room-governance-feedback room-governance-feedback--error" role="alert">
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
          {activeTab === 'invites' ? renderInvitesTab() : null}
        </div>
      ) : null}
    </section>
    {confirmDialog}
    </>
  )
}

export default RoomManagementSection
