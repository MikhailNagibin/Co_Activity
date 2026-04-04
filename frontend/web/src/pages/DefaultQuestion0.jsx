import { Link } from 'react-router-dom'
import AppHeader from '../components/AppHeader.jsx'

const answers = [
  {
    author: 'Pyotr Sergeyev',
    date: '04.03.2026 10:05',
    text: "Попробуй потыкаться в Leetcode, там отработаешь синтаксис, видосики-разборы заданий глянешь - и профит. After that, try building small projects like a calculator or todo app to practice. The key is to code every day, even if it's just 30 minutes.",
    accepted: true,
  },
  {
    author: 'Vyacheslav Pupiy',
    date: '05.03.2026 11:15',
    text: `Check out "Head First Java" book - it's perfect for beginners! Also, codecademy and udemy have great interactive Java courses. Don't forget to join Java communities on Discord or Reddit for help.`,
  },
  {
    author: 'John Dickens',
    date: '06.03.2026 12:45',
    text: `Check out "Head First Java" book - it's perfect for beginners! Also, codecademy and udemy have great interactive Java courses. Don't forget to join Java communities on Discord or Reddit for help.`,
  },
]

function DefaultQuestion0() {
  return (
    <>
      <AppHeader activeTab="qa" />
      <div className="main-page-shell qa-thread-shell">
        <Link className="back-link qa-thread-back" to="/qa">
          ← Назад к вопросам
        </Link>

        <article className="qa-thread-op">
          <aside className="qa-thread-op-sidebar" aria-label="Автор вопроса">
            <i
              className="fa-regular fa-circle-user qa-thread-avatar qa-thread-avatar--lg"
              aria-hidden="true"
            ></i>
            <h3 className="qa-thread-author-name">Alex Ivanov</h3>
            <time className="qa-thread-date">02.03.2026 18:24</time>
          </aside>

          <div className="qa-thread-op-main">
            <h1 className="qa-thread-title">Как начать изучать язык программирования Java новичку?</h1>
            <p className="qa-thread-body">
              Я имею небольшой опыт в программировании на Python, с Объектно-ориентированным
              программированием возникают объективные проблемы - но если я выучу такой ООПшный язык как
              джава, то больше вопросов по питончику у меня явно не останется! Я пробовал читать книги,
              но они давались сложно. Курсы же какие-то дорогие бывают, а меня жаба душит, не могу, хочу
              стать топ-джавистом здесь и сейчас. Посоветуйте что-нибудь таким нетерпеливым и заряженным
              типам, как я. Спасибо
            </p>
            <div className="qa-thread-op-tags">
              <span className="qa-tag">java</span>
              <span className="qa-tag">программирование</span>
              <span className="qa-tag">it</span>
              <span className="qa-tag">python</span>
            </div>
          </div>
        </article>

        <section className="qa-thread-answers" aria-label="Ответы">
          <h2 className="qa-thread-answers-heading">3 ответа</h2>
          {answers.map((answer) => (
            <article
              className={`qa-answer-card${answer.accepted ? ' qa-answer-card--accepted' : ''}`}
              key={`${answer.author}-${answer.date}`}
            >
              <aside className="qa-answer-sidebar" aria-label="Автор ответа">
                <i className="fa-regular fa-circle-user qa-thread-avatar" aria-hidden="true"></i>
                <h4 className="qa-answer-author">{answer.author}</h4>
                <time className="qa-thread-date">{answer.date}</time>
              </aside>
              <div className="qa-answer-body">
                {answer.accepted ? (
                  <div className="qa-answer-badge">
                    <span aria-hidden="true">✓</span>
                    <span>Принятый ответ</span>
                  </div>
                ) : null}
                <p>{answer.text}</p>
              </div>
            </article>
          ))}
        </section>

        <div className="qa-thread-cta">
          <Link className="main-create-activity-btn qa-thread-cta-link" to="/sign-in">
            Войти, чтобы ответить
          </Link>
        </div>
      </div>
    </>
  )
}

export default DefaultQuestion0
