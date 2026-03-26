//package com.coactivity;
//
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.sql.Connection;
//import java.sql.Statement;
//import javax.sql.DataSource;
//import org.springframework.core.io.ClassPathResource;
//
//class DatabaseResetTool {
//
//  public static void main(String[] args) {
//    try {
//      System.out.println("Начинаю полный сброс базы данных...");
//
//      // Создаем DataRepository для получения DataSource
//      DataRepository dataRepository = new DataRepository();
//      DataSource dataSource = dataRepository.getDataSource();
//
//      // Выполняем сброс
//      resetDatabase(dataSource);
//
//      System.out.println("База данных успешно сброшена и создана заново!");
//
//    } catch (Exception e) {
//      System.err.println("Ошибка при сбросе базы данных:");
//      e.printStackTrace();
//      System.exit(1);
//    }
//  }
//
//  private static void resetDatabase(DataSource dataSource) throws Exception {
//    try (Connection connection = dataSource.getConnection();
//         Statement statement = connection.createStatement()) {
//
//      System.out.println("Подключение к базе данных установлено");
//
//      // 1. Отключаем проверку внешних ключей
//      statement.execute("SET session_replication_role = 'replica';");
//
//      // 2. Удаляем все таблицы в правильном порядке
//      System.out.println("Удаление таблиц...");
//
//      String[] dropTables = {
//        "DROP TABLE IF EXISTS usersNotification CASCADE;",
//        "DROP TABLE IF EXISTS Notifications CASCADE;",
//        "DROP TABLE IF EXISTS Rooms_requests CASCADE;",
//        "DROP TABLE IF EXISTS RequestStatuses CASCADE;",
//        "DROP TABLE IF EXISTS Bans CASCADE;",
//        "DROP TABLE IF EXISTS Rooms_members CASCADE;",
//        "DROP TABLE IF EXISTS Roles CASCADE;",
//        "DROP TABLE IF EXISTS Answers CASCADE;",
//        "DROP TABLE IF EXISTS Questions CASCADE;",
//        "DROP TABLE IF EXISTS BulletinBoard CASCADE;",
//        "DROP TABLE IF EXISTS Pictures CASCADE;",
//        "DROP TABLE IF EXISTS Rooms CASCADE;",
//        "DROP TABLE IF EXISTS Categories CASCADE;",
//        "DROP TABLE IF EXISTS Users CASCADE;"
//      };
//
//      for (String dropTable : dropTables) {
//        statement.execute(dropTable);
//      }
//
//      // 3. Включаем проверку внешних ключей обратно
//      statement.execute("SET session_replication_role = 'origin';");
//
//      // 4. Создаем таблицы заново из init_complete.sql
//      System.out.println("Создание таблиц...");
//      String initScript = loadInitScript();
//
//      // Разделяем скрипт на отдельные SQL-запросы
//      String[] sqlStatements = initScript.split(";");
//
//      for (String sql : sqlStatements) {
//        String trimmedSql = sql.trim();
//        if (!trimmedSql.isEmpty() && !trimmedSql.startsWith("--")) {
//          statement.execute(trimmedSql + ";");
//        }
//      }
//
//      System.out.println("Вставка начальных данных...");
//
//    } catch (Exception e) {
//      throw new Exception("Ошибка при выполнении SQL операций: " + e.getMessage(), e);
//    }
//  }
//
//  private static String loadInitScript() throws Exception {
//    try (InputStream inputStream = new ClassPathResource("init_tables.sql").getInputStream()) {
//      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//    } catch (Exception e) {
//      // Если файл не найден, используем встроенный скрипт
//      System.out.println("Файл init_complete.sql не найден, использую встроенный скрипт");
//      return getBuiltInInitScript();
//    }
//  }
//
//  private static String getBuiltInInitScript() {
//    return """
//            CREATE TABLE IF NOT EXISTS Users (
//              id SERIAL PRIMARY KEY,
//              login VARCHAR(100) NOT NULL,
//              username VARCHAR(20) UNIQUE NOT NULL,
//              password VARCHAR(128) NOT NULL,
//              birthday TIMESTAMP,
//              country VARCHAR(100),
//              city VARCHAR(100),
//              description TEXT,
//              avatar_id INT
//            );
//
//            CREATE TABLE IF NOT EXISTS Categories (
//                id SERIAL PRIMARY KEY,
//                name VARCHAR(20) NOT NULL UNIQUE
//            );
//
//            CREATE TABLE IF NOT EXISTS Rooms (
//              id SERIAL PRIMARY KEY,
//              is_active BOOLEAN NOT NULL,
//              is_public BOOLEAN NOT NULL,
//              chat_link VARCHAR(100),
//              category_id INT,
//              name VARCHAR(100) NOT NULL,
//              description TEXT,
//              start_date TIMESTAMP,
//              end_date TIMESTAMP,
//              age_rating INT,
//              frequency TIMESTAMP,
//              maximum_number_of_people INT,
//              FOREIGN KEY (category_id) REFERENCES Categories(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Pictures (
//                picture_id SERIAL PRIMARY KEY,
//                room_id INT NOT NULL,
//                FOREIGN KEY (room_id) REFERENCES Rooms(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Roles (
//                id SERIAL PRIMARY KEY,
//                role VARCHAR(50) UNIQUE NOT NULL
//            );
//
//            CREATE TABLE IF NOT EXISTS Rooms_members (
//                room_id INT NOT NULL,
//                user_id INT NOT NULL,
//                role_id INT,
//                PRIMARY KEY (room_id, user_id),
//                FOREIGN KEY (room_id) REFERENCES Rooms(id),
//                FOREIGN KEY (user_id) REFERENCES Users(id),
//                FOREIGN KEY (role_id) REFERENCES Roles(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS RequestStatuses(
//                id SERIAL PRIMARY KEY,
//                status_info VARCHAR(50) NOT NULL UNIQUE
//            );
//
//            CREATE TABLE IF NOT EXISTS Rooms_requests(
//                id SERIAL PRIMARY KEY,
//                user_id INT NOT NULL,
//                room_id INT NOT NULL,
//                status_id INT NOT NULL DEFAULT 1,
//                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                CONSTRAINT uq_rooms_requests_user_room UNIQUE (user_id, room_id),
//                FOREIGN KEY (user_id) REFERENCES Users(id),
//                FOREIGN KEY (room_id) REFERENCES Rooms(id),
//                FOREIGN KEY (status_id) REFERENCES RequestStatuses(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Bans(
//                user_id INT NOT NULL,
//                room_id INT NOT NULL,
//                PRIMARY KEY (user_id, room_id),
//                FOREIGN KEY (user_id) REFERENCES Users(id),
//                FOREIGN KEY (room_id) REFERENCES Rooms(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Questions(
//                id SERIAL PRIMARY KEY,
//                owner INT NOT NULL,
//                question TEXT NOT NULL,
//                category_id INT,
//                FOREIGN KEY (category_id) REFERENCES Categories(id),
//                FOREIGN KEY (owner) REFERENCES Users(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Answers(
//                id SERIAL PRIMARY KEY,
//                question_id INT NOT NULL,
//                prev_ans_id INT,
//                answer TEXT,
//                owner INT NOT NULL,
//                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                FOREIGN KEY (question_id) REFERENCES Questions(id),
//                FOREIGN KEY (owner) REFERENCES Users(id),
//                FOREIGN KEY (prev_ans_id) REFERENCES Answers(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS Notifications (
//                id SERIAL PRIMARY KEY,
//                notification VARCHAR(50) UNIQUE
//            );
//
//            CREATE TABLE IF NOT EXISTS usersNotification (
//                user_id INT NOT NULL,
//                notification_id INT NOT NULL,
//                PRIMARY KEY (user_id, notification_id),
//                FOREIGN KEY (user_id) REFERENCES Users(id),
//                FOREIGN KEY (notification_id) REFERENCES Notifications(id)
//            );
//
//            CREATE TABLE IF NOT EXISTS BulletinBoard (
//                id SERIAL PRIMARY KEY,
//                room_id INT,
//                content TEXT,
//                author_id INT,
//                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                FOREIGN KEY (room_id) REFERENCES Rooms(id),
//                FOREIGN KEY (author_id) REFERENCES Users(id)
//            );
//
//            INSERT INTO Categories(name) VALUES
//            ('Sport'),
//            ('Music'),
//            ('Art'),
//            ('Entertainments'),
//            ('Business'),
//            ('Education'),
//            ('ActiveRecreation'),
//            ('PassiveRecreation'),
//            ('MassEvent'),
//            ('Other'),
//            ('NotSpecified');
//
//            INSERT INTO RequestStatuses(status_info) VALUES
//            ('Consideration'),
//            ('Accepted'),
//            ('Refused'),
//            ('RefusedWithBan');
//
//            INSERT INTO Notifications(notification) VALUES
//            ('MembershipAccepted'),
//            ('MembershipRejected'),
//            ('ActivityClosed'),
//            ('NewJoinRequest');
//            """;
//  }
//}