CREATE TABLE IF NOT EXISTS users (
  id SERIAl PRIMARY KEY,
  login VARCHAR(100) NOT NULL,
  username VARCHAR(20) UNIQUE NOT NULL,
  password VARCHAR(128) NOT NULL,
  birthday TIMESTAMP,
  country VARCHAR(100),
  city VARCHAR(100),
  description TEXT,
  avatar_id INT,
  avatar_file_id INT
);

CREATE TABLE IF NOT EXISTS user_avatars (
  id SERIAL PRIMARY KEY,
  storage_key VARCHAR(255) NOT NULL UNIQUE,
  original_filename VARCHAR(255),
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id SERIAl PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rooms (
  id SERIAl PRIMARY KEY,
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
  maximum_number_of_people int NOT NULL,
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Note: The code uses 'is_private' in RoomRepositoryImpl but SQL has 'is_public'
-- This is a logic mismatch - the code should be fixed to use 'is_public' or SQL should use 'is_private'
-- Currently keeping 'is_public' in SQL as it matches the domain model field name


CREATE TABLE IF NOT EXISTS pictures (
    picture_id SERIAl PRIMARY KEY,
    room_id INT NOT NULL,
    storage_key VARCHAR(255),
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    sort_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAl PRIMARY KEY,
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

CREATE TABLE IF NOT EXISTS request_statuses(
    id SERIAl PRIMARY KEY,
    status_info VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS room_requests(
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

CREATE TABLE IF NOT EXISTS bans(
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    PRIMARY KEY (user_id, room_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);


CREATE TABLE IF NOT EXISTS questions(
    id SERIAl PRIMARY KEY,
    owner INT NOT NULL,
    question TEXT NOT NULL,
    category_id int NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (owner) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS answers(
    id SERIAl PRIMARY KEY,
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
    id serial PRIMARY KEY,
    notification varchar(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS user_notifications (
    user_id int NOT NULL,
    notification_id int NOT NULL,
    PRIMARY KEY (user_id, notification_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (notification_id) REFERENCES notifications(id)
);

CREATE TABLE IF NOT EXISTS bulletin_board (
    id SERIAL PRIMARY KEY,
    room_id int NOT NULL,
    content TEXT,
    author_id int NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_users_avatar_file'
      AND table_name = 'users'
  ) THEN
    ALTER TABLE users
      ADD CONSTRAINT fk_users_avatar_file
      FOREIGN KEY (avatar_file_id) REFERENCES user_avatars(id);
  END IF;
END $$;


--Insert into roles(role) values
--('Admin'),
--('Participant'),
--('Owner');

--INSERT INTO categories(name) VALUES
--('Sport'),
--('Music'),
--('Art'),
--('Entertainments'),
--('Business'),
--('Education'),
--('ActiveRecreation'),
--('PassiveRecreation'),
--('MassEvent'),
--('Other'),
--('NotSpecified');

--INSERT INTO request_statuses(status_info) VALUES
--('Consideration'),
--('Accepted'),
--('Refused'),
--('RefusedWithBan');

-- INSERT INTO notifications(notification) VALUES
-- ('membershipAccepted'),
-- ('membershipRejected'),
-- ('activityClosed'),
-- ('newJoinRequest')
-- ON CONFLICT (notification) DO NOTHING;
