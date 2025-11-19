package com.coactivity.service;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest { // ← Must be public

  @Mock
  private MailService mailService;

  @InjectMocks
  private NotificationService notificationService;

  @Test // ← Must have @Test annotation
  public void testSendMembershipAccepted() { // ← Must be public
    // Test implementation
    notificationService.sendMembershipAccepted("test@test.com", "Test Room");
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void sendRealTestEmail() {
    // Replace with your actual email address
    String yourEmail = "bumaginnicita@yandex.ru";

    // Test one notification type
    notificationService.sendMembershipAccepted(yourEmail, "CoActivity Test Room");

    System.out.println("📧 Test email sent to: " + yourEmail);
    System.out.println("✅ Check your inbox (and spam folder)!");
  }
}