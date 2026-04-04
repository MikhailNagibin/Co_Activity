import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { describeFetchFailure, isApiError } from '../api/httpClient.js'
import { loginStep1, verifyCode } from '../services/authService.js'

function SignIn() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [step, setStep] = useState(1)
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    code: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handlePasswordSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    const login = formData.email.trim()
    if (!login) {
      setErrorMessage('Укажите почту')
      return
    }
    if (formData.password.length < 8) {
      setErrorMessage('Пароль: от 8 символов')
      return
    }

    setIsSubmitting(true)
    try {
      await loginStep1({ login, password: formData.password })
      setStep(2)
    } catch (error) {
      if (isApiError(error)) {
        if (error.status === 503) {
          setErrorMessage(
            `${error.message} Частая причина: не запущены Kafka/notifications-service или не настроен SMTP (см. .env: SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD).`,
          )
        } else {
          setErrorMessage(error.message)
        }
      } else {
        setErrorMessage(describeFetchFailure(error))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCodeSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')

    const login = formData.email.trim()
    const code = formData.code.trim()
    if (!code) {
      setErrorMessage('Введите код из письма')
      return
    }

    setIsSubmitting(true)
    try {
      await verifyCode({ login, code })
      const next = searchParams.get('next')
      const safeNext =
        next && next.startsWith('/') && !next.startsWith('//') ? next : '/main'
      navigate(safeNext)
    } catch (error) {
      if (isApiError(error)) {
        setErrorMessage(error.message)
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
      subtitle={
        step === 1
          ? 'Введите свои данные для входа'
          : 'Введите код из письма (отправлен на вашу почту)'
      }
      authActionLabel="Войти"
      authActionTo="/sign-in"
      footer={
        <h3>
          У вас нет аккаунта? <Link to="/sign-up">Зарегистрироваться</Link>
        </h3>
      }
    >
      {step === 1 ? (
        <form onSubmit={handlePasswordSubmit}>
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
            {isSubmitting ? 'Отправка...' : 'Далее'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleCodeSubmit}>
          <p className="gray-elem" style={{ marginBottom: '12px', textAlign: 'left' }}>
            Код действует около 10 минут. Письмо уходит не с сайта, а с сервера уведомлений: нужны
            запущенные Kafka и notifications-service и настроенная почта (Yandex и др.) в .env.
            Проверьте «Спам» и ту почту, которую указывали при регистрации.
          </p>
          <p className="gray-elem" style={{ marginBottom: '12px', textAlign: 'left', fontSize: '11px' }}>
            Локально без почты: в core-service можно задать AUTH_LOGIN_ALLOW_WITHOUT_KAFKA=true — код
            появится в логах core-service (только для разработки).
          </p>
          <AuthField
            label="Код подтверждения"
            name="code"
            placeholder="Например, 12345"
            value={formData.code}
            onChange={handleFieldChange}
            autoComplete="one-time-code"
            disabled={isSubmitting}
          />
          {errorMessage ? (
            <p style={{ color: '#b00020', marginTop: '12px', marginBottom: 0 }}>{errorMessage}</p>
          ) : null}
          <div style={{ display: 'flex', gap: '12px', marginTop: '20px', flexWrap: 'wrap' }}>
            <button
              type="button"
              style={{ padding: '12px' }}
              className="enter-button"
              disabled={isSubmitting}
              onClick={() => {
                setStep(1)
                setErrorMessage('')
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
              {isSubmitting ? 'Вход...' : 'Войти'}
            </button>
          </div>
        </form>
      )}
    </AuthLayout>
  )
}

export default SignIn
