CREATE TABLE IF NOT EXISTS user_avatars (
  id SERIAL PRIMARY KEY,
  storage_key VARCHAR(255) NOT NULL UNIQUE,
  original_filename VARCHAR(255),
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  login VARCHAR(255) NOT NULL,
  email_normalized VARCHAR(255) NOT NULL,
  username VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  email_verified_at TIMESTAMP,
  birthday TIMESTAMP,
  country VARCHAR(100),
  city VARCHAR(100),
  description TEXT,
  avatar_id INT,
  avatar_file_id INT,
  CONSTRAINT fk_users_avatar_file
    FOREIGN KEY (avatar_file_id) REFERENCES user_avatars(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized
  ON users (email_normalized);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower
  ON users (LOWER(username));

CREATE TABLE IF NOT EXISTS categories (
  id SERIAL PRIMARY KEY,
  name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rooms (
  id SERIAL PRIMARY KEY,
  status VARCHAR(20) NOT NULL,
  is_public BOOLEAN NOT NULL,
  chat_link VARCHAR(100),
  category_id INT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  city VARCHAR(100),
  country VARCHAR(100),
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  age_rating INT NOT NULL,
  frequency TIMESTAMP,
  maximum_number_of_people INT NOT NULL,
  CONSTRAINT chk_rooms_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED')),
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX IF NOT EXISTS idx_rooms_city_lower
  ON rooms (LOWER(city))
  WHERE city IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rooms_country_lower
  ON rooms (LOWER(country))
  WHERE country IS NOT NULL;

CREATE TABLE IF NOT EXISTS pictures (
  picture_id SERIAL PRIMARY KEY,
  room_id INT NOT NULL,
  storage_key VARCHAR(255),
  original_filename VARCHAR(255),
  content_type VARCHAR(100),
  size_bytes BIGINT,
  sort_order INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pictures_storage_key_not_null
  ON pictures (storage_key)
  WHERE storage_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pictures_room_sort_order_not_null
  ON pictures (room_id, sort_order)
  WHERE storage_key IS NOT NULL AND sort_order IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pictures_room_sort_order
  ON pictures (room_id, sort_order);

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
('NewJoinRequest'),
('ImportantRoomUpdates')
ON CONFLICT DO NOTHING;
