package com.coactivity.notifications.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs a clear error when SMTP is not configured — otherwise login verification emails fail
 * silently from the user's perspective (Kafka publish succeeds, mail never leaves the server).
 */
@Component
public class SmtpStartupWarning {

  private static final Logger log = LoggerFactory.getLogger(SmtpStartupWarning.class);

  @Value("${spring.mail.username:}")
  private String mailUsername;

  @Value("${spring.mail.password:}")
  private String mailPassword;

  @EventListener(ApplicationReadyEvent.class)
  public void warnIfSmtpNotConfigured() {
    if (mailUsername == null || mailUsername.isBlank()) {
      log.error(
          "SPRING_MAIL_USERNAME is empty — this service cannot send email. "
              + "Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD (e.g. Yandex app password) in .env "
              + "for notifications-service, then restart. Login verification codes will not arrive otherwise.");
      return;
    }
    if (mailPassword == null || mailPassword.isBlank()) {
      log.error(
          "SPRING_MAIL_PASSWORD is empty — SMTP authentication will fail. "
              + "Use a Yandex (or other) app password, not the main account password.");
    }
  }
}
