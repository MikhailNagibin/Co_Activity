import { Link } from 'react-router-dom'
import UserAvatar from './UserAvatar.jsx'

function answersWord(n) {
  const x = Math.abs(Number(n)) % 100
  const x1 = x % 10
  if (x > 10 && x < 20) {
    return 'ответов'
  }
  if (x1 > 1 && x1 < 5) {
    return 'ответа'
  }
  if (x1 === 1) {
    return 'ответ'
  }
  return 'ответов'
}

function QuestionPreview({ item, canViewProfiles = false }) {
  const answersNum = Number(item.answersCount)
  const answersCount = Number.isFinite(answersNum) ? answersNum : 0
  const authorLink =
    item.authorId != null && !Number.isNaN(Number(item.authorId))
      ? `/users/${Number(item.authorId)}`
      : null

  return (
    <article className="qa-question-card">
      {item.linkTo ? (
        <Link className="qa-question-card-link" to={item.linkTo}>
          <div className="qa-question-card-body">
            <h2 className="qa-question-card-title">
              <span className="qa-question-card-title-text">{item.title}</span>
            </h2>
            <p className="qa-question-card-excerpt">{item.description}</p>
            {item.tags.length > 0 ? (
              <div className="qa-question-card-tags" role="list">
                {item.tags.map((tag) => (
                  <span key={tag} className="qa-tag" role="listitem">
                    {tag}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
        </Link>
      ) : (
        <div className="qa-question-card-body">
          <h2 className="qa-question-card-title">{item.title}</h2>
          <p className="qa-question-card-excerpt">{item.description}</p>
          {item.tags.length > 0 ? (
            <div className="qa-question-card-tags" role="list">
              {item.tags.map((tag) => (
                <span key={tag} className="qa-tag" role="listitem">
                  {tag}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      )}
      <footer className="qa-question-card-footer">
        <div className="qa-question-card-author">
          <UserAvatar user={item.authorUser} alt={`Аватар, ${item.author}`} size="sm" />
          {canViewProfiles && authorLink ? <Link to={authorLink}>{item.author}</Link> : <span>{item.author}</span>}
        </div>
        {item.createdAt ? (
          <time
            className="qa-question-card-date"
            dateTime={item.createdAtIso != null && item.createdAtIso !== '' ? item.createdAtIso : undefined}
          >
            {item.createdAt}
          </time>
        ) : null}
        {answersCount > 0 ? (
          <div className="qa-question-card-stat">
            <i className="fa-regular fa-comments" aria-hidden="true"></i>
            <span>
              {answersCount} {answersWord(answersCount)}
            </span>
          </div>
        ) : null}
      </footer>
    </article>
  )
}

export default QuestionPreview
