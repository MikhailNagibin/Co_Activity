import AppHeader from './AppHeader.jsx'

function AuthLayout({
  title,
  subtitle,
  authActionLabel,
  authActionTo,
  children,
  footer = null,
  hideHeader = false,
  hideHero = false,
  simpleTitle = '',
}) {
  const shellClassName = [
    'main-page-shell',
    'auth-page-shell',
    hideHeader ? 'auth-page-shell--simple' : '',
    !hideHeader && hideHero ? 'auth-page-shell--center-card' : '',
  ]
    .filter(Boolean)
    .join(' ')
  const shouldShowHero = !hideHeader && !hideHero

  return (
    <>
      {hideHeader ? null : <AppHeader authActionLabel={authActionLabel} authActionTo={authActionTo} />}
      <div className={shellClassName}>
        {hideHeader ? (
          <header className="auth-simple-header">
            <h1>{simpleTitle || title}</h1>
          </header>
        ) : shouldShowHero ? (
          <section className="main-hero auth-page-hero">
            <p className="auth-page-hero__eyebrow">CoActivity</p>
            <h2>{title}</h2>
            <h3>{subtitle}</h3>
          </section>
        ) : null}

        <main className="auth-layout" aria-label={title}>
          <section className="auth-card">
            <section className="auth-card__content">{children}</section>
            {footer ? <footer className="auth-card__footer">{footer}</footer> : null}
          </section>
        </main>
      </div>
    </>
  )
}

export default AuthLayout
