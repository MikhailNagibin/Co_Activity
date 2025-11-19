package com.coactivity.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private final JavaMailSender mailSender;

  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Sends a simple text email.
   */
  public void sendSimpleMessage(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("bumagin.nicita@yandex.ru");
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }
}
