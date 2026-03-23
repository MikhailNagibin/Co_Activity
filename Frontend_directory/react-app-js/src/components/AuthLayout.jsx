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
      <div className="virtual-elem"></div>
      <fieldset className="sign-field">
        <h2>{title}</h2>
        <h4 className="gray-elem">{subtitle}</h4>
        <br />
        <hr />
        <br />
        <section>{children}</section>
        {footer ? (
          <>
            <hr />
            <br />
            {footer}
          </>
        ) : null}
      </fieldset>
    </>
  )
}

export default AuthLayout
