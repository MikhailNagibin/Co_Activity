package com.coactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);
  private final JavaMailSender mailSender;

  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Sends a simple text email.
   *
   * @param to      the recipient email address
   * @param subject the email subject
   * @param text    the email body text
   */
  public void sendSimpleMessage(String to, String subject, String text) {
    try {
      logger.info("Preparing to send email to: {}, subject: '{}'", to, subject);

      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom("bumagin.nicita@yandex.ru");
      message.setTo(to);
      message.setSubject(subject);
      message.setText(text);

      mailSender.send(message);

      logger.info("Email successfully sent to: {}", to);
    } catch (Exception e) {
      logger.error("Failed to send email to: {}, subject: '{}'. Error: {}", to, subject,
          e.getMessage(), e);
      throw new RuntimeException("Failed to send email to " + to, e);
    }
  }
}
