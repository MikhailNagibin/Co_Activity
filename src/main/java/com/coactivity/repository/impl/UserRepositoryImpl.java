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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
      statement.setInt(7, request.getAvatarId());
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException();
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

      Timestamp newBirthday =
          request.getDateOfBirth() != null ? Timestamp.from(request.getDateOfBirth()) :
              Timestamp.from(user.getDataOfBirth());
      String newCountry = request.getCountry() != null ? request.getCountry() : user.getCountry();
      String newCity = request.getCity() != null ? request.getCity() : user.getCity();
      String newDescription =
          request.getDescription() != null ? request.getDescription() : user.getDescription();
      Integer newAvatarId = request.getAvatarId() != null ? request.getAvatarId() : user.getAvatarId();
      statement.setString(1, newUsername);
      statement.setTimestamp(2, newBirthday);
      statement.setString(3, newCountry);
      statement.setString(4, newCity);
      statement.setString(5, newDescription);
      statement.setInt(6, newAvatarId);
      statement.executeUpdate();

      user.setDataOfBirth(newBirthday.toInstant());
      user.setCountry(newCountry);
      user.setCity(newCity);
      user.setDescription(newDescription);
      user.setAvatarId(newAvatarId);

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public void deleteUser(Integer userId) {
    String sql = """
        DELETE FROM Rooms_members WHERE user_id = ?;
        DELETE FROM Rooms_requests WHERE user_id = ?;
        DELETE FROM Bans WHERE user_id = ?;
        DELETE FROM Questions WHERE owner = ?;
        DELETE FROM Answers WHERE owner = ?;
        DELETE FROM usersNotification WHERE user_id = ?;
        DELETE FROM BulletinBoard WHERE author_id = ?;
        DELETE FROM Users WHERE id = ?;
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 1; i <= 8; i++) {
        statement.setInt(i, userId);
      }
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException("No user did not delete");
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();

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
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return null;
  }

  public void setNotification(Integer id, Notification notification) {
    String sql = """
        Insert into usersNotification (user_id, notification_id)  values (?,
        (select id from Notifications where notification = ?));
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.setString(2, notification.toString());
      statement.executeUpdate();

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
        throw new RuntimeException();
      }
    } catch (SQLException | NoSuchAlgorithmException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
    Integer userId = resultSet.getInt("id");
    String login = resultSet.getString("login");
    String password = resultSet.getString("password");
    Instant birthday = resultSet.getTimestamp("birthday").toInstant();
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return rooms;
  }

  private List<Notification> getNotification(Integer userId) {
    String sql = "select notification_id from usersNotification where user_id = ?";
    var notifications = new ArrayList<Notification>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          Notification notification = Notification.getByIndex(resultSet.getInt(1));
          notifications.add(notification);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }
}
