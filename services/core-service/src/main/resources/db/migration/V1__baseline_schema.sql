CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  login VARCHAR(100) NOT NULL,
  username VARCHAR(20) UNIQUE NOT NULL,
  password VARCHAR(128) NOT NULL,
  birthday TIMESTAMP,
  country VARCHAR(100),
  city VARCHAR(100),
  description TEXT,
  avatar_id INT
);

CREATE TABLE IF NOT EXISTS categories (
  id SERIAL PRIMARY KEY,
  name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rooms (
  id SERIAL PRIMARY KEY,
  is_active BOOLEAN NOT NULL,
  is_public BOOLEAN NOT NULL,
  chat_link VARCHAR(100),
  category_id INT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  age_rating INT NOT NULL,
  frequency TIMESTAMP,
  maximum_number_of_people INT NOT NULL,
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS pictures (
  picture_id SERIAL PRIMARY KEY,
  room_id INT NOT NULL,
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS roles (
  id SERIAL PRIMARY KEY,
  role VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS room_members (
  room_id INT NOT NULL,
  user_id INT NOT NULL,
  role_id INT NOT NULL,
  PRIMARY KEY (room_id, user_id),
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS request_statuses (
  id SERIAL PRIMARY KEY,
  status_info VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS room_requests (
  id SERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  room_id INT NOT NULL,
  status_id INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_room_requests_user_room UNIQUE (user_id, room_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (status_id) REFERENCES request_statuses(id)
);

CREATE TABLE IF NOT EXISTS bans (
  user_id INT NOT NULL,
  room_id INT NOT NULL,
  PRIMARY KEY (user_id, room_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS questions (
  id SERIAL PRIMARY KEY,
  owner INT NOT NULL,
  question TEXT NOT NULL,
  category_id INT NOT NULL,
  FOREIGN KEY (category_id) REFERENCES categories(id),
  FOREIGN KEY (owner) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS answers (
  id SERIAL PRIMARY KEY,
  question_id INT NOT NULL,
  prev_ans_id INT,
  answer TEXT,
  owner INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (question_id) REFERENCES questions(id),
  FOREIGN KEY (owner) REFERENCES users(id),
  FOREIGN KEY (prev_ans_id) REFERENCES answers(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id SERIAL PRIMARY KEY,
  notification VARCHAR(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS user_notifications (
  user_id INT NOT NULL,
  notification_id INT NOT NULL,
  PRIMARY KEY (user_id, notification_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (notification_id) REFERENCES notifications(id)
);

CREATE TABLE IF NOT EXISTS bulletin_board (
  id SERIAL PRIMARY KEY,
  room_id INT NOT NULL,
  content TEXT,
  author_id INT NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (author_id) REFERENCES users(id)
);

INSERT INTO roles (role) VALUES
('Owner'),
('Admin'),
('Participant')
ON CONFLICT DO NOTHING;

INSERT INTO categories (name) VALUES
('Sport'),
('Music'),
('Art'),
('Entertainments'),
('Business'),
('Education'),
('ActiveRecreation'),
('PassiveRecreation'),
('MassEvent'),
('Other'),
('NotSpecified')
ON CONFLICT DO NOTHING;

INSERT INTO request_statuses (status_info) VALUES
('Consideration'),
('Accepted'),
('Refused'),
('RefusedWithBan')
ON CONFLICT DO NOTHING;

INSERT INTO notifications (notification) VALUES
('MembershipAccepted'),
('MembershipRejected'),
('ActivityClosed'),
('NewJoinRequest')
ON CONFLICT DO NOTHING;
