import AppHeader from '../components/AppHeader.jsx'
import ActivityCard from '../components/ActivityCard.jsx'

const activities = [
  {
    title: 'Баскетбол в центре города',
    description: 'Нужны игроки для отличного матча kjnfv',
    location: 'Москва, Ленинский проспект, д. 17',
    date: '11.03.2026',
    capacity: 'Набрано 6/20',
    author: 'Alex Ivanov',
    image: 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800',
    linkTo: '/cards/default-0',
  },
  {
    title: 'Баскетбол в центре города',
    description:
      'Нужны игроки для отличного матча kjndjkvndklvjnksldmckjscfhyuewivhjncdslkjvnslkd;vmklsnfv',
    location: 'Москва, Ленинский проспект, д. 17',
    date: '11.03.2026',
    capacity: 'Набрано 6/20',
    author: 'Alex Ivanov',
    image: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800',
  },
  {
    title: 'Баскетбол в центре города',
    description:
      'Нужны игроки для отличного матча kjndjkvndklvjnksldmckjscfhyuewivhjncdslkjvnslkd;vmklsnfv',
    location: 'Москва, Ленинский проспект, д. 17',
    date: '11.03.2026',
    capacity: 'Набрано 6/20',
    author: 'Alex Ivanov',
    image: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800',
  },
]

function MainPage() {
  return (
    <>
      <AppHeader activeTab="main" />
      <section className="main-hero">
        <h2>Исследуйте активности</h2>
        <h3 className="gray-elem">Найдите партнеров по хобби, проектам, интересам</h3>
      </section>

      <main className="main-page-content">
        <div className="search-wrapper">
          <button className="search-button" type="button" aria-label="Поиск">
            🔍
          </button>
          <input placeholder="Поиск активностей..." type="text" />
        </div>

        <select name="categories" defaultValue="all-categories">
          <option value="all-categories">Все категории</option>
          <option value="sport">Спорт</option>
          <option value="music">Музыка</option>
          <option value="art">Искусство</option>
          <option value="entertainment">Развлечения</option>
          <option value="business">Бизнес</option>
          <option value="education">Образование</option>
          <option value="active-recreation">Активный отдых</option>
          <option value="passive-recreation">Пассивный отдых</option>
          <option value="others">Другое</option>
        </select>

        <button type="button">Фильтры</button>

        <section className="cards">
          {activities.map((item, index) => (
            <ActivityCard key={`${item.title}-${index}`} item={item} />
          ))}
        </section>
      </main>
    </>
  )
}

export default MainPage
