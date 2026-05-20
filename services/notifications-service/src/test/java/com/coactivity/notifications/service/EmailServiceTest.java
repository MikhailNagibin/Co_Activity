package com.coactivity.notifications.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.coactivity.notifications.dto.SendEmailRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@DisplayName("Email Service Tests")
class EmailServiceTest {

  @Test
  @DisplayName("Should use explicit notifications mail from address")
  void shouldUseExplicitNotificationsMailFromAddress() {
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    EmailService emailService = new EmailService(mailSender, "noreply@coactivity.dev");
    SendEmailRequest request = new SendEmailRequest(
        "student@example.com",
        "Welcome",
        "Your request was accepted"
    );

    emailService.sendEmail(request);

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage message = captor.getValue();
    assertEquals("noreply@coactivity.dev", message.getFrom());
    assertEquals("student@example.com", message.getTo()[0]);
  }
}
