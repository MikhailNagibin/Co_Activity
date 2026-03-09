package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.OutboxEventRepositoryImpl;
import com.coactivity.repository.impl.OutboxEventRepositoryImpl.OutboxEventRecord;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox -> Kafka Flow Tests")
class NotificationOutboxKafkaFlowTest {

  @Mock
  private OutboxEventRepositoryImpl outboxEventRepository;

  @Mock
  private UserRepositoryImpl userRepository;

  @Mock
  private MailService mailService;

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  private NotificationOutboxWorker worker;

  @BeforeEach
  void setUp() {
    NotificationService notificationService = new NotificationService(mailService, userRepository);
    notificationService.setNotificationsMode("KAFKA");
    notificationService.setNotificationsKafkaTopic("notifications.email.v1");
    notificationService.setNotificationsKafkaSendTimeoutMs(5000L);
    notificationService.setKafkaTemplate(kafkaTemplate);

    worker = new NotificationOutboxWorker(outboxEventRepository, notificationService,
        new ObjectMapper());
    ReflectionTestUtils.setField(worker, "outboxEnabled", true);
    ReflectionTestUtils.setField(worker, "batchSize", 20);
    ReflectionTestUtils.setField(worker, "maxRetries", 5);
    ReflectionTestUtils.setField(worker, "retryDelaySeconds", 30);
  }

  @Test
  @DisplayName("Should publish Kafka command and mark outbox event as sent")
  void acceptedEvent_publishedToKafka_marksSent() throws Exception {
    OutboxEventRecord event = new OutboxEventRecord(
        301L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED,
        "{\"userId\":77,\"roomName\":\"Algorithms Club\"}",
        0,
        "join-request:301:ACCEPTED"
    );

    User user = new User(
        77,
        "student@example.com",
        "student",
        "hashed",
        Instant.now(),
        "Country",
        "City",
        "Desc",
        1,
        List.of(),
        List.of(Notification.MEMBERSHIP_ACCEPTED)
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));
    when(userRepository.getUserById(77)).thenReturn(user);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    worker.publishPendingEvents();

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate).send(eq("notifications.email.v1"), eq("student@example.com"),
        payloadCaptor.capture());
    verify(outboxEventRepository).markSent(301L);
    verify(outboxEventRepository, never()).markFailed(anyLong(), anyString(), anyInt());
    verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());

    JsonNode payload = new ObjectMapper().readTree(payloadCaptor.getValue());
    assertEquals("student@example.com", payload.get("to").asText());
    assertTrue(payload.get("subject").asText().contains("Algorithms Club"));
    assertTrue(payload.get("body").asText().contains("accepted"));
  }

  @Test
  @DisplayName("Should mark event as failed when Kafka publish fails")
  void kafkaPublishFails_marksFailed() {
    OutboxEventRecord event = new OutboxEventRecord(
        302L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED,
        "{\"userId\":88,\"roomName\":\"Math Club\"}",
        0,
        "join-request:302:ACCEPTED"
    );

    User user = new User(
        88,
        "math@example.com",
        "math",
        "hashed",
        Instant.now(),
        "Country",
        "City",
        "Desc",
        1,
        List.of(),
        List.of(Notification.MEMBERSHIP_ACCEPTED)
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));
    when(userRepository.getUserById(88)).thenReturn(user);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

    worker.publishPendingEvents();

    verify(outboxEventRepository).markFailed(eq(302L),
        contains("Delivery failed for event type"), eq(30));
    verify(outboxEventRepository, never()).markSent(anyLong());
  }

  @Test
  @DisplayName("Should mark as sent without Kafka publish when user opted out")
  void userOptedOut_marksSentWithoutPublish() {
    OutboxEventRecord event = new OutboxEventRecord(
        303L,
        JoinRequestOutboxTransactionService.EVENT_MEMBERSHIP_ACCEPTED,
        "{\"userId\":99,\"roomName\":\"Physics Club\"}",
        0,
        "join-request:303:ACCEPTED"
    );

    User user = new User(
        99,
        "physics@example.com",
        "physics",
        "hashed",
        Instant.now(),
        "Country",
        "City",
        "Desc",
        1,
        List.of(),
        List.of(Notification.ACTIVITY_CLOSED)
    );

    when(outboxEventRepository.claimNextBatch(20, 5)).thenReturn(List.of(event));
    when(userRepository.getUserById(99)).thenReturn(user);

    worker.publishPendingEvents();

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    verify(outboxEventRepository).markSent(303L);
    verify(outboxEventRepository, never()).markFailed(anyLong(), anyString(), anyInt());
  }
}

