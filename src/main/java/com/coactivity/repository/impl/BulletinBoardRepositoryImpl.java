package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.repository.BulletinBoardRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class BulletinBoardRepositoryImpl implements BulletinBoardRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;

  public BulletinBoardRepositoryImpl(DataRepository dataRepository,
      UserRepositoryImpl userRepository,
      RoomRepositoryImpl roomRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
  }

  @Override
  public BulletinBoard createBulletinBoard(Integer roomId, String content, Integer authorId) {
    String sql = "INSERT INTO BulletinBoard (room_id, content, author_id, updated_at) " +
        "VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id, updated_at";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      statement.setString(2, content);
      statement.setInt(3, authorId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer boardId = resultSet.getInt("id");
          Instant createdAt = resultSet.getTimestamp("updated_at").toInstant();
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
  public BulletinBoard updateBulletinBoard(Integer roomId, String content, Integer authorId) {
    String sql =
        "UPDATE BulletinBoard SET content = ?, author_id = ?, updated_at = CURRENT_TIMESTAMP" +
            " WHERE room_id = ? RETURNING id, updated_at";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, content);
      statement.setInt(2, authorId);
      statement.setInt(3, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer boardId = resultSet.getInt("id");
          Instant updatedAt = resultSet.getTimestamp("updated_at").toInstant();
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
  public boolean isBulletinBoardExists(Integer roomId) {
    String sql = "SELECT COUNT(*) FROM BulletinBoard WHERE room_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int count = resultSet.getInt(1);
          return count > 0;
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException("Failed to check bulletin board existence for room: " + roomId, e);
    }
    return false;
  }

  @Override
  public BulletinBoard getBulletinBoard(Integer roomId) {
    String sql = "SELECT * FROM BulletinBoard WHERE room_id = ?";

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
  public void deleteBulletinBoard(Integer roomId) {
    String sql = "DELETE FROM BulletinBoard WHERE room_id = ?";

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
    Integer id = resultSet.getInt("id");
    Integer roomId = resultSet.getInt("room_id");
    String content = resultSet.getString("content");
    Integer authorId = resultSet.getInt("author_id");
    Instant updatedAt = resultSet.getTimestamp("updated_at").toInstant();

    return new BulletinBoard(id, roomRepository.getRoomById(roomId), content,
        userRepository.getUserById(authorId), updatedAt);
  }
}