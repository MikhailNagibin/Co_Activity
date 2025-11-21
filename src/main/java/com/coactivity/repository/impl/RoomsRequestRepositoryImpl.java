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

  public RoomsRequestRepositoryImpl(DataRepository dataRepository, RoomRepositoryImpl roomRepositoryImpl, UserRepositoryImpl userRepositoryImpl) {
    this.dataRepository = dataRepository;
    this.roomRepository = roomRepositoryImpl;
    this.userRepository = userRepositoryImpl;
  }

  @Override
  public RoomsRequest createRequest(int userId, int roomId, RequestStatus status) {
    String sql = """
            INSERT INTO rooms_requests (user_id, room_id, status_id, created_at)
            VALUES (?, ?, (SELECT id FROM RequestStatuses WHERE status_info = ?), CURRENT_TIMESTAMP)
            RETURNING id, user_id, room_id,
                     (SELECT status_info FROM RequestStatuses WHERE id = status_id) AS status_info,
                     created_at
            """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      statement.setString(3, status.name());

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoomsRequest(resultSet);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public RoomsRequest updateRequest(int requestId, RequestStatus status) {
    String sql = """
            UPDATE rooms_requests
            SET status_id = (SELECT id FROM RequestStatuses WHERE status_info = ?)
            WHERE id = ?
            RETURNING id, user_id, room_id,
                     (SELECT status_info FROM RequestStatuses WHERE id = status_id) AS status_info,
                     created_at;
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
      System.err.println(e.getMessage());
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
            WHERE rr.room_id = ?;
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
            WHERE rr.user_id = ?;
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
            WHERE rr.id = ?;
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return null;
  }

  private RoomsRequest mapResultSetToRoomsRequest(ResultSet resultSet) throws SQLException {
    int requestId = resultSet.getInt("id");
    int userId = resultSet.getInt("user_id");
    int roomId = resultSet.getInt("room_id");
    String statusInfo = resultSet.getString("status_info");
    RequestStatus status = RequestStatus.valueOf(statusInfo.trim().toUpperCase());
    Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
    Instant createdAt = createdAtTimestamp != null ? createdAtTimestamp.toInstant() : null;

    return new RoomsRequest(
        requestId,
        userRepository.getUserById(userId),
        roomRepository.getRoomById(roomId),
        createdAt,
        status);
  }
}