package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.MailSendException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Unit tests for NotificationService - runs synchronously without Spring context. This avoids async
 * complications and tests the business logic directly.
 */
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

  private MailService mailService;
  private UserRepositoryImpl userRepository;
  private NotificationService notificationService;

  private User testUser;
  private Integer testUserId;
  private String testUserEmail;
  private String testRoomName;

  @BeforeEach
  void setUp() {
    // Create mocks manually
    mailService = Mockito.mock(MailService.class);
    userRepository = Mockito.mock(UserRepositoryImpl.class);

    // Create real NotificationService with mocked dependencies
    // This runs synchronously (no @Async) since we're not using Spring context
    notificationService = new NotificationService(mailService, userRepository);

    // Initialize test data
    testUserId = 1;
    testUserEmail = "test@example.com";
    testRoomName = "Test Room";

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
        new ArrayList<>(),
        new ArrayList<>()
    );
  }

  // ==================== Membership Accepted Tests ====================

  @Nested
  @DisplayName("sendMembershipAccepted")
  class MembershipAcceptedTests {

    @Test
    @DisplayName("Should send notification when user has preference enabled")
    void withPreference_sendsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

      verify(mailService).sendSimpleMessage(
          emailCaptor.capture(),
          subjectCaptor.capture(),
          messageCaptor.capture()
      );

      assertEquals(testUserEmail, emailCaptor.getValue());
      assertTrue(subjectCaptor.getValue().contains("Welcome to"));
      assertTrue(subjectCaptor.getValue().contains(testRoomName));
      assertTrue(messageCaptor.getValue().contains("accepted"));
    }

    @Test
    @DisplayName("Should NOT send notification when user has preference disabled")
    void withoutPreference_skipsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.ACTIVITY_CLOSED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should NOT send notification when user has no preferences set")
    void noPreferences_skipsEmail() {
      // Arrange
      testUser.setNotifications(new ArrayList<>());
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should NOT send notification when user not found")
    void userNotFound_skipsEmail() {
      // Arrange
      when(userRepository.getUserById(testUserId)).thenReturn(null);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should include room name in email body")
    void includesRoomName() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
      verify(mailService).sendSimpleMessage(anyString(), anyString(), messageCaptor.capture());

      String message = messageCaptor.getValue();
      assertTrue(message.contains(testRoomName));
      assertTrue(message.contains("CoActivity Team"));
    }
  }

  // ==================== Membership Rejected Tests ====================

  @Nested
  @DisplayName("sendMembershipRejected")
  class MembershipRejectedTests {

    @Test
    @DisplayName("Should send notification when user has preference enabled")
    void withPreference_sendsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_REJECTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipRejected(testUserId, testRoomName);

      // Assert
      ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

      verify(mailService).sendSimpleMessage(
          eq(testUserEmail),
          subjectCaptor.capture(),
          messageCaptor.capture()
      );

      assertTrue(subjectCaptor.getValue().contains("Membership request update"));
      assertTrue(messageCaptor.getValue().contains("cannot accept your participation"));
    }

    @Test
    @DisplayName("Should NOT send notification when user has preference disabled")
    void withoutPreference_skipsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.ACTIVITY_CLOSED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipRejected(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== Activity Closed Tests ====================

  @Nested
  @DisplayName("sendActivityClosed")
  class ActivityClosedTests {

    @Test
    @DisplayName("Should send notification when user has preference enabled")
    void withPreference_sendsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.ACTIVITY_CLOSED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendActivityClosed(testUserId, testRoomName);

      // Assert
      ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

      verify(mailService).sendSimpleMessage(
          eq(testUserEmail),
          subjectCaptor.capture(),
          messageCaptor.capture()
      );

      assertTrue(subjectCaptor.getValue().contains("Activity closed"));
      assertTrue(messageCaptor.getValue().contains("has been closed"));
    }

    @Test
    @DisplayName("Should NOT send notification when user has preference disabled")
    void withoutPreference_skipsEmail() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendActivityClosed(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== New Join Request Tests ====================

  @Nested
  @DisplayName("sendNewJoinRequest")
  class NewJoinRequestTests {

    @Test
    @DisplayName("Should send notification when admin has preference enabled")
    void withPreference_sendsEmail() {
      // Arrange
      String requesterUsername = "newuser";
      List<Notification> notifications = List.of(Notification.NEW_JOIN_REQUEST);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendNewJoinRequest(testUserId, testRoomName, requesterUsername);

      // Assert
      ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

      verify(mailService).sendSimpleMessage(
          eq(testUserEmail),
          subjectCaptor.capture(),
          messageCaptor.capture()
      );

      assertTrue(subjectCaptor.getValue().contains("New join request"));
      assertTrue(messageCaptor.getValue().contains(requesterUsername));
      assertTrue(messageCaptor.getValue().contains("Room Administrator"));
    }

    @Test
    @DisplayName("Should NOT send notification when admin has preference disabled")
    void withoutPreference_skipsEmail() {
      // Arrange
      String requesterUsername = "newuser";
      List<Notification> notifications = List.of(Notification.ACTIVITY_CLOSED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendNewJoinRequest(testUserId, testRoomName, requesterUsername);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== Login Verification Tests ====================

  @Nested
  @DisplayName("sendLoginVerificationCode")
  class LoginVerificationTests {

    @Test
    @DisplayName("Should always send verification code (security requirement)")
    void alwaysSends() {
      // Arrange
      String verificationCode = "123456";

      // Act
      notificationService.sendLoginVerificationCode(testUserEmail, verificationCode);

      // Assert
      ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

      verify(mailService).sendSimpleMessage(
          eq(testUserEmail),
          subjectCaptor.capture(),
          messageCaptor.capture()
      );

      assertTrue(subjectCaptor.getValue().contains("verification code"));
      assertTrue(messageCaptor.getValue().contains(verificationCode));
      assertTrue(messageCaptor.getValue().contains("10 minutes"));
    }
  }

  // ==================== Error Handling Tests ====================

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle null user email gracefully")
    void nullUserEmail_skipsEmail() {
      // Arrange
      testUser = new User(
          testUserId,
          null, // null email (login)
          "testuser",
          "hashedPassword",
          Instant.now(),
          "Test Country",
          "Test City",
          "Test description",
          1,
          new ArrayList<>(),
          List.of(Notification.MEMBERSHIP_ACCEPTED)
      );
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not throw exception when mail service fails")
    void mailServiceException_doesNotThrow() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);
      doThrow(new MailSendException("SMTP connection failed"))
          .when(mailService).sendSimpleMessage(anyString(), anyString(), anyString());

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() ->
          notificationService.sendMembershipAccepted(testUserId, testRoomName)
      );
    }

    @Test
    @DisplayName("Should not throw exception when mail service fails for verification code")
    void mailServiceException_verificationCode_doesNotThrow() {
      // Arrange
      doThrow(new MailSendException("SMTP connection failed"))
          .when(mailService).sendSimpleMessage(anyString(), anyString(), anyString());

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() ->
          notificationService.sendLoginVerificationCode("test@example.com", "123456")
      );
    }

    @Test
    @DisplayName("Should handle null notifications list gracefully")
    void nullNotificationsList_skipsEmail() {
      // Arrange
      testUser.setNotifications(null);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== Multiple Preferences Tests ====================

  @Nested
  @DisplayName("Multiple Preferences")
  class MultiplePreferencesTests {

    @Test
    @DisplayName("Should send notification when user has multiple preferences including required")
    void multiplePreferences_includesRequired_sendsEmail() {
      // Arrange
      List<Notification> notifications = List.of(
          Notification.MEMBERSHIP_ACCEPTED,
          Notification.ACTIVITY_CLOSED,
          Notification.NEW_JOIN_REQUEST
      );
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, times(1)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should NOT send when multiple preferences exclude required type")
    void multiplePreferences_excludesRequired_skipsEmail() {
      // Arrange
      List<Notification> notifications = List.of(
          Notification.ACTIVITY_CLOSED,
          Notification.NEW_JOIN_REQUEST
      );
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      // Act
      notificationService.sendMembershipAccepted(testUserId, testRoomName);

      // Assert
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== Kafka Mode Tests ====================

  @Nested
  @DisplayName("Kafka Mode")
  class KafkaModeTests {

    @Test
    @DisplayName("Should publish email command to Kafka and not use local mail service")
    void kafkaMode_publishesToKafka() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);

      @SuppressWarnings("unchecked")
      KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
      when(kafkaTemplate.send(anyString(), anyString(), anyString()))
          .thenReturn(CompletableFuture.completedFuture(null));

      notificationService.setNotificationsMode("KAFKA");
      notificationService.setNotificationsKafkaTopic("notifications.email.v1");
      notificationService.setKafkaTemplate(kafkaTemplate);

      // Act
      boolean delivered = notificationService.sendMembershipAcceptedSync(testUserId, testRoomName);

      // Assert
      assertTrue(delivered);
      verify(kafkaTemplate, times(1))
          .send(eq("notifications.email.v1"), eq(testUserEmail), anyString());
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return false when Kafka mode is enabled but template is missing")
    void kafkaMode_withoutTemplate_returnsFalse() {
      // Arrange
      List<Notification> notifications = List.of(Notification.MEMBERSHIP_ACCEPTED);
      testUser.setNotifications(notifications);
      when(userRepository.getUserById(testUserId)).thenReturn(testUser);
      notificationService.setNotificationsMode("KAFKA");

      // Act
      boolean delivered = notificationService.sendMembershipAcceptedSync(testUserId, testRoomName);

      // Assert
      assertFalse(delivered);
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }
}
