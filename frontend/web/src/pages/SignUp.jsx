import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import {
  register,
  resendRegistrationVerificationCode,
  verifyRegistration,
} from '../services/authService.js'

function birthDateInputToInstant(isoDate) {
  if (!isoDate || !/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) {
    return null
  }
  return `${isoDate}T00:00:00Z`
}

function appendSafeNextParam(searchParams, params) {
  const next = searchParams.get('next')
  if (next && next.startsWith('/') && !next.startsWith('//')) {
    params.set('next', next)
  }
}

function SignUp() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { isAuthenticated } = useAuthSession()
  const [formData, setFormData] = useState({
    email: '',
    nickname: '',
    password: '',
    passwordAgain: '',
    birthDate: '',
    country: '',
    city: '',
    about: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [pendingEmail, setPendingEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [step, setStep] = useState('register')
  const [isResendingCode, setIsResendingCode] = useState(false)
  const [resendCooldownSeconds, setResendCooldownSeconds] = useState(0)

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/main', { replace: true })
    }
  }, [isAuthenticated, navigate])

  useEffect(() => {
    const requestedStep = searchParams.get('step')
    const emailFromUrl = searchParams.get('email')?.trim() ?? ''

    if (requestedStep === 'verify' && emailFromUrl) {
      setFormData((prev) =>
        prev.email === emailFromUrl ? prev : { ...prev, email: emailFromUrl },
      )
      setPendingEmail(emailFromUrl)
      setStep('verify')
      setErrorMessage('')
      setSuccessMessage('Аккаунт ещё не подтверждён. Введите код из письма, чтобы завершить регистрацию.')
      setResendCooldownSeconds(0)
    }
  }, [searchParams])

  useEffect(() => {
    if (resendCooldownSeconds <= 0) {
      return undefined
    }
    const timerId = window.setTimeout(() => {
      setResendCooldownSeconds((prev) => Math.max(0, prev - 1))
    }, 1000)
    return () => window.clearTimeout(timerId)
  }, [resendCooldownSeconds])

  const openVerifyStep = (email, message, cooldownSeconds = 0) => {
    const params = new URLSearchParams()
    params.set('step', 'verify')
    params.set('email', email)
    appendSafeNextParam(searchParams, params)
    setFormData((prev) => ({ ...prev, email }))
    setPendingEmail(email)
    setVerificationCode('')
    setStep('verify')
    setErrorMessage('')
    setSuccessMessage(message)
    setResendCooldownSeconds(cooldownSeconds)
    navigate({ pathname: '/sign-up', search: `?${params.toString()}` }, { replace: true })
  }

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleRegister = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')

    if (formData.password !== formData.passwordAgain) {
      setErrorMessage('Пароли не совпадают')
      return
    }

    const email = formData.email.trim()
    const userName = formData.nickname.trim()

    if (!email) {
      setErrorMessage('Укажите почту')
      return
    }
    if (userName.length < 2 || userName.length > 20) {
      setErrorMessage('Имя пользователя: от 2 до 20 символов')
      return
    }
    if (formData.password.length < 8 || formData.password.length > 128) {
      setErrorMessage('Пароль: от 8 до 128 символов')
      return
    }

    const dateOfBirth = birthDateInputToInstant(formData.birthDate)
    if (!dateOfBirth) {
      setErrorMessage('Укажите дату рождения')
      return
    }

    const payload = {
      email,
      userName,
      password: formData.password,
      dateOfBirth,
      city: formData.city.trim() || undefined,
      country: formData.country.trim() || undefined,
      description: formData.about.trim() || undefined,
    }

    setIsSubmitting(true)
    try {
      await register(payload)
      openVerifyStep(email, 'Аккаунт создан. Введите код из письма, чтобы активировать его.', 60)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/sign-up' })
        return
      }
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось создать аккаунт. Попробуйте снова.'))
      } else {
        setErrorMessage('Не удалось создать аккаунт. Попробуйте снова.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleVerify = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')

    const code = verificationCode.trim()
    if (!code) {
      setErrorMessage('Введите код из письма')
      return
    }

    setIsSubmitting(true)
    try {
      await verifyRegistration({ email: pendingEmail, code })
      const next = searchParams.get('next')
      const params = new URLSearchParams()
      if (next && next.startsWith('/') && !next.startsWith('//')) {
        params.set('next', next)
      }
      navigate(
        {
          pathname: '/sign-in',
          search: params.toString() ? `?${params.toString()}` : '',
        },
        { replace: true },
      )
    } catch (error) {
      if (isApiError(error)) {
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось подтвердить почту.'))
      } else {
        setErrorMessage('Не удалось подтвердить почту.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleResendCode = async () => {
    if (!pendingEmail || resendCooldownSeconds > 0) {
      return
    }

    setIsResendingCode(true)
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await resendRegistrationVerificationCode({ email: pendingEmail })
      setVerificationCode('')
      setSuccessMessage('Новый код отправлен на почту. Проверьте входящие и спам.')
      setResendCooldownSeconds(60)
    } catch (error) {
      if (isApiError(error)) {
        if (
          error.code === 'REGISTRATION_CODE_RESEND_COOLDOWN' ||
          (error.status === 429 && resendCooldownSeconds <= 0)
        ) {
          setResendCooldownSeconds(60)
        }
        setErrorMessage(getUserFacingApiMessage(error, 'Не удалось отправить новый код.'))
      } else {
        setErrorMessage('Не удалось отправить новый код.')
      }
    } finally {
      setIsResendingCode(false)
    }
  }

  return (
    <AuthLayout
      title="Создать аккаунт в CoActivity"
      subtitle={
        step === 'register'
          ? 'Заполните профиль и начните искать активности в едином пространстве'
          : 'Подтвердите почту кодом из письма'
      }
      authActionLabel="Регистрация"
      authActionTo="/sign-in"
      footer={
        <p className="auth-card__footer-text">
          Уже есть аккаунт? <Link to="/sign-in">Войти</Link>
        </p>
      }
    >
      {step === 'register' ? (
        <form onSubmit={handleRegister} className="auth-form">
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
            label="Имя пользователя"
            name="nickname"
            placeholder="Введите имя"
            value={formData.nickname}
            onChange={handleFieldChange}
            autoComplete="username"
            disabled={isSubmitting}
          />
          <AuthField
            label="Пароль"
            type="password"
            name="password"
            placeholder="Введите пароль"
            value={formData.password}
            onChange={handleFieldChange}
            autoComplete="new-password"
            disabled={isSubmitting}
          />
          <AuthField
            label="Повтор пароля"
            type="password"
            name="passwordAgain"
            placeholder="Введите пароль еще раз"
            value={formData.passwordAgain}
            onChange={handleFieldChange}
            autoComplete="new-password"
            disabled={isSubmitting}
          />
          <AuthField
            label="Дата рождения"
            type="date"
            name="birthDate"
            value={formData.birthDate}
            onChange={handleFieldChange}
            disabled={isSubmitting}
          />

          <div className="auth-grid auth-grid--two">
            <AuthField
              label="Страна"
              name="country"
              placeholder="Например, Россия"
              value={formData.country}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            />
            <AuthField
              label="Город"
              name="city"
              placeholder="Например, Москва"
              value={formData.city}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            />
          </div>

          <div className="auth-field">
            <label htmlFor="about" className="auth-field__label">
              О себе
            </label>
            <textarea
              id="about"
              className="auth-field__textarea"
              name="about"
              rows="5"
              value={formData.about}
              onChange={handleFieldChange}
              disabled={isSubmitting}
              placeholder="Расскажите коротко о своих интересах"
            />
          </div>

          {errorMessage ? <p className="auth-banner auth-banner--error">{errorMessage}</p> : null}
          {successMessage ? <p className="auth-banner auth-banner--success">{successMessage}</p> : null}

          <button type="submit" className="auth-submit-button" disabled={isSubmitting}>
            {isSubmitting ? 'Создание...' : 'Создать аккаунт'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleVerify} className="auth-form">
          <p className="auth-banner auth-banner--neutral">
            Код отправлен на <strong>{pendingEmail}</strong>. Без подтверждения почты вход запрещён.
          </p>

          <AuthField
            label="Код подтверждения"
            name="verificationCode"
            placeholder="Например, 123456"
            value={verificationCode}
            onChange={(event) => setVerificationCode(event.target.value)}
            autoComplete="one-time-code"
            disabled={isSubmitting}
          />

          {errorMessage ? <p className="auth-banner auth-banner--error">{errorMessage}</p> : null}
          {successMessage ? <p className="auth-banner auth-banner--success">{successMessage}</p> : null}

          <div className="auth-verify-help">
            <p className="auth-field__meta">
              Код истёк или потерялся? Запросите новый. Повторная отправка доступна раз в минуту.
            </p>
            <button
              type="button"
              className="auth-text-button"
              disabled={isSubmitting || isResendingCode || resendCooldownSeconds > 0}
              onClick={handleResendCode}
            >
              {isResendingCode
                ? 'Отправка...'
                : resendCooldownSeconds > 0
                  ? `Отправить код заново через ${resendCooldownSeconds} сек`
                  : 'Отправить код заново'}
            </button>
          </div>

          <div className="auth-actions auth-actions--split">
            <button
              type="button"
              className="auth-secondary-button"
              disabled={isSubmitting || isResendingCode}
              onClick={() => {
                const params = new URLSearchParams()
                appendSafeNextParam(searchParams, params)
                navigate(
                  {
                    pathname: '/sign-up',
                    search: params.toString() ? `?${params.toString()}` : '',
                  },
                  { replace: true },
                )
                setFormData((prev) => ({ ...prev, email: pendingEmail || prev.email }))
                setPendingEmail('')
                setVerificationCode('')
                setStep('register')
                setErrorMessage('')
                setSuccessMessage('')
                setResendCooldownSeconds(0)
              }}
            >
              Назад
            </button>
            <button
              type="submit"
              className="auth-submit-button"
              disabled={isSubmitting || isResendingCode}
            >
              {isSubmitting ? 'Проверка...' : 'Подтвердить почту'}
            </button>
          </div>
        </form>
      )}
    </AuthLayout>
  )
}

export default SignUp
