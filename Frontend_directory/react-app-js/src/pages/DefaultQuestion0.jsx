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
      <main className="default-question-page">
        <Link className="back-link" to="/qa">
          ← Обратно
        </Link>

        <article id="specific-question">
          <div className="question-author-col">
            <i
              className="fa-regular fa-circle-user question-avatar question-avatar-lg"
              aria-hidden="true"
            ></i>
            <h3>Alex Ivanov</h3>
            <em>02.03.2026 18:24</em>
          </div>

          <div>
            <h2>Как начать изучать язык программирования Java новичку?</h2>
            <p>
              Я имею небольшой опыт в программировании на Python, с
              Объектно-ориентированным программированием возникают объективные проблемы - но
              если я выучу такой ООПшный язык как джава, то больше вопросов по питончику у
              меня явно не останется! Я пробовал читать книги, но они давались сложно. Курсы
              же какие-то дорогие бывают, а меня жаба душит, не могу, хочу стать
              топ-джавистом здесь и сейчас. Посоветуйте что-нибудь таким нетерпеливым и
              заряженным типам, как я. Спасибо
            </p>
            <div className="question-tags">
              <button type="button">java</button>
              <button type="button">программирование</button>
              <button type="button">it</button>
              <button type="button">python</button>
            </div>
          </div>
        </article>

        <div className="answers-wrap">
          <h2>3 ответа</h2>
          {answers.map((answer) => (
            <article
              className={`answer ${answer.accepted ? 'accepted-answer' : ''}`}
              key={`${answer.author}-${answer.date}`}
            >
              <div className="account-part-of-article">
                <i className="fa-regular fa-circle-user question-avatar" aria-hidden="true"></i>
                <h4>{answer.author}</h4>
                <em>{answer.date}</em>
              </div>
              <div className="answer-body">
                {answer.accepted ? (
                  <div className="accepted">
                    <span aria-hidden="true">✓</span>
                    <strong>принятый ответ</strong>
                  </div>
                ) : null}
                <p>{answer.text}</p>
              </div>
            </article>
          ))}
        </div>
      </main>

      <div className="sign-in-to-answer">
        <button type="button" className="cta-black-button">
          <Link to="/sign-in">Войти, чтобы ответить на вопрос</Link>
        </button>
      </div>
    </>
  )
}

export default DefaultQuestion0
