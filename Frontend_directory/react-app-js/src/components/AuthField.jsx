function AuthField({
  label,
  type = 'text',
  name,
  placeholder,
  inlineRight = null,
}) {
  return (
    <div>
      <div className={inlineRight ? 'field-head field-head--between' : 'field-head'}>
        <h3>{label}</h3>
        {inlineRight}
      </div>
      <form action="">
        <input type={type} name={name} placeholder={placeholder} />
      </form>
    </div>
  )
}

export default AuthField
