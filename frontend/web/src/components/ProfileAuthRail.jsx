export default function ProfileAuthRail({ hasToken, onLogout, sessionEnded = false }) {
  return (
    <aside className="profile-auth-rail" aria-label="Выход из аккаунта">
      <div
        className={
          sessionEnded
            ? 'profile-auth-rail__card profile-auth-rail__card--session-ended'
            : 'profile-auth-rail__card'
        }
      >
        <p className="profile-auth-rail__label">{sessionEnded ? 'Сессия' : 'Аккаунт'}</p>
        {sessionEnded ? (
          <p className="profile-auth-rail__hint">Сначала нажмите «Выйти», затем войдите снова.</p>
        ) : null}
        <button
          type="button"
          className="profile-auth-btn profile-auth-btn--logout"
          onClick={onLogout}
          disabled={!hasToken}
        >
          Выйти
        </button>
      </div>
    </aside>
  )
}
