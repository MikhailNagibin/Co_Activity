package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.repository.BulletinBoardRepository;
import java.sql.*;
import java.time.Instant;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import jakarta.persistence.criteria.CriteriaBuilder;


public class BulletinBoardRepositoryImpl implements BulletinBoardRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;

  public BulletinBoardRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.roomRepository = new RoomRepositoryImpl(dataRepository);
    this.userRepository = new UserRepositoryImpl(dataRepository);
  }

  @Override
  public BulletinBoard createBulletinBoard(int roomId, String content, int authorId) {
    String sql = "INSERT INTO bulletinboard (room_id, content, author_id, updated_at) " +
      "VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id, CURRENT_TIMESTAMP";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      statement.setString(2, content);
      statement.setInt(3, authorId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int boardId = resultSet.getInt("id");
          Instant createdAt = resultSet.getTimestamp(2).toInstant();
          return new BulletinBoard(boardId, roomRepository.getRoomById(roomId),
            content, userRepository.getUserById(authorId), createdAt);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public BulletinBoard updateBulletinBoard(int roomId, String content, int authorId) {
    String sql = "UPDATE bulletinboard SET content = ?, author_id = ?, updated_at = CURRENT_TIMESTAMP" +
      " WHERE room_id = ? RETURNING id CURRENT_TIMESTAMP";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, content);
      statement.setInt(2, authorId);
      statement.setInt(3, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int boardId = resultSet.getInt("id");
          Instant updatedAt = resultSet.getTimestamp(2).toInstant();
          return new BulletinBoard(boardId, roomRepository.getRoomById(roomId), content,
            userRepository.getUserById(authorId), updatedAt);
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
    Instant updatedAt = resultSet.getTimestamp("updated_at").toInstant();

    return new BulletinBoard(id, roomRepository.getRoomById(roomId), content,
      userRepository.getUserById(authorId), updatedAt);
  }
}