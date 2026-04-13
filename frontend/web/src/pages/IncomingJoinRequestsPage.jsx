import { Link } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'
import IncomingJoinRequestsSection from '../components/IncomingJoinRequestsSection.jsx'

function IncomingJoinRequestsPage() {
  return (
    <>
      <AppHeader activeTab={null} />
      <div className="profile-list-shell">
        <section className="main-hero">
          <h2>Входящие заявки</h2>
          <h3 className="gray-elem">Ожидающие заявки в комнаты, где у вас есть права модерации</h3>
        </section>

        <main className="profile-list-page">
          <div className="profile-list-backline">
            <Link className="back-link" to="/profile">
              ← Назад в профиль
            </Link>
          </div>

          <IncomingJoinRequestsSection
            title="Все ожидающие заявки"
            description="Заявки сгруппированы по комнатам. Email пользователя здесь не показывается."
            emptyMessage="Сейчас у вас нет ожидающих заявок на вступление."
            nextPath="/profile/incoming-requests"
            groupByRoom
          />
        </main>
      </div>
    </>
  )
}

export default IncomingJoinRequestsPage
