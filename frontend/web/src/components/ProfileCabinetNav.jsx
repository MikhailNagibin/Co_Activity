import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/profile', end: true, label: 'Профиль' },
  { to: '/profile/notifications', label: 'Настройка уведомлений' },
  { to: '/profile/site-settings', label: 'Настройки сайта' },
  { to: '/profile/my-rooms', label: 'Мои активности' },
  { to: '/profile/incoming-requests', label: 'Входящие заявки' },
  { to: '/profile/sent-requests', label: 'Отправленные заявки' },
  { to: '/profile/banned-rooms', label: 'Забанен в комнатах' },
]

export default function ProfileCabinetNav() {
  return (
    <nav className="profile-cabinet-nav" aria-label="Разделы личного кабинета">
      <p className="profile-cabinet-nav__title">Разделы</p>
      <ul className="profile-cabinet-nav__list">
        {NAV_ITEMS.map(({ to, end, label }) => (
          <li key={to}>
            <NavLink
              to={to}
              end={Boolean(end)}
              className={({ isActive }) =>
                isActive
                  ? 'profile-cabinet-nav__link profile-cabinet-nav__link--active'
                  : 'profile-cabinet-nav__link'
              }
            >
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
