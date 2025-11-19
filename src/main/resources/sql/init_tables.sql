CREATE TABLE IF NOT EXISTS  Users (
  id SERIAl PRIMARY KEY,
  login VARCHAR(100) NOT NULL,
  username VARCHAR(20) UNIQUE NOT NULL,
  password VARCHAR(128) NOT NULL,
  birthday DATE,
  country VARCHAR(100),
  city VARCHAR(100),
  description TEXT,
  avatar_id INT
);

CREATE TABLE IF NOT EXISTS  Categories (
    id SERIAl PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS  Rooms (
  id SERIAl PRIMARY KEY,
  is_active BOOLEAN NOT NULL,
  is_public BOOLEAN NOT NULL,
  chat_link VARCHAR(100),
  category_id INT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  age_rating INT,
  frequency TIMESTAMP,
  maximum_number_of_people int,
  FOREIGN KEY (category_id) REFERENCES Categories(id)
);


CREATE TABLE IF NOT EXISTS  Pictures (
    picture_id SERIAl PRIMARY KEY,
    room_id INT NOT NULL,
    FOREIGN KEY (room_id) REFERENCES Rooms(id)
);

CREATE TABLE IF NOT EXISTS  Roles (
    id SERIAl NOT NULL UNIQUE,
    role VARCHAR(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS  Rooms_members (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    role_id INT,
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (role_id) REFERENCES Roles(id)
);

CREATE TABLE IF NOT EXISTS  RequestStatuses(
    id SERIAl PRIMARY KEY,
    status_info VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS  Rooms_requests(
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    status_id INT NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (status_id) REFERENCES RequestStatuses(id)
);

CREATE TABLE IF NOT EXISTS  Bans(
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (room_id) REFERENCES Rooms(id)
);


CREATE TABLE IF NOT EXISTS  Questions(
    id SERIAl PRIMARY KEY,
    owner INT NOT NULL,
    question TEXT NOT NULL,
    category_id int,
    FOREIGN KEY (category_id) REFERENCES Categories(id),
    FOREIGN KEY (owner) REFERENCES Users(id)
);

CREATE TABLE IF NOT EXISTS  Answers(
    id SERIAl PRIMARY KEY,
    question_id INT NOT NULL,
    prev_ans_id INT,
    answer TEXT,
    owner INT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (owner) REFERENCES Users(id)
);



create table IF NOT EXISTS  Notifications (
    id serial PRIMARY KEY,
    notification varchar(50)
);

create table IF NOT EXISTS  usersNotification (
    user_id int,
    notification_id int,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (notification_id) REFERENCES Notifications(id)
);

create table IF NOT EXISTS  BulletinBoard (
    id SERIAL PRIMARY KEY,
    room_id int,
    content TEXT,
    author_id int,
    updated_at Date,
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (author_id) REFERENCES Users(id)
);
--
--Insert into Roles(role) values
--('Admin'),
--('Participant'),
--('Owner');
--
--INSERT INTO Categories(name) VALUES
--('Sport'),
--('Music'),
--('Art'),
--('Entertainments'),
--('Business'),
--('Education'),
--('ActiveRecreation'),
--('PassiveRecreation'),
--('isAMassEvent'),
--('Other'),
--('NotSpecified');
--
--INSERT INTO RequestStatuses(status_info) VALUES
--('Consideration'),
--('Accepted'),
--('Refused');
--
--INSERT INTO Notifications(notification) VALUES
--('membershipAccepted'),
--('membershipRejected'),
--('activityClosed'),
--('newJoinRequest');