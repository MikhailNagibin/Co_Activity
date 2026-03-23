import { Link } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'

function DefaultCard0() {
  return (
    <>
      <AppHeader activeTab="main" />
      <div className="virtual-elem"></div>

      <main className="default-card-page">
        <Link className="back-link" to="/main">
          ← Обратно
        </Link>

        <div className="two-top-fields">
          <img
            src="https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800"
            alt="Баскетбол в центре города"
          />
          <section>
            <article>
              <h2>Организатор</h2>
              <hr />
              <div className="organizer-row">
                <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
                <h2>Alex Ivanov</h2>
              </div>
            </article>

            <article>
              <h2>Присоединиться к комнате</h2>
              <div className="sign-in-to-answer">
                <button type="button" className="cta-black-button">
                  <Link to="/sign-in">Войти, чтобы Присоединиться</Link>
                </button>
              </div>
            </article>
          </section>
        </div>
      </main>
    </>
  )
}

export default DefaultCard0
