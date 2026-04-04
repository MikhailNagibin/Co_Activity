import { Link } from 'react-router-dom'

function ActivityCard({ item }) {
  const body = (
    <>
      <h2 className="activity-card-title">{item.title}</h2>
      <p>{item.description}</p>
      <hr />
      <p>{item.location}</p>
      <p>{item.date}</p>
      <p>{item.capacity}</p>
      <div className="activity-card-author">
        <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
        <h5>{item.author}</h5>
      </div>
    </>
  )

  if (item.linkTo) {
    return (
      <Link className="activity-card-outer-link" to={item.linkTo}>
        <article className="activity-card">{body}</article>
      </Link>
    )
  }

  return <article className="activity-card">{body}</article>
}

export default ActivityCard
