package com.coactivity.service;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for NotificationService. Uses mocks to test business logic in isolation.
 * <p>
 * For real email sending tests, see MailServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

  @Mock
  private MailService mailService;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void testSendMembershipAccepted() {
    // Given
    String email = "test@test.com";
    String roomName = "Test Room";

    // When
    notificationService.sendMembershipAccepted(email, roomName);

    // Then
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void testSendMembershipRejected() {
    // Given
    String email = "test@test.com";
    String roomName = "Test Room";

    // When
    notificationService.sendMembershipRejected(email, roomName);

    // Then
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void testSendActivityClosed() {
    // Given
    String email = "test@test.com";
    String roomName = "Test Room";

    // When
    notificationService.sendActivityClosed(email, roomName);

    // Then
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void testSendNewJoinRequest() {
    // Given
    String adminEmail = "admin@test.com";
    String roomName = "Test Room";
    String requesterUsername = "testuser";

    // When
    notificationService.sendNewJoinRequest(adminEmail, roomName, requesterUsername);

    // Then
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void testSendLoginVerificationCode() {
    // Given
    String email = "test@test.com";
    String code = "ABC123";

    // When
    notificationService.sendLoginVerificationCode(email, code);

    // Then
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }
}
