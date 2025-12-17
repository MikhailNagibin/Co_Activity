package com.coactivity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private final JavaMailSender mailSender;
  private final String fromEmail;

  public MailService(JavaMailSender mailSender,
      @Value("${spring.mail.username}") String fromEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
  }

  /**
   * Sends a simple text email.
   */
  public void sendSimpleMessage(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }
}
