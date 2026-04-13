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
  ...inputProps
}) {
  const inputId = inputProps.id ?? name

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
        {...inputProps}
      />
    </div>
  )
}

export default AuthField
