package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Ban;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.domain.Room;
import com.coactivity.repository.UserRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {

    private final DataRepository dataRepository;

    public UserRepositoryImpl(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public User createUser(String login, String username, String password, Instant birthday, String country, String city, String description, int avatarId){
        String sql = "INSERT INTO Users (login, password, birthday, country, city, description, avatar_id) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp newBirthday = java.sql.Timestamp.from(birthday);
            statement.setString(1, login);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setTimestamp(4, newBirthday);
            statement.setString(5, country);
            statement.setString(6, city);
            statement.setString(7, description);
            statement.setInt(8, avatarId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("id");
                    return new User(userId, login, username, password, birthday, country, city, description, avatarId,
                      List.of(), List.of(), List.of());
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    @Override
    public void updateUser(User user, String login, String password, Instant birthday, String country,
                           String city, String description, int avatarId){
        String sql = "UPDATE Users SET login = ?, username = ?, password = ?, birthday = ?, country = ?, " +
          "city = ?, description = ?, avatar_id = ? WHERE id = ?;";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String newLogin = login != null ? login : user.getLogin();
            String newPassword = password != null ? password : user.getPassword();
            Timestamp newBirthday = birthday != null ? java.sql.Timestamp.from(birthday) :
                                                       java.sql.Timestamp.from(user.getDataOfBirth());
            String newCountry = country != null ? country : user.getCountry();
            String newCity = city != null ? city : user.getCity();
            String newDescription = description != null ? description : user.getDescription();
            int newAvatarId = avatarId != 0 ? avatarId : user.getAvatarId();

            statement.setString(1, newLogin);
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

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException{
      int userId = resultSet.getInt("id");
      String login = resultSet.getString("login");
      String username = resultSet.getString("username");
      String password = resultSet.getString("password");
      Instant birthday = resultSet.getTimestamp("birthday").toInstant();
      String country = resultSet.getString("country");
      String city = resultSet.getString("city");
      String description = resultSet.getString("description");
      int avatarId = resultSet.getInt("avatar_id");
      return new User(userId, login, username, password, birthday, country, city, description, avatarId);
    }
}
