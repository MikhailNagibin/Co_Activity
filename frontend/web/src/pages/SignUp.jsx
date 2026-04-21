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
  login,
  register,
  resendRegistrationVerificationCode,
  verifyRegistration,
} from '../services/authService.js'
import { uploadMyAvatar } from '../services/profileService.js'
import { getUnsupportedImageFiles, revokeObjectUrl } from '../utils/mediaFiles.js'

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

function isEmailLike(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value ?? '').trim())
}

function mapRegisterApiErrorToFields(error) {
  if (!isApiError(error)) {
    return {}
  }

  if (error.code === 'EMAIL_ALREADY_REGISTERED') {
    return { email: 'Эта почта уже занята' }
  }

  if (error.code === 'USERNAME_ALREADY_TAKEN') {
    return { nickname: 'Это имя пользователя уже занято' }
  }

  const message = String(error.message ?? '').toLowerCase()
  const nextErrors = {}

  if (message.includes('email')) {
    nextErrors.email = 'Проверьте почту'
  }
  if (message.includes('username') || message.includes('user name') || message.includes('nickname')) {
    nextErrors.nickname = 'Проверьте имя пользователя'
  }
  if (message.includes('password')) {
    nextErrors.password = 'Проверьте пароль'
  }
  if (message.includes('date') || message.includes('birth')) {
    nextErrors.birthDate = 'Проверьте дату рождения'
  }

  return nextErrors
}

function SignUp() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { isAuthenticated, markAuthenticated } = useAuthSession()
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
  const [fieldErrors, setFieldErrors] = useState({})
  const [pendingEmail, setPendingEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [step, setStep] = useState('register')
  const [isResendingCode, setIsResendingCode] = useState(false)
  const [resendCooldownSeconds, setResendCooldownSeconds] = useState(0)
  const [selectedAvatarFile, setSelectedAvatarFile] = useState(null)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState('')

  useEffect(() => {
    if (isAuthenticated) {
      const next = searchParams.get('next')
      const safeNext = next && next.startsWith('/') && !next.startsWith('//') ? next : '/main'
      navigate(safeNext, { replace: true })
    }
  }, [isAuthenticated, navigate, searchParams])

  useEffect(() => {
    return () => {
      revokeObjectUrl(avatarPreviewUrl)
    }
  }, [avatarPreviewUrl])

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

  const openVerifyStep = (email, cooldownSeconds = 0) => {
    const params = new URLSearchParams()
    params.set('step', 'verify')
    params.set('email', email)
    appendSafeNextParam(searchParams, params)
    setFormData((prev) => ({ ...prev, email }))
    setPendingEmail(email)
    setVerificationCode('')
    setStep('verify')
    setErrorMessage('')
    setResendCooldownSeconds(cooldownSeconds)
    navigate({ pathname: '/sign-up', search: `?${params.toString()}` }, { replace: true })
  }

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setFieldErrors((prev) => {
      if (!prev[name] && !((name === 'password' || name === 'passwordAgain') && (prev.password || prev.passwordAgain))) {
        return prev
      }
      const nextErrors = { ...prev, [name]: '' }
      if (name === 'password' || name === 'passwordAgain') {
        nextErrors.password = ''
        nextErrors.passwordAgain = ''
      }
      if (name === 'country' || name === 'city') {
        nextErrors.country = ''
        nextErrors.city = ''
      }
      return nextErrors
    })
  }

  const resetSelectedAvatar = () => {
    revokeObjectUrl(avatarPreviewUrl)
    setSelectedAvatarFile(null)
    setAvatarPreviewUrl('')
    setFieldErrors((prev) => ({ ...prev, avatar: '' }))
  }

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0] ?? null
    event.target.value = ''

    setErrorMessage('')
    setFieldErrors((prev) => ({ ...prev, avatar: '' }))

    if (!file) {
      resetSelectedAvatar()
      return
    }

    if (getUnsupportedImageFiles([file]).length > 0) {
      resetSelectedAvatar()
      setFieldErrors((prev) => ({
        ...prev,
        avatar: 'Аватар должен быть в формате PNG, JPEG или WEBP',
      }))
      return
    }

    revokeObjectUrl(avatarPreviewUrl)
    setSelectedAvatarFile(file)
    setAvatarPreviewUrl(URL.createObjectURL(file))
  }

  const handleRegister = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setFieldErrors({})

    const nextFieldErrors = {}
    const email = formData.email.trim()
    const userName = formData.nickname.trim()

    if (formData.password !== formData.passwordAgain) {
      nextFieldErrors.passwordAgain = 'Пароли не совпадают'
    }

    if (!email) {
      nextFieldErrors.email = 'Укажите почту'
    } else if (!isEmailLike(email)) {
      nextFieldErrors.email = 'Введите корректную почту'
    }

    if (userName.length < 2 || userName.length > 20) {
      nextFieldErrors.nickname = 'Имя пользователя: от 2 до 20 символов'
    }
    if (formData.password.length < 8 || formData.password.length > 128) {
      nextFieldErrors.password = 'Пароль: от 8 до 128 символов'
    }

    const dateOfBirth = birthDateInputToInstant(formData.birthDate)
    if (!dateOfBirth) {
      nextFieldErrors.birthDate = 'Укажите дату рождения'
    }

    if (formData.country.trim().length > 100) {
      nextFieldErrors.country = 'Страна: до 100 символов'
    }

    if (formData.city.trim().length > 100) {
      nextFieldErrors.city = 'Город: до 100 символов'
    }

    if (formData.country.trim() === '' && formData.city.trim() !== '') {
      nextFieldErrors.country = 'Сначала укажите страну'
      nextFieldErrors.city = 'Город можно указать только после страны'
    }

    if (formData.about.trim().length > 500) {
      nextFieldErrors.about = 'О себе: до 500 символов'
    }

    if (Object.values(nextFieldErrors).some(Boolean)) {
      setFieldErrors(nextFieldErrors)
      setErrorMessage('Исправьте подсвеченные поля и попробуйте снова.')
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
      openVerifyStep(email, 60)
    } catch (error) {
      if (isUnauthorizedApiError(error)) {
        redirectToSignInForExpiredSession(navigate, { next: '/sign-up' })
        return
      }
      if (isApiError(error)) {
        const mappedFieldErrors = mapRegisterApiErrorToFields(error)
        if (Object.keys(mappedFieldErrors).length > 0) {
          setFieldErrors((prev) => ({ ...prev, ...mappedFieldErrors }))
        }
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

    const code = verificationCode.trim()
    if (!code) {
      setErrorMessage('Введите код из письма')
      return
    }

    setIsSubmitting(true)
    try {
      await verifyRegistration({ email: pendingEmail, code })
      const next = searchParams.get('next')
      const safeNext = next && next.startsWith('/') && !next.startsWith('//') ? next : '/main'

      if (formData.password.length >= 8) {
        try {
          const sessionPayload = await login({ email: pendingEmail, password: formData.password })
          markAuthenticated(sessionPayload)

          if (selectedAvatarFile) {
            try {
              const updatedProfile = await uploadMyAvatar(selectedAvatarFile)
              markAuthenticated(updatedProfile)
            } catch (avatarError) {
              const avatarMessage = isApiError(avatarError)
                ? getUserFacingApiMessage(avatarError, 'Аккаунт подтверждён, но аватар не загрузился.')
                : 'Аккаунт подтверждён, но аватар не загрузился.'
              window.alert(`${avatarMessage} Вы сможете загрузить его позже в профиле.`)
            }
          }

          resetSelectedAvatar()
          navigate(safeNext, { replace: true })
          return
        } catch {
          const params = new URLSearchParams()
          params.set('email', pendingEmail)
          if (next && next.startsWith('/') && !next.startsWith('//')) {
            params.set('next', next)
          }
          navigate(
            {
              pathname: '/sign-in',
              search: `?${params.toString()}`,
            },
            { replace: true },
          )
          return
        }
      }

      const params = new URLSearchParams()
      params.set('email', pendingEmail)
      if (next && next.startsWith('/') && !next.startsWith('//')) {
        params.set('next', next)
      }
      navigate(
        {
          pathname: '/sign-in',
          search: `?${params.toString()}`,
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
    try {
      await resendRegistrationVerificationCode({ email: pendingEmail })
      setVerificationCode('')
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
        <div className="auth-card__footer-stack">
          <p className="auth-card__footer-text">
            Уже есть аккаунт? <Link to="/sign-in">Войти</Link>
          </p>
          {step === 'register' ? (
            <p className="auth-card__footer-text">
              <Link to="/main">Продолжить без регистрации</Link>
            </p>
          ) : null}
        </div>
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
            error={fieldErrors.email}
          />
          <AuthField
            label="Имя пользователя"
            name="nickname"
            placeholder="Введите имя"
            value={formData.nickname}
            onChange={handleFieldChange}
            autoComplete="username"
            disabled={isSubmitting}
            error={fieldErrors.nickname}
            hint="Публичное имя от 2 до 20 символов."
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
            error={fieldErrors.password}
            hint="От 8 до 128 символов."
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
            error={fieldErrors.passwordAgain}
          />
          <AuthField
            label="Дата рождения"
            type="date"
            name="birthDate"
            value={formData.birthDate}
            onChange={handleFieldChange}
            disabled={isSubmitting}
            error={fieldErrors.birthDate}
          />

          <div className="auth-grid auth-grid--two">
            <AuthField
              label="Страна"
              name="country"
              placeholder="Например, Россия"
              value={formData.country}
              onChange={handleFieldChange}
              disabled={isSubmitting}
              error={fieldErrors.country}
            />
            <AuthField
              label="Город"
              name="city"
              placeholder="Например, Москва"
              value={formData.city}
              onChange={handleFieldChange}
              disabled={isSubmitting}
              error={fieldErrors.city}
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
              aria-invalid={fieldErrors.about ? 'true' : 'false'}
              aria-describedby={fieldErrors.about ? 'about-error' : undefined}
            />
            {fieldErrors.about ? (
              <p id="about-error" className="auth-field__error" role="alert">
                {fieldErrors.about}
              </p>
            ) : null}
          </div>

          <div
            className={
              fieldErrors.avatar
                ? 'auth-avatar-card auth-avatar-card--error'
                : 'auth-avatar-card'
            }
          >
            <div className="auth-avatar-card__preview">
              {avatarPreviewUrl ? (
                <img src={avatarPreviewUrl} alt="Предпросмотр аватара" />
              ) : (
                <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
              )}
            </div>
            <div className="auth-avatar-card__body">
              <label className="profile-avatar-upload auth-avatar-card__upload">
                <span>{selectedAvatarFile ? 'Выбрать другой аватар' : 'Выбрать аватар'}</span>
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  disabled={isSubmitting}
                  onChange={handleAvatarChange}
                />
              </label>
              <p className="auth-field__meta">
                Аватар необязателен. Если выберете файл сейчас, он загрузится сразу после
                подтверждения почты.
              </p>
              {fieldErrors.avatar ? (
                <p className="auth-field__error" role="alert">
                  {fieldErrors.avatar}
                </p>
              ) : null}
              {selectedAvatarFile ? (
                <p className="auth-avatar-card__file">Файл: {selectedAvatarFile.name}</p>
              ) : null}
              {selectedAvatarFile ? (
                <button
                  type="button"
                  className="auth-text-button auth-avatar-card__reset"
                  disabled={isSubmitting}
                  onClick={resetSelectedAvatar}
                >
                  Убрать аватар
                </button>
              ) : null}
            </div>
          </div>

          {errorMessage ? <p className="auth-banner auth-banner--error">{errorMessage}</p> : null}

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
