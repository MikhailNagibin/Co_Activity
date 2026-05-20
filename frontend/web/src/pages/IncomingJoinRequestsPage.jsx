import ProfileCabinetShell from '../components/ProfileCabinetShell.jsx'
import IncomingJoinRequestsSection from '../components/IncomingJoinRequestsSection.jsx'

function IncomingJoinRequestsPage() {
  return (
    <ProfileCabinetShell
      heroTitle="Входящие заявки"
      heroSubtitle="Ожидающие заявки в комнаты, где у вас есть права модерации"
    >
      <main className="profile-list-page">
        <IncomingJoinRequestsSection
          title="Все ожидающие заявки"
          description="Заявки сгруппированы по комнатам. Email пользователя здесь не показывается."
          emptyMessage="Сейчас у вас нет ожидающих заявок на вступление."
          nextPath="/profile/incoming-requests"
          groupByRoom
        />
      </main>
    </ProfileCabinetShell>
  )
}

export default IncomingJoinRequestsPage
