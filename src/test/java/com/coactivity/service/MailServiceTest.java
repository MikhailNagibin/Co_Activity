package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for MailService.
 */
@DisplayName("MailService Tests")
class MailServiceTest {

  private static final String FROM_EMAIL = "test-sender@example.com";
  private JavaMailSender javaMailSender;
  private MailService mailService;

  @BeforeEach
  void setUp() {
    javaMailSender = Mockito.mock(JavaMailSender.class);
    mailService = new MailService(javaMailSender, FROM_EMAIL);
  }

  @Nested
  @DisplayName("sendSimpleMessage")
  class SendSimpleMessageTests {

    @Test
    @DisplayName("Should send email with correct recipient")
    void sendsToCorrectRecipient() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Test Subject";
      String text = "Test message body";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      String[] recipients = sentMessage.getTo();
      assertNotNull(recipients);
      assertEquals(1, recipients.length);
      assertEquals(to, recipients[0]);
    }

    @Test
    @DisplayName("Should send email with correct subject")
    void sendsWithCorrectSubject() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Important Notification";
      String text = "Test message body";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      assertEquals(subject, sentMessage.getSubject());
    }

    @Test
    @DisplayName("Should send email with correct body text")
    void sendsWithCorrectBody() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Test Subject";
      String text = "This is the email body content.\nWith multiple lines.";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      assertEquals(text, sentMessage.getText());
    }

    @Test
    @DisplayName("Should use configured from email address")
    void usesConfiguredFromAddress() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Test Subject";
      String text = "Test message body";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      assertEquals(FROM_EMAIL, sentMessage.getFrom());
    }

    @Test
    @DisplayName("Should call JavaMailSender.send exactly once")
    void callsSendOnce() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Test Subject";
      String text = "Test message body";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should handle emoji in subject")
    void handlesEmojiInSubject() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "🎉 Welcome to the team!";
      String text = "Congratulations on joining!";

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      assertEquals(subject, sentMessage.getSubject());
    }

    @Test
    @DisplayName("Should handle long email body")
    void handlesLongBody() {
      // Arrange
      String to = "recipient@example.com";
      String subject = "Test Subject";
      String text = "A".repeat(10000); // 10KB of text

      // Act
      mailService.sendSimpleMessage(to, subject, text);

      // Assert
      ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(
          SimpleMailMessage.class);
      verify(javaMailSender).send(messageCaptor.capture());

      SimpleMailMessage sentMessage = messageCaptor.getValue();
      assertNotNull(sentMessage);
      String bodyText = sentMessage.getText();
      assertNotNull(bodyText);
      assertEquals(10000, bodyText.length());
    }
  }
}
