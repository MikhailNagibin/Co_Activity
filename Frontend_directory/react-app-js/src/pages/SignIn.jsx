import { Link } from 'react-router-dom'
import AuthField from '../components/AuthField.jsx'
import AuthLayout from '../components/AuthLayout.jsx'

function SignIn() {
  return (
    <AuthLayout
      title="Войти в CoActivity"
      subtitle="Введите свои данные для входа"
      authActionLabel="Войти"
      authActionTo="/sign-in"
      footer={
        <h3>
          У вас нет аккаунта? <Link to="/sign-up">Зарегистрироваться</Link>
        </h3>
      }
    >
      <AuthField label="Почта" name="email" placeholder="Введите почту" />
      <AuthField
        label="Пароль"
        name="password"
        placeholder="Введите пароль"
        inlineRight={
          <em>
            <a className="gray-elem" href="#">
              Забыли пароль?
            </a>
          </em>
        }
      />
      <button
        type="button"
        style={{ backgroundColor: 'black', color: 'white', padding: '12px' }}
        className="enter-button"
      >
        Войти
      </button>
    </AuthLayout>
  )
}

export default SignIn
