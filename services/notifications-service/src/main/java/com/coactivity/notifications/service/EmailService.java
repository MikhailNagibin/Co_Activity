package com.coactivity.notifications.service;

import com.coactivity.notifications.dto.SendEmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final String fromEmail;

  public EmailService(JavaMailSender mailSender,
      @Value("${spring.mail.username:}") String fromEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
  }

  public void sendEmail(SendEmailRequest request) {
    SimpleMailMessage message = new SimpleMailMessage();
    if (fromEmail != null && !fromEmail.isBlank()) {
      message.setFrom(fromEmail);
    }
    message.setTo(request.to());
    message.setSubject(request.subject());
    message.setText(request.body());
    mailSender.send(message);
  }
}
