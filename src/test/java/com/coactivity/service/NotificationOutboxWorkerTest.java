package com.coactivity.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.coactivity.repository.impl.OutboxEventRepositoryImpl;
import com.coactivity.repository.impl.OutboxEventRepositoryImpl.OutboxEventRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxWorker Tests")
class NotificationOutboxWorkerTest {

  @Mock
  private OutboxEventRepositoryImpl outboxEventRepository;

  @Mock
  private NotificationService notificationService;

  private NotificationOutboxWorker worker;

  @BeforeEach
  void setUp() {
    worker = new NotificationOutboxWorker(outboxEventRepository, notificationService,
        new ObjectMapper());

    ReflectionTestUtils.setField(worker, "outboxEnabled", true);
    ReflectionTestUtils.setField(worker, "batchSize", 20);
    ReflectionTestUtils.setField(worker, "maxRetries", 5);
    ReflectionTestUtils.setField(worker, "retryDelaySeconds", 30);
  }

  @Test
  @DisplayName("Should skip processing when outbox is disabled")
  void disabledOutbox_skipsProcessing() {
    ReflectionTestUtils.setField(worker, "outboxEnabled", false);

    worker.publishPendingEvents();

    verifyNoInteractions(outboxEventRepository, notificationService);
  }

  @Test
  @DisplayName("Should mark event as sent for delivered membership accepted")
  void acceptedDelivered_marksSent() {
    OutboxEventRecord event = new OutboxEventRecord(
        101L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED,
        "{\"userId\":42,\"roomName\":\"Chess Club\"}",
        0,
        "join-request:101:ACCEPTED"
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));
    when(notificationService.sendMembershipAcceptedSync(42, "Chess Club")).thenReturn(true);

    worker.publishPendingEvents();

    verify(notificationService).sendMembershipAcceptedSync(42, "Chess Club");
    verify(outboxEventRepository).markSent(101L);
    verify(outboxEventRepository, never()).markFailed(anyLong(), anyString(), anyInt());
  }

  @Test
  @DisplayName("Should mark event as failed when delivery returns false")
  void rejectedNotDelivered_marksFailed() {
    OutboxEventRecord event = new OutboxEventRecord(
        102L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_REJECTED,
        "{\"userId\":55,\"roomName\":\"Math Room\"}",
        0,
        "join-request:102:REFUSED"
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));
    when(notificationService.sendMembershipRejectedSync(55, "Math Room")).thenReturn(false);

    worker.publishPendingEvents();

    verify(notificationService).sendMembershipRejectedSync(55, "Math Room");
    verify(outboxEventRepository).markFailed(eq(102L),
        contains("Delivery failed for event type"), eq(30));
    verify(outboxEventRepository, never()).markSent(anyLong());
  }

  @Test
  @DisplayName("Should mark event as failed for invalid payload")
  void invalidPayload_marksFailed() {
    OutboxEventRecord event = new OutboxEventRecord(
        103L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED,
        "{\"userId\":42}",
        0,
        "join-request:103:ACCEPTED"
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));

    worker.publishPendingEvents();

    verify(outboxEventRepository).markFailed(eq(103L),
        contains("Outbox payload must contain userId and roomName"), eq(30));
    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("Should mark event as failed for unsupported event type")
  void unsupportedEventType_marksFailed() {
    OutboxEventRecord event = new OutboxEventRecord(
        104L,
        "membership.unknown",
        "{\"userId\":7,\"roomName\":\"Robotics\"}",
        0,
        "join-request:104:UNKNOWN"
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));

    worker.publishPendingEvents();

    verify(outboxEventRepository).markFailed(eq(104L),
        contains("Unsupported outbox event type"), eq(30));
    verify(notificationService, never()).sendMembershipAcceptedSync(anyInt(), anyString());
    verify(notificationService, never()).sendMembershipRejectedSync(anyInt(), anyString());
  }
}

