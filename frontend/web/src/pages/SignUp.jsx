import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { useAuthSession } from '../auth/authSessionContext.js'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { register, verifyRegistration } from '../services/authService.js'

function birthDateInputToInstant(isoDate) {
  if (!isoDate || !/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) {
    return null
  }
  return `${isoDate}T00:00:00Z`
}

function SignUp() {
  const navigate = useNavigate()
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

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/main', { replace: true })
    }
  }, [isAuthenticated, navigate])

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
      setPendingEmail(email)
      setStep('verify')
      setSuccessMessage('Аккаунт создан. Введите код из письма, чтобы активировать его.')
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
      navigate('/sign-in', { replace: true })
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

  return (
    <AuthLayout
      title="Создать аккаунт в CoActivity"
      subtitle={
        step === 'register'
          ? 'Заполните поля для регистрации'
          : 'Подтвердите почту кодом из письма'
      }
      authActionLabel="Регистрация"
      authActionTo="/sign-in"
      footer={
        <h3>
          Уже есть аккаунт? <Link to="/sign-in">Войти</Link>
        </h3>
      }
    >
      {step === 'register' ? (
        <form onSubmit={handleRegister}>
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

          <div className="split-fields">
            <AuthField
              label="Страна"
              name="country"
              value={formData.country}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            />
            <AuthField
              label="Город"
              name="city"
              value={formData.city}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            />
          </div>

          <div>
            <h3>О себе</h3>
            <textarea
              name="about"
              rows="5"
              value={formData.about}
              onChange={handleFieldChange}
              disabled={isSubmitting}
            ></textarea>
          </div>

          {errorMessage ? (
            <p style={{ color: '#b00020', marginTop: '12px', marginBottom: 0 }}>{errorMessage}</p>
          ) : null}
          {successMessage ? (
            <p style={{ color: '#146c43', marginTop: '12px', marginBottom: 0 }}>{successMessage}</p>
          ) : null}

          <button
            type="submit"
            style={{
              marginTop: '20px',
              backgroundColor: 'black',
              color: 'white',
              padding: '12px',
            }}
            className="enter-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Создание...' : 'Создать аккаунт'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleVerify}>
          <p className="gray-elem" style={{ marginBottom: '12px', textAlign: 'left' }}>
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

          {errorMessage ? (
            <p style={{ color: '#b00020', marginTop: '12px', marginBottom: 0 }}>{errorMessage}</p>
          ) : null}
          {successMessage ? (
            <p style={{ color: '#146c43', marginTop: '12px', marginBottom: 0 }}>{successMessage}</p>
          ) : null}

          <div style={{ display: 'flex', gap: '12px', marginTop: '20px', flexWrap: 'wrap' }}>
            <button
              type="button"
              style={{ padding: '12px' }}
              className="enter-button"
              disabled={isSubmitting}
              onClick={() => {
                setStep('register')
                setErrorMessage('')
                setSuccessMessage('')
              }}
            >
              Назад
            </button>
            <button
              type="submit"
              style={{ backgroundColor: 'black', color: 'white', padding: '12px' }}
              className="enter-button"
              disabled={isSubmitting}
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
