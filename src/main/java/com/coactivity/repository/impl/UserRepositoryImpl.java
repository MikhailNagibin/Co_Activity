package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.*;
import com.coactivity.repository.UserRepository;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.domain.Notification;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {

    private final DataRepository dataRepository;
    private final RoomRepositoryImpl roomRepository;

    public UserRepositoryImpl(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
        this.roomRepository = new RoomRepositoryImpl(dataRepository);
    }

    @Override
    public User createUser(String login, String username, String password, Instant birthday, String country, String city, String description, int avatarId){
        String sql = "INSERT INTO Users (login, password, birthday, country, city, description, avatar_id, username) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp newBirthday = Timestamp.from(birthday);
            statement.setString(1, login);
            statement.setString(2, password);
            statement.setTimestamp(3, newBirthday);
            statement.setString(4, country);
            statement.setString(5, city);
            statement.setString(6, description);
            statement.setInt(7, avatarId);
            statement.setString(8, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("id");
                    return new User(userId, login, username, password, birthday, country, city, description, avatarId,
                      List.of(), List.of());
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    @Override
    public void updateUser(User user, String password, Instant birthday, String country,
                           String city, String description, int avatarId, String username){
        String sql = "UPDATE Users SET username = ?, password = ?, birthday = ?, country = ?, " +
          "city = ?, description = ?, avatar_id = ? WHERE id = ?;";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String newPassword = password != null ? password : user.getPassword();
            String newUsername = password != null ? password : user.getUsername();

            Timestamp newBirthday = birthday != null ? Timestamp.from(birthday) :
                                                       Timestamp.from(user.getDataOfBirth());
            String newCountry = country != null ? country : user.getCountry();
            String newCity = city != null ? city : user.getCity();
            String newDescription = description != null ? description : user.getDescription();
            int newAvatarId = avatarId != 0 ? avatarId : user.getAvatarId();
            statement.setString(1, newUsername);
            statement.setString(2, newPassword);
            statement.setTimestamp(3, newBirthday);
            statement.setString(4, newCountry);
            statement.setString(5, newCity);
            statement.setString(6, newDescription);
            statement.setInt(7, newAvatarId);

            user.setPassword(newPassword);
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
    public void deleteUser(int id) {
        String sql = """
          DELETE FROM Rooms_members WHERE user_id = ?;
          DELETE FROM Rooms_requests WHERE user_id = ?;
          DELETE FROM Bans WHERE user_id = ?;
          DELETE FROM Questions WHERE user_id = ?;
          DELETE FROM Answers WHERE user_id = ?;
          DELETE FROM Subscriptions WHERE user_id = ?;
          DELETE FROM BulletinBoard WHERE user_id = ?;
          DELETE FROM Users WHERE id = ?;
          """;
        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
          for (int i = 1; i <= 8; i++) {
            statement.setInt(i, id);
          }
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new RuntimeException();
            }

        } catch (SQLException e) {
          System.err.println(e.getMessage());
            throw new RuntimeException();

        }
    }

    @Override
    public User getUser(int login, String password){
        String sql = "SELECT * FROM Users WHERE login = ? AND password = ?";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, login);
            statement.setString(2, password);
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

    public User getUserById(int id) {
      String sql = "SELECT * FROM user WHERE id = ?";
      try (Connection connection = dataRepository.getDataSource().getConnection();
           PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setInt(1, id);
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

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException{
      int userId = resultSet.getInt("id");
      String login = resultSet.getString("login");
      String password = resultSet.getString("password");
      Instant birthday = resultSet.getTimestamp("birthday").toInstant();
      String country = resultSet.getString("country");
      String city = resultSet.getString("city");
      String description = resultSet.getString("description");
      String username = resultSet.getString("username");
      int avatarId = resultSet.getInt("avatar_id");

      return new User(userId, login, username, password, birthday, country, city, description, avatarId,
        getRooms(userId), getNotification(userId));
    }

    private List<Room> getRooms(int userId) {
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

    private List<Notification> getNotification(int userId) {
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
}
