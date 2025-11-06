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
    public User createUser(String login, String password, String birthday, String country, String city, String description, int avatar_id){
        String sql = "INSERT INTO Users (login, password, birthday, country, city, description, avatar_id) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, birthday);
            statement.setString(4, country);
            statement.setString(5, city);
            statement.setString(6, description);
            statement.setInt(7, avatar_id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userID = resultSet.getInt("id");
                    return new User(userID, login, password, birthday, country, city, description, avatar_id);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException("Failed to create user", e);
        }
        throw new RuntimeException("User creation failed - no ID returned");
    }

    @Override
    public void updateUser(String login, String password, String birthday, String country, String city, String description, int avatar_id){
        String sql = "UPDATE Users SET login = ?, username = ?, password = ?, birthday = ?, country = ?, city = ?, description = ?, avatar_id = ? WHERE id = ? RETURNING id";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, birthday);
            statement.setString(4, country);
            statement.setString(5, city);
            statement.setString(6, description);
            statement.setInt(7, avatar_id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userID = resultSet.getInt("id");
                    return new User(userID, login, password, birthday, country, city, description, avatar_id);
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
        String sql = "DELETE FROM Users WHERE id = ?";
        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new RuntimeException();
            }

        } catch (SQLException e) {
            throw new RuntimeException();

        }
    }


    @Override
    public User getUser(int login, String password){
        String sql = "SELECT * FROM Users WHERE login = ? AND password = ?";

        try (Connection connection = dataRepository.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToBulletinBoard(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return null;
    }
}
