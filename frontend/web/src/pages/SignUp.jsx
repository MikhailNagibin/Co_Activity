import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  isUnauthorizedApiError,
  redirectToSignInForExpiredSession,
} from '../utils/sessionExpiredRedirect.js'
import { register } from '../services/authService.js'

/** Backend: Instant (ISO-8601), e.g. 2000-01-01T00:00:00Z */
function birthDateInputToInstant(isoDate) {
  if (!isoDate || !/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) {
    return null
  }
  return `${isoDate}T00:00:00Z`
}

function SignUp() {
  const navigate = useNavigate()
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

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
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

    /** Matches core-service UserRegistrationRequest */
    const payload = {
      login: email,
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
      setSuccessMessage('Аккаунт успешно создан. Теперь можно войти.')
      setFormData({
        email: '',
        nickname: '',
        password: '',
        passwordAgain: '',
        birthDate: '',
        country: '',
        city: '',
        about: '',
      })
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

  return (
    <AuthLayout
      title="Создать аккаунт в CoActivity"
      subtitle="Заполните поля для регистрации"
      authActionLabel="Регистрация"
      authActionTo="/sign-in"
      footer={
        <h3>
          Уже есть аккаунт? <Link to="/sign-in">Войти</Link>
        </h3>
      }
    >
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
    </AuthLayout>
  )
}

export default SignUp
