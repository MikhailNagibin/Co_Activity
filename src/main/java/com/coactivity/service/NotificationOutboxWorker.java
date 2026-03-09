package com.coactivity.service;

import com.coactivity.repository.impl.OutboxEventRepositoryImpl;
import com.coactivity.repository.impl.OutboxEventRepositoryImpl.OutboxEventRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxWorker {

  private static final Logger log = LoggerFactory.getLogger(NotificationOutboxWorker.class);

  private final OutboxEventRepositoryImpl outboxEventRepository;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  @Value("${outbox.enabled:false}")
  private boolean outboxEnabled;

  @Value("${outbox.batch-size:20}")
  private int batchSize;

  @Value("${outbox.max-retries:5}")
  private int maxRetries;

  @Value("${outbox.retry-delay-seconds:30}")
  private int retryDelaySeconds;

  public NotificationOutboxWorker(OutboxEventRepositoryImpl outboxEventRepository,
      NotificationService notificationService,
      ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:3000}")
  public void publishPendingEvents() {
    if (!outboxEnabled) {
      return;
    }

    List<OutboxEventRecord> events =
        outboxEventRepository.claimNextBatch(Math.max(batchSize, 1), Math.max(maxRetries, 1));

    if (events.isEmpty()) {
      return;
    }

    for (OutboxEventRecord event : events) {
      processEvent(event);
    }
  }

  private void processEvent(OutboxEventRecord event) {
    try {
      MembershipPayload payload = parseMembershipPayload(event.payload());
      boolean delivered = switch (event.eventType()) {
        case JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED ->
            notificationService.sendMembershipAcceptedSync(payload.userId(), payload.roomName());
        case JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_REJECTED ->
            notificationService.sendMembershipRejectedSync(payload.userId(), payload.roomName());
        default -> throw new IllegalStateException("Unsupported outbox event type: "
            + event.eventType());
      };

      if (delivered) {
        outboxEventRepository.markSent(event.id());
      } else {
        outboxEventRepository.markFailed(event.id(),
            "Delivery failed for event type: " + event.eventType(), retryDelaySeconds);
      }
    } catch (IllegalArgumentException | IllegalStateException ex) {
      log.warn("Outbox event {} failed validation/dispatch: {}", event.id(), ex.getMessage());
      outboxEventRepository.markFailed(event.id(), ex.getMessage(), retryDelaySeconds);
    } catch (Exception ex) {
      log.error("Failed to process outbox event {}", event.id(), ex);
      outboxEventRepository.markFailed(event.id(), ex.getMessage(), retryDelaySeconds);
    }
  }

  private MembershipPayload parseMembershipPayload(String rawPayload) throws Exception {
    JsonNode root = objectMapper.readTree(rawPayload);

    if (!root.hasNonNull("userId") || !root.hasNonNull("roomName")) {
      throw new IllegalArgumentException("Outbox payload must contain userId and roomName");
    }

    int userId = root.get("userId").asInt();
    String roomName = root.get("roomName").asText();

    if (userId <= 0 || roomName == null || roomName.isBlank()) {
      throw new IllegalArgumentException("Outbox payload contains invalid userId/roomName");
    }

    return new MembershipPayload(userId, roomName);
  }

  private record MembershipPayload(Integer userId, String roomName) {
  }
}
