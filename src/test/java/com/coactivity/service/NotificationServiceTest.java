package com.coactivity.service;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

  @Mock
  private MailService mailService;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  public void testSendMembershipAccepted() {
    notificationService.sendMembershipAccepted("test@test.com", "Test Room");
    verify(mailService).sendSimpleMessage(anyString(), anyString(), anyString());
  }

  @Test
  void sendRealTestEmail() {
    String actualEmail = "bumaginnicita@yandex.ru";

    notificationService.sendMembershipAccepted(actualEmail, "CoActivity Test Room");

    System.out.println("📧 Test email sent to: " + actualEmail);
    System.out.println("✅ Check your inbox (and spam folder)!");
  }
}
