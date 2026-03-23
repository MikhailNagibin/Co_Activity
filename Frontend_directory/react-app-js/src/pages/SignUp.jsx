import { Link } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'

function SignUp() {
  return (
    <AuthLayout
      title="Создать аккаунт в CoActivity"
      subtitle="Заполните поля для регистрации"
      authActionLabel="Войти"
      authActionTo="/sign-in"
    >
      <AuthField label="Почта" name="email" placeholder="Введите почту" />
      <AuthField label="Имя пользователя" name="nickname" placeholder="Введите имя" />
      <AuthField label="Пароль" name="password" placeholder="Введите пароль" />
      <AuthField
        label="Повтор пароля"
        name="password-again"
        placeholder="Введите пароль еще раз"
      />
      <AuthField label="Дата рождения" type="date" name="birth-date" />

      <div className="split-fields">
        <AuthField label="Страна" name="country" />
        <AuthField label="Город" name="city" />
      </div>

      <div>
        <h3>О себе</h3>
        <textarea name="about" rows="5"></textarea>
      </div>

      <button
        type="button"
        style={{
          marginTop: '20px',
          backgroundColor: 'black',
          color: 'white',
          padding: '12px',
        }}
        className="enter-button"
      >
        Создать аккаунт
      </button>
    </AuthLayout>
  )
}

export default SignUp
