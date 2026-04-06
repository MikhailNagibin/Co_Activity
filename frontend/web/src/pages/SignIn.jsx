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

function readSessionExpiredFromUrl() {
  try {
    return new URLSearchParams(window.location.search).get('session') === 'expired'
  } catch {
    return false
  }
}

function SignIn() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { markAuthenticated } = useAuthSession()
  const [sessionExpiredBanner] = useState(readSessionExpiredFromUrl)
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useLayoutEffect(() => {
    if (!sessionExpiredBanner) {
      return
    }
    const sp = new URLSearchParams(window.location.search)
    if (sp.get('session') !== 'expired') {
      return
    }
    sp.delete('session')
    const next = sp.get('next')
    const cleaned = new URLSearchParams()
    if (next) {
      cleaned.set('next', next)
    }
    const q = cleaned.toString()
    navigate({ pathname: '/sign-in', search: q ? `?${q}` : '' }, { replace: true })
  }, [sessionExpiredBanner, navigate])

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    const email = formData.email.trim()
    if (!email) {
      setErrorMessage('Укажите почту')
      return
    }
    if (formData.password.length < 8) {
      setErrorMessage('Пароль: от 8 символов')
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
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось войти. Попробуйте снова.'))
      } else {
        setErrorMessage(describeFetchFailure(error))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Войти в CoActivity"
      subtitle="Введите почту и пароль"
      authActionLabel="Войти"
      authActionTo="/sign-in"
      footer={
        <h3>
          У вас нет аккаунта? <Link to="/sign-up">Зарегистрироваться</Link>
        </h3>
      }
    >
      {sessionExpiredBanner ? (
        <div className="sign-in-session-notice" role="status">
          Сессия истекла. Требуется заново войти в аккаунт
        </div>
      ) : null}
      <form onSubmit={handleSubmit}>
        <AuthField
          label="Почта"
          name="email"
          placeholder="Введите почту"
          value={formData.email}
          onChange={handleFieldChange}
          autoComplete="email"
          disabled={isSubmitting}
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
          inlineRight={
            <em>
              <a className="gray-elem" href="#">
                Забыли пароль?
              </a>
            </em>
          }
        />
        {errorMessage ? (
          <p style={{ color: '#b00020', marginTop: '12px', marginBottom: 0 }}>{errorMessage}</p>
        ) : null}
        <button
          type="submit"
          style={{ backgroundColor: 'black', color: 'white', padding: '12px', marginTop: '20px' }}
          className="enter-button"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Вход...' : 'Войти'}
        </button>
      </form>
    </AuthLayout>
  )
}

export default SignIn
