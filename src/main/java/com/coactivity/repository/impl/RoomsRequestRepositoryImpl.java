package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.repository.RoomsRequestRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RoomsRequestRepositoryImpl implements RoomsRequestRepository {

  private final DataRepository dataRepository;

  public RoomsRequestRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @Override
  public RoomsRequest createRequest(int userId, int roomId, int statusId) {
    String sql = """
            INSERT INTO rooms_requests (user_id, room_id, status, created_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            RETURNING id, user_id, room_id, status, created_at
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      statement.setInt(3, statusId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error creating request: " + e.getMessage());
      throw new RuntimeException("Failed to create request", e);
    }
    throw new RuntimeException("Failed to create request");
  }

  @Override
  public RoomsRequest updateRequest(int requestId, RequestStatus status) {
    String sql = """
            UPDATE rooms_requests
            SET status = ?
            WHERE id = ?
            RETURNING id, user_id, room_id, status, created_at;
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, status.name());
      statement.setInt(2, requestId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public void deleteRequest(int requestId) {
    String sql = "DELETE FROM rooms_requests WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, requestId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException("Request not found with id: " + requestId);
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
  }

  @Override
  public List<RoomsRequest> getRoomRequest(int roomId) {
    String sql = """
            SELECT * FROM Rooms_requests
            WHERE room_id = ?;
            """;

    List<RoomsRequest> requests = new ArrayList<>();

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          requests.add(mapResultSetToRoomsRequest(resultSet));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return requests;
  }

  @Override
  public List<RoomsRequest> getRequestsByUser(int userId) {
    String sql = """
            SELECT * FROM Rooms_requests
            WHERE user_id = ?;
            """;

    List<RoomsRequest> requests = new ArrayList<>();

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          requests.add(mapResultSetToRoomsRequest(resultSet));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return requests;
  }

  private RoomsRequest mapResultSetToRoomsRequest(ResultSet resultSet) throws SQLException {
    int userId = resultSet.getInt("user_id");
    int roomId = resultSet.getInt("room_id");
    RequestStatus status = RequestStatus.valueOf(resultSet.getString("status"));
    Instant createdAt = resultSet.getTimestamp("created_at").toInstant();

    return new RoomsRequest(userId, roomId, createdAt, status);
  }
}