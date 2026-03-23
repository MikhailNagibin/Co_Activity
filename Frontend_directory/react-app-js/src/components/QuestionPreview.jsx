import { Link } from 'react-router-dom'

function QuestionPreview({ item }) {
  const title = item.linkTo ? (
    <Link className="question-link" to={item.linkTo}>
      {item.title}
    </Link>
  ) : (
    item.title
  )

  return (
    <article className="question-item">
      <div className="question-author-col">
        <i className="fa-regular fa-circle-user question-avatar" aria-hidden="true"></i>
        <h4>{item.author}</h4>
        <em>{item.createdAt}</em>
      </div>
      <div className="question-content-col">
        <h2>{title}</h2>
        <p>{item.description}</p>
        <div className="question-tags">
          {item.tags.map((tag) => (
            <button key={tag} type="button">
              {tag}
            </button>
          ))}
        </div>
        <div className="question-meta">
          <span aria-hidden="true">💬</span>
          <em>{item.answersCount} ответа</em>
        </div>
      </div>
    </article>
  )
}

export default QuestionPreview
