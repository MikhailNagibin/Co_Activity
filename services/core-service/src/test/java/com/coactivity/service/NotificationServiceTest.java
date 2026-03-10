package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("NotificationService Tests")
class NotificationServiceTest {

  private static final String TOPIC = "notifications.email.v1";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UserRepositoryImpl userRepository;
  private KafkaTemplate<String, String> kafkaTemplate;
  private NotificationService notificationService;

  private User testUser;
  private Integer testUserId;
  private String testUserEmail;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepositoryImpl.class);
    kafkaTemplate = Mockito.mock(KafkaTemplate.class);

    notificationService = new NotificationService(userRepository);
    notificationService.setKafkaTemplate(kafkaTemplate);
    notificationService.setNotificationsKafkaTopic(TOPIC);

    CompletableFuture<SendResult<String, String>> sendResult =
        CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(sendResult);

    testUserId = 1;
    testUserEmail = "test@example.com";
    testUser = new User(
        testUserId,
        testUserEmail,
        "testuser",
        "hashedPassword",
        Instant.now(),
        "Test Country",
        "Test City",
        "Test description",
        1,
        List.of(),
        List.of());
  }

  @Test
  @DisplayName("Publishes Kafka email command when membership accepted notifications are enabled")
  void sendMembershipAcceptedSync_publishesKafkaCommand() throws Exception {
    testUser.setNotifications(List.of(Notification.MEMBERSHIP_ACCEPTED));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    boolean delivered = notificationService.sendMembershipAcceptedSync(testUserId, "Chess Club");

    assertTrue(delivered);

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq(testUserEmail), payloadCaptor.capture());

    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertTrue(payload.get("subject").asText().contains("Chess Club"));
    assertTrue(payload.get("body").asText().contains("accepted"));
    assertTrue(payload.get("to").asText().equals(testUserEmail));
  }

  @Test
  @DisplayName("Skips Kafka publish when user disabled this notification type")
  void sendMembershipAcceptedSync_skipsWhenPreferenceDisabled() {
    testUser.setNotifications(List.of(Notification.ACTIVITY_CLOSED));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    boolean delivered = notificationService.sendMembershipAcceptedSync(testUserId, "Chess Club");

    assertTrue(delivered);
    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Always publishes Kafka email command for login verification")
  void sendLoginVerificationCode_publishesKafkaCommand() throws Exception {
    notificationService.sendLoginVerificationCode(testUserEmail, "123456");

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq(testUserEmail), payloadCaptor.capture());

    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertTrue(payload.get("subject").asText().contains("verification code"));
    assertTrue(payload.get("body").asText().contains("123456"));
  }

  @Test
  @DisplayName("Returns false when Kafka template is missing")
  void sendMembershipAcceptedSync_returnsFalseWhenKafkaTemplateMissing() {
    NotificationService notificationServiceWithoutKafka = new NotificationService(userRepository);
    notificationServiceWithoutKafka.setNotificationsKafkaTopic(TOPIC);

    testUser.setNotifications(List.of(Notification.MEMBERSHIP_ACCEPTED));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    boolean delivered =
        notificationServiceWithoutKafka.sendMembershipAcceptedSync(testUserId, "Chess Club");

    assertFalse(delivered);
  }
}
