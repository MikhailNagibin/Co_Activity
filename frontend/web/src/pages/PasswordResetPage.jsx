import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import { describeFetchFailure, isApiError } from '../api/httpClient.js'
import { getUserFacingApiMessage } from '../utils/userFacingApiError.js'
import {
  confirmPasswordReset,
  requestPasswordReset,
  verifyPasswordReset,
} from '../services/authService.js'

function normalizeStep(step, hasEmail) {
  if (step === 'verify' && hasEmail) {
    return 'verify'
  }
  if (step === 'confirm' && hasEmail) {
    return 'confirm'
  }
  if (step === 'done') {
    return 'done'
  }
  return 'request'
}

function PasswordResetPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const errorRef = useRef(null)
  const [formData, setFormData] = useState({
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [step, setStep] = useState('request')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [invalidFields, setInvalidFields] = useState({})

  useEffect(() => {
    const email = searchParams.get('email')?.trim() ?? ''
    const requestedStep = searchParams.get('step')
    const normalizedStep = normalizeStep(requestedStep, Boolean(email))

    setStep(normalizedStep)
    if (email) {
      setFormData((prev) => ({ ...prev, email }))
    }

    if (normalizedStep === 'verify') {
      setSuccessMessage('Мы отправили инструкции на почту, если аккаунт существует и активирован.')
    }
    if (normalizedStep === 'confirm') {
      setSuccessMessage('Код подтверждён. Теперь задайте новый пароль.')
    }
  }, [searchParams])

  useEffect(() => {
    if (!errorMessage) {
      return
    }
    errorRef.current?.focus()
  }, [errorMessage])

  const openStep = (nextStep, nextEmail, message) => {
    const params = new URLSearchParams()
    if (nextStep !== 'request') {
      params.set('step', nextStep)
    }
    if (nextEmail) {
      params.set('email', nextEmail)
    }

    setStep(nextStep)
    setErrorMessage('')
    setInvalidFields({})
    setSuccessMessage(message)
    navigate(
      {
        pathname: '/password-reset',
        search: params.toString() ? `?${params.toString()}` : '',
      },
      { replace: true },
    )
  }

  const showError = (message, fields = {}) => {
    setInvalidFields(fields)
    setErrorMessage(message)
  }

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setInvalidFields((prev) => {
      if (!prev[name]) {
        return prev
      }
      return { ...prev, [name]: false }
    })
  }

  const handleRequestReset = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')
    setInvalidFields({})

    const email = formData.email.trim()
    if (!email) {
      showError('Укажите почту', { email: true })
      return
    }

    setIsSubmitting(true)
    try {
      await requestPasswordReset({ email })
      openStep(
        'verify',
        email,
        'Если аккаунт существует и подтверждён, мы отправили код для сброса пароля на эту почту.',
      )
    } catch (error) {
      if (isApiError(error)) {
        showError(getUserFacingApiMessage(error, 'Не удалось отправить код для сброса пароля.'), {
          email: true,
        })
      } else {
        showError(describeFetchFailure(error), { email: true })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleVerifyCode = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')
    setInvalidFields({})

    const email = formData.email.trim()
    const code = formData.code.trim()

    if (!email) {
      showError('Сначала укажите почту', { email: true })
      openStep('request', '', '')
      return
    }
    if (!/^\d{6}$/.test(code)) {
      showError('Введите 6-значный код из письма', { code: true })
      return
    }

    setIsSubmitting(true)
    try {
      await verifyPasswordReset({ email, code })
      openStep('confirm', email, 'Код подтверждён. Теперь задайте новый пароль.')
    } catch (error) {
      if (isApiError(error)) {
        const codeInvalid =
          error.code === 'INVALID_PASSWORD_RESET_CODE' ||
          error.code === 'PASSWORD_RESET_CODE_EXPIRED'
        showError(getUserFacingApiMessage(error, 'Не удалось проверить код сброса.'), {
          code: codeInvalid,
          email: !codeInvalid,
        })
      } else {
        showError(describeFetchFailure(error), { code: true })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleConfirmReset = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')
    setInvalidFields({})

    const email = formData.email.trim()
    const code = formData.code.trim()
    if (!email) {
      showError('Сначала запросите код сброса', { email: true })
      openStep('request', '', '')
      return
    }
    if (!/^\d{6}$/.test(code)) {
      showError('Сначала подтвердите корректный 6-значный код', { code: true })
      openStep('verify', email, '')
      return
    }
    if (formData.newPassword.length < 8) {
      showError('Пароль: от 8 символов', { newPassword: true })
      return
    }
    if (formData.confirmPassword !== formData.newPassword) {
      showError('Пароли не совпадают', { confirmPassword: true })
      return
    }

    setIsSubmitting(true)
    try {
      await confirmPasswordReset({
        email,
        code,
        newPassword: formData.newPassword,
      })
      setFormData({
        email,
        code: '',
        newPassword: '',
        confirmPassword: '',
      })
      openStep('done', email, 'Пароль обновлён. Теперь можно войти с новым паролем.')
    } catch (error) {
      if (isApiError(error)) {
        const codeInvalid =
          error.code === 'INVALID_PASSWORD_RESET_CODE' ||
          error.code === 'PASSWORD_RESET_CODE_EXPIRED'
        showError(getUserFacingApiMessage(error, 'Не удалось обновить пароль.'), {
          code: codeInvalid,
          newPassword: !codeInvalid,
        })
      } else {
        showError(describeFetchFailure(error), { newPassword: true })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Восстановление пароля"
      subtitle={
        step === 'request'
          ? 'Введите почту, чтобы запросить код для сброса пароля'
          : step === 'verify'
            ? 'Введите код из письма'
            : step === 'confirm'
              ? 'Задайте новый пароль'
              : 'Пароль успешно обновлён'
      }
      authActionLabel="Войти"
      authActionTo="/sign-in"
      footer={
        <p className="auth-card__footer-text">
          Вспомнили пароль? <Link to="/sign-in">Вернуться ко входу</Link>
        </p>
      }
    >
      {step === 'request' ? (
        <form onSubmit={handleRequestReset} className="auth-form">
          <p className="auth-banner auth-banner--neutral">
            Код отправляется только для существующего и подтверждённого аккаунта, но ответ UI всегда
            остаётся безопасно нейтральным.
          </p>

          <AuthField
            label="Почта"
            name="email"
            placeholder="Введите почту"
            value={formData.email}
            onChange={handleFieldChange}
            autoComplete="email"
            disabled={isSubmitting}
            aria-invalid={invalidFields.email ? 'true' : 'false'}
            aria-describedby={errorMessage ? 'password-reset-error' : undefined}
          />

          {errorMessage ? (
            <p
              id="password-reset-error"
              ref={errorRef}
              tabIndex={-1}
              className="auth-banner auth-banner--error"
            >
              {errorMessage}
            </p>
          ) : null}
          {successMessage ? <p className="auth-banner auth-banner--success">{successMessage}</p> : null}

          <button type="submit" className="auth-submit-button" disabled={isSubmitting}>
            {isSubmitting ? 'Отправка...' : 'Получить код'}
          </button>
        </form>
      ) : null}

      {step === 'verify' ? (
        <form onSubmit={handleVerifyCode} className="auth-form">
          <p className="auth-banner auth-banner--neutral">
            Введите код из письма для <strong>{formData.email}</strong>.
          </p>

          <AuthField
            label="Код сброса"
            name="code"
            placeholder="Например, 123456"
            value={formData.code}
            onChange={handleFieldChange}
            autoComplete="one-time-code"
            inputMode="numeric"
            maxLength={6}
            disabled={isSubmitting}
            aria-invalid={invalidFields.code ? 'true' : 'false'}
            aria-describedby={errorMessage ? 'password-reset-error' : undefined}
          />

          {errorMessage ? (
            <p
              id="password-reset-error"
              ref={errorRef}
              tabIndex={-1}
              className="auth-banner auth-banner--error"
            >
              {errorMessage}
            </p>
          ) : null}
          {successMessage ? <p className="auth-banner auth-banner--success">{successMessage}</p> : null}

          <div className="auth-actions auth-actions--split">
            <button
              type="button"
              className="auth-secondary-button"
              disabled={isSubmitting}
              onClick={() => openStep('request', '', '')}
            >
              Изменить почту
            </button>
            <button type="submit" className="auth-submit-button" disabled={isSubmitting}>
              {isSubmitting ? 'Проверка...' : 'Проверить код'}
            </button>
          </div>
        </form>
      ) : null}

      {step === 'confirm' ? (
        <form onSubmit={handleConfirmReset} className="auth-form">
          <p className="auth-banner auth-banner--neutral">
            Код подтверждён для <strong>{formData.email}</strong>. Новый пароль должен быть не короче
            8 символов.
          </p>

          <AuthField
            label="Новый пароль"
            type="password"
            name="newPassword"
            placeholder="Введите новый пароль"
            value={formData.newPassword}
            onChange={handleFieldChange}
            autoComplete="new-password"
            disabled={isSubmitting}
            aria-invalid={invalidFields.newPassword ? 'true' : 'false'}
            aria-describedby={errorMessage ? 'password-reset-error' : undefined}
          />
          <AuthField
            label="Повторите новый пароль"
            type="password"
            name="confirmPassword"
            placeholder="Повторите новый пароль"
            value={formData.confirmPassword}
            onChange={handleFieldChange}
            autoComplete="new-password"
            disabled={isSubmitting}
            aria-invalid={invalidFields.confirmPassword ? 'true' : 'false'}
            aria-describedby={errorMessage ? 'password-reset-error' : undefined}
          />

          {errorMessage ? (
            <p
              id="password-reset-error"
              ref={errorRef}
              tabIndex={-1}
              className="auth-banner auth-banner--error"
            >
              {errorMessage}
            </p>
          ) : null}
          {successMessage ? <p className="auth-banner auth-banner--success">{successMessage}</p> : null}

          <div className="auth-actions auth-actions--split">
            <button
              type="button"
              className="auth-secondary-button"
              disabled={isSubmitting}
              onClick={() => openStep('verify', formData.email.trim(), '')}
            >
              Назад к коду
            </button>
            <button type="submit" className="auth-submit-button" disabled={isSubmitting}>
              {isSubmitting ? 'Сохранение...' : 'Сменить пароль'}
            </button>
          </div>
        </form>
      ) : null}

      {step === 'done' ? (
        <div className="auth-form">
          <p className="auth-banner auth-banner--success">{successMessage}</p>
          <button
            type="button"
            className="auth-submit-button"
            onClick={() => navigate('/sign-in', { replace: true })}
          >
            Перейти ко входу
          </button>
        </div>
      ) : null}
    </AuthLayout>
  )
}

export default PasswordResetPage
