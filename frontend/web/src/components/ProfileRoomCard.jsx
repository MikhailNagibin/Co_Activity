import { Link } from 'react-router-dom'

function ProfileRoomCard({
  item,
  eyebrow = '',
  statusLabel = '',
  statusTone = 'neutral',
  actions = [],
}) {
  return (
    <article className="profile-room-card">
      <div className="profile-room-card__header">
        <div className="profile-room-card__title-group">
          {eyebrow ? <p className="profile-room-card__eyebrow">{eyebrow}</p> : null}
          {item.linkTo ? (
            <Link className="profile-room-card__title" to={item.linkTo}>
              {item.title}
            </Link>
          ) : (
            <h2 className="profile-room-card__title">{item.title}</h2>
          )}
        </div>
        {statusLabel ? (
          <span className={`profile-room-card__status profile-room-card__status--${statusTone}`}>
            {statusLabel}
          </span>
        ) : null}
      </div>

      <p className="profile-room-card__description">{item.description}</p>

      <div className="profile-room-card__meta">
        <span>{item.category}</span>
        <span>{item.date}</span>
        <span>{item.capacity}</span>
        <span>Организатор: {item.author}</span>
      </div>

      <div className="profile-room-card__badges">
        {item.membershipRole ? (
          <span className="profile-room-card__badge">
            Роль: {item.membershipRole === 'OWNER' ? 'создатель' : item.membershipRole === 'ADMIN' ? 'админ' : 'участник'}
          </span>
        ) : null}
        <span className="profile-room-card__badge">
          {item.isPublic ? 'Публичная' : 'Приватная'}
        </span>
        {item.roomStatus ? <span className="profile-room-card__badge">Статус: {item.roomStatus}</span> : null}
      </div>

      {actions.length > 0 ? (
        <div className="profile-room-card__actions">
          {actions.map((action) => (
            <button
              key={action.key}
              type="button"
              className={`profile-room-card__action profile-room-card__action--${action.variant ?? 'secondary'}`}
              onClick={action.onClick}
              disabled={action.disabled}
            >
              {action.label}
            </button>
          ))}
        </div>
      ) : null}
    </article>
  )
}

export default ProfileRoomCard
