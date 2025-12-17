package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.repository.RoomsRequestRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RoomsRequestRepositoryImpl implements RoomsRequestRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;

  public RoomsRequestRepositoryImpl(DataRepository dataRepository,
                                    RoomRepositoryImpl roomRepositoryImpl,
                                    UserRepositoryImpl userRepositoryImpl) {
    this.dataRepository = dataRepository;
    this.roomRepository = roomRepositoryImpl;
    this.userRepository = userRepositoryImpl;
  }

  @Override
  public RoomsRequest createRequest(int userId, int roomId, RequestStatus status) {
    // Получаем ID статуса
    int statusId = getStatusIdByRequestStatus(status);

    String sql = """
            INSERT INTO rooms_requests (user_id, room_id, status_id, created_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            RETURNING id, user_id, room_id, status_id, created_at
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      statement.setInt(3, statusId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int requestId = resultSet.getInt("id");
          int resultUserId = resultSet.getInt("user_id");
          int resultRoomId = resultSet.getInt("room_id");
          int resultStatusId = resultSet.getInt("status_id");
          Timestamp createdAt = resultSet.getTimestamp("created_at");

          // Получаем статус по ID
          RequestStatus requestStatus = getRequestStatusById(resultStatusId);

          return new RoomsRequest(
            requestId,
            userRepository.getUserById(resultUserId),
            roomRepository.getRoomById(resultRoomId),
            createdAt.toInstant(),
            requestStatus);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error creating request: " + e.getMessage());
      // Добавляем дополнительную информацию об ошибке
      System.err.println("SQL: " + sql);
      System.err.println("Parameters: userId=" + userId + ", roomId=" + roomId + ", statusId=" + statusId);
      throw new RuntimeException("Failed to create rooms request: " + e.getMessage(), e);
    }
    throw new RuntimeException("Failed to create rooms request - no result returned");
  }

  private RequestStatus getRequestStatusById(int statusId) {
    String sql = "SELECT status_info FROM RequestStatuses WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, statusId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String statusInfo = resultSet.getString("status_info");
          return RequestStatus.fromDatabase(statusInfo);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error getting status by ID: " + e.getMessage());
      throw new RuntimeException("Failed to get status for id: " + statusId, e);
    }

    throw new RuntimeException("Status not found for id: " + statusId);
  }

  private int getStatusIdByRequestStatus(RequestStatus status) {
    String sql = "SELECT id FROM RequestStatuses WHERE status_info = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      String dbValue = status.toDatabaseValue();
      statement.setString(1, dbValue);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }

    } catch (SQLException e) {
      System.err.println("Error getting status ID: " + e.getMessage());
      System.err.println("Looking for status: " + status.toDatabaseValue());
      throw new RuntimeException("Failed to get status ID for: " + status.toDatabaseValue(), e);
    }

    // Попробуем найти статус по имени без учета регистра
    return getStatusIdByCaseInsensitive(status.toDatabaseValue());
  }

  private int getStatusIdByCaseInsensitive(String statusValue) {
    String sql = "SELECT id FROM RequestStatuses WHERE LOWER(status_info) = LOWER(?)";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, statusValue);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }

    } catch (SQLException e) {
      System.err.println("Error getting status ID (case insensitive): " + e.getMessage());
      throw new RuntimeException("Failed to get status ID for: " + statusValue, e);
    }

    throw new RuntimeException("Status not found: " + statusValue);
  }

  @Override
  public RoomsRequest updateRequest(int requestId, RequestStatus status) {
    String sql = """
            WITH updated AS (
                UPDATE rooms_requests
                SET status_id = (SELECT id FROM RequestStatuses WHERE status_info = ?)
                WHERE id = ?
                RETURNING id, user_id, room_id, status_id, created_at
            )
            SELECT
                u.id,
                u.user_id,
                u.room_id,
                rs.status_info,
                u.created_at
            FROM updated u
            JOIN RequestStatuses rs ON rs.id = u.status_id
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, status.toDatabaseValue());
      statement.setInt(2, requestId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error updating request: " + e.getMessage());
      throw new RuntimeException("Failed to update request with id: " + requestId, e);
    }
    throw new RuntimeException("Request not found with id: " + requestId);
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
      System.err.println("Error deleting request: " + e.getMessage());
      throw new RuntimeException("Failed to delete request with id: " + requestId, e);
    }
  }

  @Override
  public List<RoomsRequest> getRoomRequests(int roomId) {
    String sql = """
            SELECT rr.id,
                   rr.user_id,
                   rr.room_id,
                   rs.status_info,
                   rr.created_at
            FROM rooms_requests rr
            INNER JOIN RequestStatuses rs ON rs.id = rr.status_id
            WHERE rr.room_id = ?
            ORDER BY rr.created_at DESC
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
      System.err.println("Error getting room requests: " + e.getMessage());
      throw new RuntimeException("Failed to get requests for room: " + roomId, e);
    }
    return requests;
  }

  @Override
  public List<RoomsRequest> getRequestsByUser(int userId) {
    String sql = """
            SELECT rr.id,
                   rr.user_id,
                   rr.room_id,
                   rs.status_info,
                   rr.created_at
            FROM rooms_requests rr
            INNER JOIN RequestStatuses rs ON rs.id = rr.status_id
            WHERE rr.user_id = ?
            ORDER BY rr.created_at DESC
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
      System.err.println("Error getting user requests: " + e.getMessage());
      throw new RuntimeException("Failed to get requests for user: " + userId, e);
    }
    return requests;
  }

  @Override
  public RoomsRequest getRequestById(int requestId) {
    String sql = """
            SELECT rr.id,
                   rr.user_id,
                   rr.room_id,
                   rs.status_info,
                   rr.created_at
            FROM rooms_requests rr
            INNER JOIN RequestStatuses rs ON rs.id = rr.status_id
            WHERE rr.id = ?
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, requestId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error getting request by id: " + e.getMessage());
      throw new RuntimeException("Failed to get request with id: " + requestId, e);
    }
    return null;
  }

  public RoomsRequest getRequestByUserAndRoom(int userId, int roomId) {
    String sql = """
            SELECT rr.id,
                   rr.user_id,
                   rr.room_id,
                   rs.status_info,
                   rr.created_at
            FROM rooms_requests rr
            INNER JOIN RequestStatuses rs ON rs.id = rr.status_id
            WHERE rr.user_id = ? AND rr.room_id = ?
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setInt(2, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      System.err.println("Error getting request by user and room: " + e.getMessage());
      throw new RuntimeException("Failed to get request for user: " + userId + " and room: " + roomId, e);
    }
    return null;
  }

  private RoomsRequest mapResultSetToRoomsRequest(ResultSet resultSet) throws SQLException {
    try {
      int requestId = resultSet.getInt("id");
      int userId = resultSet.getInt("user_id");
      int roomId = resultSet.getInt("room_id");
      String statusInfo = resultSet.getString("status_info");

      RequestStatus status = RequestStatus.fromDatabase(statusInfo);

      Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
      Instant createdAt = createdAtTimestamp != null ?
        createdAtTimestamp.toInstant() : Instant.now();

      return new RoomsRequest(
        requestId,
        userRepository.getUserById(userId),
        roomRepository.getRoomById(roomId),
        createdAt,
        status);

    } catch (Exception e) {
      throw new SQLException("Failed to map result set to RoomsRequest", e);
    }
  }
}