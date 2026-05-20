import { useNavigate } from 'react-router-dom'
import AppHeader from './AppHeader.jsx'
import ProfileAuthRail from './ProfileAuthRail.jsx'
import ProfileCabinetNav from './ProfileCabinetNav.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { logout } from '../services/profileService.js'

/**
 * Общая оболочка личного кабинета: шапка, сетка с левым блоком «Разделы», колонка выхода.
 */
export default function ProfileCabinetShell({
  heroTitle,
  heroSubtitle = null,
  children,
  sessionEnded = false,
  onLogout,
  showCabinetNav = true,
}) {
  const navigate = useNavigate()
  const { isAuthenticated, clearSession } = useAuthSession()

  const handleLogout = async () => {
    if (onLogout) {
      await onLogout()
      return
    }
    try {
      await logout()
    } catch {
      // Even if backend logout fails, local session state should be reset.
    } finally {
      clearSession()
      navigate('/sign-in')
    }
  }

  const subtitleNode =
    heroSubtitle != null && heroSubtitle !== '' ? (
      typeof heroSubtitle === 'string' ? (
        <h3 className="gray-elem">{heroSubtitle}</h3>
      ) : (
        heroSubtitle
      )
    ) : null

  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-shell">
        <div className="profile-shell__column">
          <section className="main-hero">
            <h2>{heroTitle}</h2>
            {subtitleNode}
          </section>
          {showCabinetNav ? (
            <div className="profile-cabinet-layout">
              <ProfileCabinetNav />
              <div className="profile-cabinet-main">{children}</div>
            </div>
          ) : (
            children
          )}
        </div>
        <ProfileAuthRail
          hasToken={isAuthenticated}
          onLogout={handleLogout}
          sessionEnded={sessionEnded}
        />
      </div>
    </>
  )
}
