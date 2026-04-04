import { Link } from 'react-router-dom'
import { getAccessToken } from '../api/tokenStorage.js'

function AppHeader({
  authActionLabel = 'Войти',
  authActionTo = '/sign-in',
  activeTab = null,
}) {
  const hasToken = Boolean(getAccessToken())
  const actionLabel = hasToken ? 'Профиль' : authActionLabel
  const actionTo = hasToken ? '/profile' : authActionTo

  return (
    <header id="main-header" className="app-header">
      <div className="app-header-inner">
        <Link className="app-header-brand" to="/main">
          CoActivity
        </Link>

        <nav className="app-header-nav" aria-label="Основная навигация">
          <Link
            className={activeTab === 'main' ? 'app-header-nav-link app-header-nav-link--active' : 'app-header-nav-link'}
            to="/main"
          >
            Активности
          </Link>
          <Link
            className={activeTab === 'qa' ? 'app-header-nav-link app-header-nav-link--active' : 'app-header-nav-link'}
            to="/qa"
          >
            Вопросы-ответы
          </Link>
        </nav>

        <div className="app-header-actions">
          <button type="button" className="app-header-theme-btn" aria-label="Тема оформления">
            <i className="fa-regular fa-moon" aria-hidden="true"></i>
          </button>
          <Link className="app-header-auth" to={actionTo}>
            {actionLabel}
          </Link>
        </div>
      </div>
    </header>
  )
}

export default AppHeader
