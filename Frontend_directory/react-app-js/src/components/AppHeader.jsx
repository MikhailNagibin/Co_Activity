import { Link } from 'react-router-dom'

function AppHeader({
  authActionLabel = 'Войти',
  authActionTo = '/sign-in',
  activeTab = null,
}) {
  const activityClass = activeTab === 'main' ? 'nav-link-active' : 'nav-link-muted'
  const qaClass = activeTab === 'qa' ? 'nav-link-active' : 'nav-link-muted'

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
        <Link className="header-auth-link" to={authActionTo}>
          {authActionLabel}
        </Link>
      </button>
    </header>
  )
}

export default AppHeader
