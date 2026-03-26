import { Link } from 'react-router-dom'
import { getAccessToken } from '../api/tokenStorage.js'

function AppHeader({
  authActionLabel = 'Войти',
  authActionTo = '/sign-in',
  activeTab = null,
}) {
  const hasToken = Boolean(getAccessToken())
  const activityClass = activeTab === 'main' ? 'nav-link-active' : 'nav-link-muted'
  const qaClass = activeTab === 'qa' ? 'nav-link-active' : 'nav-link-muted'
  const actionLabel = hasToken ? 'Профиль' : authActionLabel
  const actionTo = hasToken ? '/profile' : authActionTo

  return (
    <header id="main-header">
      <h1>CoActivity</h1>
      <h4 className="gray-elem">
        <Link className={activityClass} to="/main">
          Активности
        </Link>
      </h4>
      <h4 className="gray-elem">
        <Link className={qaClass} to="/qa">
          Вопросы-ответы
        </Link>
      </h4>
      <i className="fa-regular fa-moon" aria-hidden="true"></i>
      <button type="button" className="header-auth-button">
        <Link className="header-auth-link" to={actionTo}>
          {actionLabel}
        </Link>
      </button>
    </header>
  )
}

export default AppHeader
