import { Link } from 'react-router-dom'
import { useAuthSession } from '../auth/authSessionContext.js'
import { useTheme } from '../theme/useTheme.js'

function AppHeader({
  authActionLabel = 'Войти',
  authActionTo = '/sign-in',
  activeTab = null,
}) {
  const { theme, toggleTheme } = useTheme()
  const { isAuthenticated } = useAuthSession()
  const actionLabel = isAuthenticated ? 'Профиль' : authActionLabel
  const actionTo = isAuthenticated ? '/profile' : authActionTo

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
          <button
            type="button"
            className="app-header-theme-btn"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему'}
            title={theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}
          >
            <i
              className={theme === 'dark' ? 'fa-solid fa-sun' : 'fa-regular fa-moon'}
              aria-hidden="true"
            ></i>
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
