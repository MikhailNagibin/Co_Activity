package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
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
    boolean delivered = notificationService.sendLoginVerificationCode(testUserEmail, "123456");

    assertTrue(delivered);

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq(testUserEmail), payloadCaptor.capture());

    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertTrue(payload.get("subject").asText().contains("verification code"));
    assertTrue(payload.get("body").asText().contains("123456"));
  }

  @Test
  @DisplayName("Returns false for login verification when Kafka template is missing")
  void sendLoginVerificationCode_returnsFalseWhenKafkaTemplateMissing() {
    NotificationService notificationServiceWithoutKafka = new NotificationService(userRepository);
    notificationServiceWithoutKafka.setNotificationsKafkaTopic(TOPIC);

    boolean delivered = notificationServiceWithoutKafka.sendLoginVerificationCode(testUserEmail,
        "123456");

    assertFalse(delivered);
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

  @Test
  @DisplayName("Logs warn without success message when membership rejected publish fails")
  void sendMembershipRejectedSync_logsWarnWithoutSuccessWhenKafkaPublishFails() {
    NotificationService notificationServiceWithoutKafka = new NotificationService(userRepository);
    notificationServiceWithoutKafka.setNotificationsKafkaTopic(TOPIC);

    testUser.setNotifications(List.of(Notification.MEMBERSHIP_REJECTED));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    ListAppender<ILoggingEvent> logAppender = attachLogAppender();
    try {
      boolean delivered =
          notificationServiceWithoutKafka.sendMembershipRejectedSync(testUserId, "Chess Club");

      assertFalse(delivered);
      assertTrue(containsLog(logAppender, "Failed to publish membership rejected notification"));
      assertFalse(containsLog(logAppender, "Membership rejected notification sent"));
    } finally {
      detachLogAppender(logAppender);
    }
  }

  @Test
  @DisplayName("Logs warn without success message when activity closed publish fails")
  void sendActivityClosed_logsWarnWithoutSuccessWhenKafkaPublishFails() {
    NotificationService notificationServiceWithoutKafka = new NotificationService(userRepository);
    notificationServiceWithoutKafka.setNotificationsKafkaTopic(TOPIC);

    testUser.setNotifications(List.of(Notification.ACTIVITY_CLOSED));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    ListAppender<ILoggingEvent> logAppender = attachLogAppender();
    try {
      notificationServiceWithoutKafka.sendActivityClosed(testUserId, "Chess Club");

      assertTrue(containsLog(logAppender, "Failed to publish activity closed notification"));
      assertFalse(containsLog(logAppender, "Activity closed notification sent"));
    } finally {
      detachLogAppender(logAppender);
    }
  }

  @Test
  @DisplayName("Logs warn without success message when new join request publish fails")
  void sendNewJoinRequest_logsWarnWithoutSuccessWhenKafkaPublishFails() {
    NotificationService notificationServiceWithoutKafka = new NotificationService(userRepository);
    notificationServiceWithoutKafka.setNotificationsKafkaTopic(TOPIC);

    testUser.setNotifications(List.of(Notification.NEW_JOIN_REQUEST));
    when(userRepository.getUserById(testUserId)).thenReturn(testUser);

    ListAppender<ILoggingEvent> logAppender = attachLogAppender();
    try {
      notificationServiceWithoutKafka.sendNewJoinRequest(testUserId, "Chess Club", "alice");

      assertTrue(containsLog(logAppender, "Failed to publish new join request notification"));
      assertFalse(containsLog(logAppender, "New join request notification sent"));
    } finally {
      detachLogAppender(logAppender);
    }
  }

  private ListAppender<ILoggingEvent> attachLogAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
    return logAppender;
  }

  private void detachLogAppender(ListAppender<ILoggingEvent> logAppender) {
    Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
    logger.detachAppender(logAppender);
    logAppender.stop();
  }

  private boolean containsLog(ListAppender<ILoggingEvent> logAppender, String fragment) {
    return logAppender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .anyMatch(message -> message.contains(fragment));
  }
}
