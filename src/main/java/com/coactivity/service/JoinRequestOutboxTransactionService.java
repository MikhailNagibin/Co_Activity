package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.repository.impl.OutboxEventRepositoryImpl;
import com.coactivity.service.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Executes join-request state change and outbox enqueue in one DB transaction.
 */
@Service
public class JoinRequestOutboxTransactionService {

  static final String EVENT_MEMBERSHIP_ACCEPTED = "membership.accepted";
  static final String EVENT_MEMBERSHIP_REJECTED = "membership.rejected";

  private static final String STATUS_CONSIDERATION = RequestStatus.CONSIDERATION.toDatabaseValue();

  private final DataRepository dataRepository;
  private final OutboxEventRepositoryImpl outboxEventRepository;
  private final ObjectMapper objectMapper;

  public JoinRequestOutboxTransactionService(DataRepository dataRepository,
      OutboxEventRepositoryImpl outboxEventRepository,
      ObjectMapper objectMapper) {
    this.dataRepository = dataRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  public void processDecision(Integer requestId, Integer roomId, Integer requesterId, Room room,
      RequestStatus action) {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      connection.setAutoCommit(false);

      try {
        switch (action) {
          case ACCEPTED -> handleAccepted(connection, requestId, roomId, requesterId, room);
          case REFUSED -> handleRefused(connection, requestId, roomId, requesterId, room,
              RequestStatus.REFUSED);
          case REFUSED_WITH_BAN -> handleRefusedWithBan(connection, requestId, roomId, requesterId,
              room);
          default -> throw new ValidationException("Unsupported outbox action: " + action);
        }

        connection.commit();
      } catch (Exception e) {
        rollbackQuietly(connection);
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }

    } catch (SQLException e) {
      throw new RuntimeException("Failed to process join request with outbox", e);
    }
  }

  private void handleAccepted(Connection connection, Integer requestId, Integer roomId,
      Integer requesterId, Room room) throws SQLException {
    int currentParticipants = getRoomParticipantCount(connection, roomId);
    if (currentParticipants >= room.getMaximumNumberOfPeople()) {
      throw new ValidationException("Room capacity exceeded");
    }

    addUserToRoomIfMissing(connection, roomId, requesterId, Role.PARTICIPANT);
    updateRequestStatus(connection, requestId, RequestStatus.ACCEPTED);

    enqueueMembershipEvent(connection, requestId, requesterId, room.getName(),
        EVENT_MEMBERSHIP_ACCEPTED, RequestStatus.ACCEPTED);
  }

  private void handleRefused(Connection connection, Integer requestId, Integer roomId,
      Integer requesterId, Room room, RequestStatus status) throws SQLException {
    updateRequestStatus(connection, requestId, status);

    enqueueMembershipEvent(connection, requestId, requesterId, room.getName(),
        EVENT_MEMBERSHIP_REJECTED, status);
  }

  private void handleRefusedWithBan(Connection connection, Integer requestId, Integer roomId,
      Integer requesterId, Room room) throws SQLException {
    addBanIfMissing(connection, roomId, requesterId);
    handleRefused(connection, requestId, roomId, requesterId, room, RequestStatus.REFUSED_WITH_BAN);
  }

  private int getRoomParticipantCount(Connection connection, Integer roomId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM Rooms_members WHERE room_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1);
        }
      }
    }
    return 0;
  }

  private void addUserToRoomIfMissing(Connection connection, Integer roomId, Integer userId,
      Role role) throws SQLException {
    String sql = """
        INSERT INTO Rooms_members (room_id, user_id, role_id)
        SELECT ?, ?, r.id
        FROM Roles r
        WHERE LOWER(r.role) = LOWER(?)
        ON CONFLICT (room_id, user_id) DO NOTHING
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.setString(3, role.name());
      statement.executeUpdate();
    }
  }

  private void addBanIfMissing(Connection connection, Integer roomId, Integer userId)
      throws SQLException {
    String sql = """
        INSERT INTO Bans (room_id, user_id)
        VALUES (?, ?)
        ON CONFLICT (user_id, room_id) DO NOTHING
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.executeUpdate();
    }
  }

  private void updateRequestStatus(Connection connection, Integer requestId, RequestStatus status)
      throws SQLException {
    String sql = """
        UPDATE rooms_requests
        SET status_id = (SELECT id FROM RequestStatuses WHERE status_info = ?)
        WHERE id = ?
          AND status_id = (SELECT id FROM RequestStatuses WHERE status_info = ?)
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, status.toDatabaseValue());
      statement.setInt(2, requestId);
      statement.setString(3, STATUS_CONSIDERATION);

      int affectedRows = statement.executeUpdate();
      if (affectedRows == 0) {
        throw new ValidationException("Join request already processed");
      }
    }
  }

  private void enqueueMembershipEvent(Connection connection, Integer requestId, Integer userId,
      String roomName, String eventType, RequestStatus action) throws SQLException {
    String idempotencyKey = "join-request:%d:%s".formatted(requestId, action.name());
    String payloadJson = toPayloadJson(userId, roomName);

    outboxEventRepository.enqueueEvent(connection, eventType, payloadJson, idempotencyKey);
  }

  private String toPayloadJson(Integer userId, String roomName) {
    Map<String, Object> payload = Map.of(
        "userId", userId,
        "roomName", roomName
    );

    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize outbox payload", e);
    }
  }

  private void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ignored) {
      // no-op
    }
  }
}
