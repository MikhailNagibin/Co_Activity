package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.Notification;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  private final DataRepository dataRepository;
  private final RoomRepositoryImpl roomRepository;

  public UserRepositoryImpl(DataRepository dataRepository, @Lazy RoomRepositoryImpl roomRepository) {
    this.dataRepository = dataRepository;
    this.roomRepository = roomRepository;
  }

  private static String sha256(String input) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(input.getBytes());
    return bytesToHex(hash);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  @Override
  public User createUser(UserRegistrationRequest request) {
    String sql =
        "INSERT INTO Users (login, password, birthday, city, country, description, avatar_id, username) "
            +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      Timestamp newBirthday = Timestamp.from(request.getDateOfBirth());

      statement.setString(1, request.getLogin());
      statement.setString(2, sha256(request.getPassword()));
      statement.setTimestamp(3, newBirthday);
      statement.setString(4, request.getCity());
      statement.setString(5, request.getCountry());
      statement.setString(6, request.getDescription());
      if (request.getAvatarId() != null) {
        statement.setInt(7, request.getAvatarId());
      } else {
        statement.setNull(7, Types.INTEGER);
      }
      statement.setString(8, request.getUserName());

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer userId = resultSet.getInt("id");
          return new User(userId, request.getLogin(), request.getUserName(), request.getPassword(),
              request.getDateOfBirth(), request.getCity(), request.getCountry(),
              request.getDescription(), request.getAvatarId(),
              List.of(), List.of());
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create user with login: " + request.getLogin(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to hash password for user creation", e);
    }
    throw new RuntimeException("Failed to create user: insert returned no id");
  }

  @Override
  public void updateUser(Integer userId, UserProfileUpdateRequest request) {
    String sql = "UPDATE Users SET username = ?, birthday = ?, country = ?, " +
        "city = ?, description = ?, avatar_id = ? WHERE id = ?;";

    User user = getUserById(userId);

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      String newUsername =
          request.getUsername() != null ? request.getUsername() : user.getUserName();

      Timestamp newBirthday;
      if (request.getDateOfBirth() != null) {
        newBirthday = Timestamp.from(request.getDateOfBirth());
      } else if (user.getDataOfBirth() != null) {
        newBirthday = Timestamp.from(user.getDataOfBirth());
      } else {
        newBirthday = null;
      }
      String newCountry = request.getCountry() != null ? request.getCountry() : user.getCountry();
      String newCity = request.getCity() != null ? request.getCity() : user.getCity();
      String newDescription =
          request.getDescription() != null ? request.getDescription() : user.getDescription();
      Integer newAvatarId =
          request.getAvatarId() != null ? request.getAvatarId() : user.getAvatarId();
      statement.setString(1, newUsername);
      statement.setTimestamp(2, newBirthday);
      statement.setString(3, newCountry);
      statement.setString(4, newCity);
      statement.setString(5, newDescription);
      if (newAvatarId != null) {
        statement.setInt(6, newAvatarId);
      } else {
        statement.setNull(6, Types.INTEGER);
      }
      statement.setInt(7, userId);
      int affectedRows = statement.executeUpdate();
      if (affectedRows == 0) {
        throw new RuntimeException("User not found for update: " + userId);
      }

      user.setDataOfBirth(newBirthday != null ? newBirthday.toInstant() : null);
      user.setUserName(newUsername);
      user.setCountry(newCountry);
      user.setCity(newCity);
      user.setDescription(newDescription);
      user.setAvatarId(newAvatarId);
      return;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to update user with id: " + userId, e);
    }
  }

  @Override
  public void deleteUser(Integer userId) {
    String[] sqlStatements = {
        "DELETE FROM Rooms_members WHERE user_id = ?",
        "DELETE FROM Rooms_requests WHERE user_id = ?",
        "DELETE FROM Bans WHERE user_id = ?",
        "DELETE FROM Questions WHERE owner = ?",
        "DELETE FROM Answers WHERE owner = ?",
        "DELETE FROM usersNotification WHERE user_id = ?",
        "DELETE FROM BulletinBoard WHERE author_id = ?",
        "DELETE FROM Users WHERE id = ?"
    };
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      for (int i = 0; i < sqlStatements.length - 1; i++) {
        try (PreparedStatement statement = connection.prepareStatement(sqlStatements[i])) {
          statement.setInt(1, userId);
          statement.executeUpdate();
        }
      }
      int affectedRows;
      try (PreparedStatement deleteUserStatement = connection.prepareStatement(
          sqlStatements[sqlStatements.length - 1])) {
        deleteUserStatement.setInt(1, userId);
        affectedRows = deleteUserStatement.executeUpdate();
      }
      if (affectedRows == 0) {
        connection.rollback();
        throw new RuntimeException("User not found for delete: " + userId);
      }
      connection.commit();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete user with id: " + userId, e);
    }
  }

  @Override
  public User getUser(String login, String password) {
    String sql = "SELECT * FROM Users WHERE login = ? AND password = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, login);
      statement.setString(2, sha256(password));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToUser(resultSet);
        }
      }

    } catch (SQLException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to get user by login credentials", e);
    }
    return null;
  }


  public User getUserByLogin(String login) {
    String sql = "SELECT * FROM Users WHERE login = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, login);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToUser(resultSet);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException("Failed to get user by login: " + login, e);
    }
    return null;
  }

  @Override
  public User getUserById(Integer userId) {
    String sql = "SELECT * FROM Users WHERE id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToUser(resultSet);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException("Failed to get user with id: " + userId, e);
    }
    return null;
  }

  public void setNotification(Integer id, Notification notification) {
    String sql = """
        INSERT INTO usersNotification (user_id, notification_id)
        VALUES (?, (SELECT id FROM Notifications WHERE LOWER(notification) = LOWER(?)))
        ON CONFLICT (user_id, notification_id) DO NOTHING;
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.setString(2, notification.toString());
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to enable notification " + notification + " for user " + id, e);
    }
  }

  public void removeNotification(Integer id, Notification notification) {
    String sql = """
        DELETE FROM usersNotification
        WHERE user_id = ?
        AND notification_id = (SELECT id FROM Notifications WHERE LOWER(notification) = LOWER(?));
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.setString(2, notification.toString());
      statement.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to disable notification " + notification + " for user " + id, e);
    }
  }

  public void updatePassword(Integer userId, String newPassword) {
    String sql = "UPDATE Users SET password = ? WHERE id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, sha256(newPassword));
      statement.setInt(2, userId);
      int affectedRows = statement.executeUpdate();
      if (affectedRows == 0) {
        throw new RuntimeException("User not found for password update: " + userId);
      }
    } catch (SQLException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to update password for user: " + userId, e);
    }
  }

  private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
    Integer userId = resultSet.getInt("id");
    String login = resultSet.getString("login");
    String password = resultSet.getString("password");
    Timestamp birthdayTimestamp = resultSet.getTimestamp("birthday");
    Instant birthday = birthdayTimestamp != null ? birthdayTimestamp.toInstant() : null;
    String country = resultSet.getString("country");
    String city = resultSet.getString("city");
    String description = resultSet.getString("description");
    String username = resultSet.getString("username");
    Integer avatarId = resultSet.getInt("avatar_id");

    return new User(userId, login, username, password, birthday, country, city, description,
        avatarId,
        getRooms(userId), getNotification(userId));
  }

  private List<Room> getRooms(Integer userId) {
    String sql = "select room_id from Rooms_members where user_id = ?";
    var rooms = new ArrayList<Room>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          Room room = roomRepository.getRoomById(resultSet.getInt(1));
          rooms.add(room);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get rooms for user: " + userId, e);
    }
    return rooms;
  }

  private List<Notification> getNotification(Integer userId) {
    String sql = """
        SELECT n.notification
        FROM usersNotification un
        JOIN Notifications n ON n.id = un.notification_id
        WHERE un.user_id = ?
        """;
    var notifications = new ArrayList<Notification>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          Notification notification = Notification.fromValue(resultSet.getString(1));
          notifications.add(notification);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get notifications for user: " + userId, e);
    }
    return notifications;
  }

  public List<Integer> getAllUsers() {
    String sql = "SELECT id FROM Users";
    var rooms = new ArrayList<Integer>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        rooms.add(resultSet.getInt("id"));
      }
      return rooms;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get user ids", e);
    }
  }
}
