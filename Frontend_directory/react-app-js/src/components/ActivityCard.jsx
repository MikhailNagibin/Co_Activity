import { Link } from 'react-router-dom'

function ActivityCard({ item }) {
  const cardTitle = item.linkTo ? (
    <Link className="activity-card-link" to={item.linkTo}>
      {item.title}
    </Link>
  ) : (
    item.title
  )

  return (
    <article className="activity-card">
      <div className="activity-card-image-wrap">
        <img src={item.image} alt={item.title} />
      </div>
      <h2>{cardTitle}</h2>
      <p>{item.description}</p>
      <hr />
      <p>{item.location}</p>
      <p>{item.date}</p>
      <p>{item.capacity}</p>
      <div className="activity-card-author">
        <i className="fa-regular fa-circle-user" aria-hidden="true"></i>
        <h5>{item.author}</h5>
      </div>
    </article>
  )
}

export default ActivityCard
