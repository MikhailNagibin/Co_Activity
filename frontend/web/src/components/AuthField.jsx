function AuthField({
  label,
  type = 'text',
  name,
  placeholder,
  inlineRight = null,
  value,
  onChange,
  autoComplete,
  disabled = false,
  error = '',
  hint = '',
  ...inputProps
}) {
  const inputId = inputProps.id ?? name
  const hintId = hint ? `${inputId}-hint` : ''
  const errorId = error ? `${inputId}-error` : ''
  const describedBy = [inputProps['aria-describedby'], hintId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className="auth-field">
      <div className={inlineRight ? 'field-head field-head--between' : 'field-head'}>
        <label htmlFor={inputId} className="auth-field__label">
          {label}
        </label>
        {inlineRight}
      </div>
      <input
        id={inputId}
        className="auth-field__input"
        type={type}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        disabled={disabled}
        aria-invalid={error ? 'true' : inputProps['aria-invalid']}
        aria-describedby={describedBy}
        {...inputProps}
      />
      {hint ? (
        <p id={hintId} className="auth-field__meta">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="auth-field__error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}

export default AuthField
