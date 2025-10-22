-- todo Написать скрипт для создания всех таблиц. Заполнить таблицы со статическими данными
CREATE TABLE Users (
  id INT PRIMARY KEY,
  login VARCHAR(100) NOT NULL,
  username VARCHAR(20) UNIQUE NOT NULL,
  password VARCHAR(128) NOT NULL,
  birthday DATE,
  country VARCHAR(100),
  city VARCHAR(100),
  description TEXT,
  avatar_id INT
);

CREATE TABLE Rooms (
  id INT PRIMARY KEY,
  is_visible BOOLEAN NOT NULL,
  chat_link VARCHAR(100),
  category_id INT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  start_date DATE,
  end_date DATE,
  age_rating INT,
  owner_id INT NOT NULL,
  frequency INT,
  FOREIGN KEY (owner_id) REFERENCES Users(id)
);

CREATE TABLE Categories (
    id INT PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

CREATE TABLE Pictures (
    picture_id INT PRIMARY KEY,
    room_id INT NOT NULL,
    FOREIGN KEY (room_id) REFERENCES Rooms(id)
);

CREATE TABLE Roles (
    user_id INT NOT NULL,
    role VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Rooms_members (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    role_id INT,
    FOREIGN KEY (room_id) REFERENCES Rooms(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (role_id) REFERENCES Roles(role)
);

CREATE TABLE Statuses(
    id INT PRIMARY KEY,
    status_info VARCHAR(50) NOT NULL
);

CREATE TABLE Rooms_requests(
    id INT PRIMARY KEY,
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    status_id INT NOT NULL,
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
    id INT PRIMARY KEY,
    owner INT NOT NULL UNIQUE,
    question TEXT NOT NULL,
    FOREIGN KEY (owner) REFERENCES Users(id)
);

CREATE TABLE Answers(
    id INT PRIMARY KEY,
    question_id INT NOT NULL,
    prev_ans_id INT UNIQUE,
    answer TEXT,
    owner INT NOT NULL UNIQUE,
    FOREIGN KEY (question_id) REFERENCES Questions(id),
    FOREIGN KEY (owner) REFERENCES Users(id)
);









