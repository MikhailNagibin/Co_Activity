package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;

import java.sql.*;

public class UserRepositoryImpl implements UserRepository {

    private final DataRepository dataRepository;

    public UserRepositoryImpl(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public User createUser(String login, String username, String password, Date birthday, String country, String city, String description, int avatar_id){
        String sql = "INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setDate(4, birthday);
            statement.setString(5, country);
            statement.setString(6, city);
            statement.setString(7, description);
            statement.setInt(8, avatar_id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userID = resultSet.getInt("id");
                    return new User(userID, login, username, password, birthday, country, city, description, avatar_id);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException("Failed to create user", e);
        }
        throw new RuntimeException("User creation failed - no ID returned");
    }

    @Override
    public User updateUser(String login, String username, String password, Date birthday, String country, String city, String description, int avatar_id){
        String sql = "UPDATE Users SET login = ?, username = ?, password = ?, birthday = ?, country = ?, city = ?, description = ?, avatar_id = ? WHERE id = ? RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setDate(4, birthday);
            statement.setString(5, country);
            statement.setString(6, city);
            statement.setString(7, description);
            statement.setInt(8, avatar_id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userID = resultSet.getInt("id");
                    return new User(userID, login, username, password, birthday, country, city, description, avatar_id);
                }
            }

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
            throw new RuntimeException();

        }
    }

    @Override
    public User getUser(String login, String password){
        String sql = "SELECT * FROM Users WHERE login = ? AND password = ?";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
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
      int avatarId = resultSet.getInt("avatar_id");
      return new User(userId, login, password, birthday, country, city, description, avatarId);

    }
}
