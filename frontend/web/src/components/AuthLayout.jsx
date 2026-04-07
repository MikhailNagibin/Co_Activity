import AppHeader from './AppHeader.jsx'

function AuthLayout({
  title,
  subtitle,
  authActionLabel,
  authActionTo,
  children,
  footer = null,
}) {
  return (
    <>
      <AppHeader authActionLabel={authActionLabel} authActionTo={authActionTo} />
      <div className="main-page-shell auth-page-shell">
        <section className="main-hero auth-page-hero">
          <p className="auth-page-hero__eyebrow">CoActivity</p>
          <h2>{title}</h2>
          <h3>{subtitle}</h3>
        </section>

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
