package com.coactivity.notifications.controller;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.coactivity.notifications.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

  private final EmailService emailService;

  public NotificationsController(EmailService emailService) {
    this.emailService = emailService;
  }

  @PostMapping("/email")
  public ResponseEntity<Void> sendEmail(@Valid @RequestBody SendEmailRequest request) {
    emailService.sendEmail(request);
    return ResponseEntity.noContent().build();
  }
}
