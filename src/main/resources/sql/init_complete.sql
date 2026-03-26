-- Создание таблиц
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     login VARCHAR(255) UNIQUE NOT NULL,
    user_name VARCHAR(50),
    password VARCHAR(128) NOT NULL,
    data_of_birth TIMESTAMP,
    country VARCHAR(100),
    city VARCHAR(100),
    description VARCHAR(1000),
    avatar_id INTEGER
    );

CREATE TABLE IF NOT EXISTS categories (
                                          id SERIAL PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS roles (
                                     id SERIAL PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE,
    permissions VARCHAR(100)
    );

CREATE TABLE IF NOT EXISTS request_statuses (
                                                id SERIAL PRIMARY KEY,
                                                name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS notifications (
                                             id SERIAL PRIMARY KEY,
                                             name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS rooms (
                                     id SERIAL PRIMARY KEY,
                                     is_active BOOLEAN DEFAULT TRUE,
                                     is_public BOOLEAN NOT NULL,
                                     chat_link VARCHAR(255),
    category_id INTEGER REFERENCES categories(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    date_of_start_event TIMESTAMP,
    date_of_end_event TIMESTAMP,
    age_rating INTEGER DEFAULT 0,
    frequency TIMESTAMP,
    maximum_number_of_people INTEGER NOT NULL
    );

CREATE TABLE IF NOT EXISTS room_members (
                                            id SERIAL PRIMARY KEY,
                                            room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id)
    );

CREATE TABLE IF NOT EXISTS rooms_requests (
                                              id SERIAL PRIMARY KEY,
                                              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_id INTEGER NOT NULL REFERENCES request_statuses(id),
    UNIQUE(user_id, room_id)
    );

CREATE TABLE IF NOT EXISTS bans (
                                    id SERIAL PRIMARY KEY,
                                    room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(room_id, user_id)
    );

CREATE TABLE IF NOT EXISTS questions (
                                         id SERIAL PRIMARY KEY,
                                         owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question VARCHAR(2000) NOT NULL,
    category_id INTEGER REFERENCES categories(id)
    );

CREATE TABLE IF NOT EXISTS answers (
                                       id SERIAL PRIMARY KEY,
                                       question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    previous_answer_id INTEGER REFERENCES answers(id),
    answer VARCHAR(2000),
    owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS pictures (
                                        id SERIAL PRIMARY KEY,
                                        room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    photo_id INTEGER
    );

CREATE TABLE IF NOT EXISTS bulletin_boards (
                                               id SERIAL PRIMARY KEY,
                                               room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    content VARCHAR(2000),
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_notifications (
                                                  id SERIAL PRIMARY KEY,
                                                  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_id INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    UNIQUE(user_id, notification_id)
    );

-- Вставка начальных данных
INSERT INTO roles (name, permissions) VALUES
                                          ('Owner', 'Owner'),
                                          ('Admin', 'Admin'),
                                          ('Participant', 'Participant')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO request_statuses (name, description) VALUES
                                                     ('Consideration', 'Pending review'),
                                                     ('Accepted', 'Request accepted'),
                                                     ('Refused', 'Request refused'),
                                                     ('RefusedWithBan', 'Refused and user banned')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO notifications (name, description) VALUES
                                                  ('membershipAccepted', 'Membership request accepted'),
                                                  ('membershipRejected', 'Membership request rejected'),
                                                  ('activityClosed', 'Activity closed'),
                                                  ('newJoinRequest', 'New join request')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name) VALUES
                                  ('SPORT'), ('MUSIC'), ('ART'), ('ENTERTAINMENT'), ('BUSINESS'), ('EDUCATION')
    ON CONFLICT (name) DO NOTHING;
