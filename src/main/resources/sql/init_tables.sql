-- todo Написать скрипт для создания всех таблиц. Заполнить таблицы со статическими данными
CREATE TABLE Users (
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

CREATE TABLE Categories (
    id SERIAl PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

CREATE TABLE Rooms (
  id SERIAl PRIMARY KEY,
  is_active BOOLEAN NOT NULL
  is_visible BOOLEAN NOT NULL,
  chat_link VARCHAR(100),
  category_id INT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  age_rating INT,
--  owner_id INT,
  frequency INT,
  maximum_number_of_people int,
  current_number_of_people int,
--  FOREIGN KEY (owner_id) REFERENCES Users(id)
  FOREIGN KEY (category_id) REFERENCES Categories(id)
);


CREATE TABLE Pictures (
    picture_id SERIAl PRIMARY KEY,
    room_id INT NOT NULL,
    FOREIGN KEY (room_id) REFERENCES Rooms(id)
);

CREATE TABLE Roles (
    id SERIAl NOT NULL,
    role VARCHAR(50),
);

CREATE TABLE Rooms_members (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    role_id INT,
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (role_id) REFERENCES Roles(id)
);

CREATE TABLE Statuses(
    id SERIAl PRIMARY KEY,
    status_info VARCHAR(50) NOT NULL
);

CREATE TABLE Rooms_requests(
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    status_id INT NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (status_id) REFERENCES Statuses(status)
);

CREATE TABLE Bans(
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    durationOfBan TIME NOT NULL,
    ban_date DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (room_id) REFERENCES Rooms(id)
);


CREATE TABLE Questions(
    id SERIAl PRIMARY KEY,
    owner INT NOT NULL,
    question TEXT NOT NULL,
    category_id int
    FOREIGN KEY (category_id) REFERENCES Categories(id)
    FOREIGN KEY (owner) REFERENCES Users(id)
);

CREATE TABLE Answers(
    id SERIAl PRIMARY KEY,
    question_id INT NOT NULL,
    prev_ans_id INT,
    answer TEXT,
    owner INT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (owner) REFERENCES Users(id)
);

create table Subscriptions (
    user_id int,
    owner_id int
    FOREIGN KEY (user_id) REFERENCES Users(id)
    FOREIGN KEY (owner_id) REFERENCES Users(id)
);

create table Notifications (
    id serial PRIMARY KEY,
    notification varchar(50),
);

create table BulletinBoard (
    id SERIAL PRIMARY KEY,
    room_id int
    content TEXT,
    author_id int,
    Date updatedAt,
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (author_id) REFERENCES Users(id)
);

Insert into Roles(role) values
(Admin),
(Participant),
(Owner);

Insert into Categories(name) values
(Sport),
(Music),
(Art),
(Entertainments),
(Business),
(Education),
(ActiveRecreation),
(PassiveRecreation),
(isAMassEvent),
(Other),
(NotSpecified);


Insert into Statuses(status_info) values
(Consideration),
(Accepted),
(Refused);

Insert into Notifications(notification) values
(membershipAccepted),
(membershipRejected),
(activityClosed),
(newJoinRequest);