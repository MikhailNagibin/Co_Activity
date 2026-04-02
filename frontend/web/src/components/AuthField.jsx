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
}) {
  return (
    <div>
      <div className={inlineRight ? 'field-head field-head--between' : 'field-head'}>
        <h3>{label}</h3>
        {inlineRight}
      </div>
      <input
        type={type}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        disabled={disabled}
      />
    </div>
  )
}

export default AuthField
