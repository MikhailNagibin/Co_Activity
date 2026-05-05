import { useLayoutEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { describeFetchFailure, isApiError } from '../api/httpClient.js'
import { useAuthSession } from '../auth/authSessionContext.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import { login } from '../services/authService.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'

function readAuthNoticeFromUrl() {
  try {
    const params = new URLSearchParams(window.location.search)
    if (params.get('reauth') === 'password-changed') {
      return 'password-changed'
    }
    if (params.get('session') === 'expired') {
      return 'expired'
    }
    return null
  } catch {
    return null
  }
}

function isEmailNotVerifiedError(error) {
  return (
    isApiError(error) &&
    (error.code === 'EMAIL_NOT_VERIFIED' ||
      /email is not verified/i.test(String(error.message ?? '')))
  )
}

function SignIn() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { markAuthenticated } = useAuthSession()
  const [authNotice] = useState(readAuthNoticeFromUrl)
  const [formData, setFormData] = useState({
    email: searchParams.get('email')?.trim() ?? '',
    password: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})

  useLayoutEffect(() => {
    if (!authNotice) {
      return
    }
    const sp = new URLSearchParams(window.location.search)
    const hasPasswordChangedNotice = sp.get('reauth') === 'password-changed'
    const hasExpiredNotice = sp.get('session') === 'expired'
    if (!hasPasswordChangedNotice && !hasExpiredNotice) {
      return
    }
    sp.delete('session')
    sp.delete('reauth')
    const next = sp.get('next')
    const email = sp.get('email')
    const cleaned = new URLSearchParams()
    if (next) {
      cleaned.set('next', next)
    }
    if (email) {
      cleaned.set('email', email)
    }
    const q = cleaned.toString()
    navigate({ pathname: '/sign-in', search: q ? `?${q}` : '' }, { replace: true })
  }, [authNotice, navigate])

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setFieldErrors((prev) => (prev[name] ? { ...prev, [name]: '' } : prev))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setFieldErrors({})

    const email = formData.email.trim()
    if (!email) {
      setFieldErrors({ email: 'Укажите почту' })
      setErrorMessage('Исправьте подсвеченные поля и попробуйте снова.')
      return
    }
    if (formData.password.length < 8) {
      setFieldErrors({ password: 'Пароль: от 8 символов' })
      setErrorMessage('Исправьте подсвеченные поля и попробуйте снова.')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = await login({ email, password: formData.password })
      markAuthenticated(payload)
      const next = searchParams.get('next')
      const safeNext =
        next && next.startsWith('/') && !next.startsWith('//') ? next : '/main'
      navigate(safeNext)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        const next = searchParams.get('next')
        redirectToSignInForExpiredSession(navigate, {
          next: next && next.startsWith('/') && !next.startsWith('//') ? next : '/main',
        })
        return
      }
      if (isEmailNotVerifiedError(error)) {
        const next = searchParams.get('next')
        const params = new URLSearchParams()
        params.set('step', 'verify')
        params.set('email', email)
        if (next && next.startsWith('/') && !next.startsWith('//')) {
          params.set('next', next)
        }
        navigate({ pathname: '/sign-up', search: `?${params.toString()}` }, { replace: true })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось войти. Попробуйте снова.'))
      } else {
        setErrorMessage(describeFetchFailure(error))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleOpenPasswordReset = () => {
    const params = new URLSearchParams()
    const email = formData.email.trim()
    if (email) {
      params.set('email', email)
    }
    navigate({
      pathname: '/password-reset',
      search: params.toString() ? `?${params.toString()}` : '',
    })
  }

  return (
    <AuthLayout
      title="Вход"
      subtitle=""
      authActionLabel="Войти"
      authActionTo="/sign-in"
      simpleTitle="Вход"
      footer={
        <div className="auth-card__footer-stack">
          <p className="auth-card__footer-text">
            У вас нет аккаунта? <Link to="/sign-up">Зарегистрироваться</Link>
          </p>
          <p className="auth-card__footer-text">
            <Link to="/main">Продолжить без входа</Link>
          </p>
        </div>
      }
    >
      {authNotice === 'expired' ? (
        <div className="auth-banner auth-banner--info" role="status">
          Сессия истекла. Требуется заново войти в аккаунт
        </div>
      ) : null}
      {authNotice === 'password-changed' ? (
        <div className="auth-banner auth-banner--info" role="status">
          Пароль изменён. Для безопасности войдите снова с новым паролем.
        </div>
      ) : null}
      <form onSubmit={handleSubmit} className="auth-form">
        <AuthField
          label="Почта"
          name="email"
          placeholder="Введите почту"
          value={formData.email}
          onChange={handleFieldChange}
          autoComplete="email"
          disabled={isSubmitting}
          error={fieldErrors.email}
        />
        <AuthField
          label="Пароль"
          type="password"
          name="password"
          placeholder="Введите пароль"
          value={formData.password}
          onChange={handleFieldChange}
          autoComplete="current-password"
          disabled={isSubmitting}
          error={fieldErrors.password}
        />
        {errorMessage ? <p className="auth-banner auth-banner--error">{errorMessage}</p> : null}
        <button
          type="button"
          className="auth-text-button"
          disabled={isSubmitting}
          onClick={handleOpenPasswordReset}
        >
          Забыли пароль?
        </button>
        <button type="submit" className="auth-submit-button" disabled={isSubmitting}>
          {isSubmitting ? 'Вход...' : 'Войти'}
        </button>
      </form>
    </AuthLayout>
  )
}

export default SignIn
