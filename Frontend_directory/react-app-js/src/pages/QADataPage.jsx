import AppHeader from '../components/AppHeader.jsx'
import QuestionPreview from '../components/QuestionPreview.jsx'

const questionTemplate = {
  author: 'Alex Ivanov',
  createdAt: '02.03.2026 18:24',
  title: 'Как начать изучать язык программирования Java новичку?',
  description:
    'Я имею небольшой опыт в программировании на Python, с Объектно-ориентированным программированием возникают объективные проблемы - но если я выучу такой ООПшный язык как джава, то больше...',
  tags: ['java', 'программирование', 'it', 'python'],
  answersCount: 3,
}

const questions = [
  { ...questionTemplate, linkTo: '/questions/default-0' },
  { ...questionTemplate },
  { ...questionTemplate },
  { ...questionTemplate },
]

const keywordTags = [
  'java',
  'программирование',
  'баскетбол',
  'футбол',
  'начинающий',
  'it',
  'python',
  'математика',
  'Дейкстра',
]

function QADataPage() {
  return (
    <>
      <AppHeader activeTab="qa" />
      <section className="main-hero">
        <h2>Форум для самых любознательных</h2>
        <h3 className="gray-elem">
          Задавайте вопросы и делитесь своими знаниями и опытом с сообществом
        </h3>
      </section>

      <main className="main-page-content qa-page-content">
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

        <div className="keywords-row">
          <h2>Ключевые слова:</h2>
          <section className="tags">
            {keywordTags.map((tag) => (
              <button key={tag} type="button">
                {tag}
              </button>
            ))}
          </section>
        </div>

        <section className="questions">
          {questions.map((item, index) => (
            <QuestionPreview key={`${item.title}-${index}`} item={item} />
          ))}
        </section>
      </main>
    </>
  )
}

export default QADataPage
