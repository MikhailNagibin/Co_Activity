package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.repository.BulletinBoardRepository;
import java.sql.*;
import java.time.Instant;


public class BulletinBoardRepositoryImpl implements BulletinBoardRepository {

  private final DataRepository dataRepository;

  public BulletinBoardRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @Override
  public BulletinBoard createBulletinBoard(int roomId, String content, int authorId, Instant updatedAt) {
    String sql = "INSERT INTO bulletinboard (room_id, content, author_id, updated_at) VALUES (?, ?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      statement.setString(2, content);
      statement.setInt(3, authorId);
      statement.setTimestamp(4, Timestamp.from(updatedAt));

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int boardId = resultSet.getInt("id");
          return new BulletinBoard(boardId, roomId, content, authorId, updatedAt);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public BulletinBoard updateBulletinBoard(int roomId, String content, int authorId, Instant updatedAt) {
    String sql = "UPDATE bulletinboard SET content = ?, author_id = ?, updated_at = ? WHERE room_id = ? RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, content);
      statement.setInt(2, authorId);
      statement.setTimestamp(3, Timestamp.from(updatedAt));
      statement.setInt(4, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int boardId = resultSet.getInt("id");
          return new BulletinBoard(boardId, roomId, content, authorId, updatedAt);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public BulletinBoard getBulletinBoard(int roomId) {
    String sql = "SELECT * FROM bulletinboard WHERE room_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

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

  @Override
  public void deleteBulletinBoard(int roomId) {
    String sql = "DELETE FROM bulletinboard WHERE room_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  private BulletinBoard mapResultSetToBulletinBoard(ResultSet resultSet) throws SQLException {
    int id = resultSet.getInt("id");
    int roomId = resultSet.getInt("room_id");
    String content = resultSet.getString("content");
    int authorId = resultSet.getInt("author_id");
    Instant updatedAt = resultSet.getTimestamp("updatedat").toInstant();

    return new BulletinBoard(id, roomId, content, authorId, updatedAt);
  }
}