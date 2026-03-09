package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxEventRepositoryImpl {

  private static final int MAX_ERROR_LENGTH = 2000;

  private final DataRepository dataRepository;

  public OutboxEventRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public void enqueueEvent(Connection connection, String eventType, String payloadJson,
      String idempotencyKey) throws SQLException {
    String sql = """
        INSERT INTO outbox_events (event_type, payload, status, idempotency_key)
        VALUES (?, CAST(? AS jsonb), 'NEW', ?)
        ON CONFLICT (idempotency_key) DO NOTHING
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, eventType);
      statement.setString(2, payloadJson);
      statement.setString(3, idempotencyKey);
      statement.executeUpdate();
    }
  }

  public List<OutboxEventRecord> claimNextBatch(int batchSize, int maxRetries) {
    String sql = """
        WITH candidates AS (
            SELECT id
            FROM outbox_events
            WHERE status IN ('NEW', 'FAILED')
              AND available_at <= CURRENT_TIMESTAMP
              AND retry_count < ?
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT ?
        )
        UPDATE outbox_events oe
        SET status = 'PROCESSING',
            updated_at = CURRENT_TIMESTAMP
        FROM candidates c
        WHERE oe.id = c.id
        RETURNING oe.id, oe.event_type, oe.payload::text, oe.retry_count, oe.idempotency_key
        """;

    List<OutboxEventRecord> events = new ArrayList<>();

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, maxRetries);
      statement.setInt(2, batchSize);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          events.add(new OutboxEventRecord(
              resultSet.getLong("id"),
              resultSet.getString("event_type"),
              resultSet.getString("payload"),
              resultSet.getInt("retry_count"),
              resultSet.getString("idempotency_key")));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim outbox events", e);
    }

    return events;
  }

  public void markSent(long eventId) {
    String sql = """
        UPDATE outbox_events
        SET status = 'SENT',
            processed_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """;

    executeStatusUpdate(sql, eventId);
  }

  public void markFailed(long eventId, String errorMessage, int retryDelaySeconds) {
    String sql = """
        UPDATE outbox_events
        SET status = 'FAILED',
            retry_count = retry_count + 1,
            last_error = ?,
            available_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """;

    String safeError = truncateError(errorMessage);

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, safeError);
      statement.setInt(2, Math.max(retryDelaySeconds, 1));
      statement.setLong(3, eventId);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to mark outbox event as failed: " + eventId, e);
    }
  }

  private void executeStatusUpdate(String sql, long eventId) {
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, eventId);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update outbox event status: " + eventId, e);
    }
  }

  private String truncateError(String value) {
    if (value == null) {
      return null;
    }
    if (value.length() <= MAX_ERROR_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_LENGTH);
  }

  public record OutboxEventRecord(
      long id,
      String eventType,
      String payload,
      int retryCount,
      String idempotencyKey) {
  }
}
