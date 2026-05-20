package com.coactivity.notifications.service;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.coactivity.notifications.monitoring.NotificationDeliveryMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
public class KafkaEmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaEmailConsumer.class);

  private final EmailService emailService;
  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final NotificationDeliveryMetrics metrics;

  public KafkaEmailConsumer(EmailService emailService, ObjectMapper objectMapper,
      Validator validator, NotificationDeliveryMetrics metrics) {
    this.emailService = emailService;
    this.objectMapper = objectMapper;
    this.validator = validator;
    this.metrics = metrics;
  }

  @KafkaListener(
      topics = "${notifications.kafka.topic:notifications.email.v1}",
      groupId = "${notifications.kafka.group-id:notifications-service-v1}")
  public void consumeEmailCommand(String payload) {
    SendEmailRequest request = deserializeAndValidate(payload);
    try {
      emailService.sendEmail(request);
      metrics.recordDelivered();
      log.info("Kafka email command delivered to {}", request.to());
    } catch (MailException e) {
      metrics.recordFailed();
      log.error("Failed to deliver Kafka email command: {}", payload, e);
      throw e;
    }
  }

  private SendEmailRequest deserializeAndValidate(String payload) {
    final SendEmailRequest request;
    try {
      request = objectMapper.readValue(payload, SendEmailRequest.class);
    } catch (Exception e) {
      metrics.recordInvalidPayload();
      log.error("Rejecting invalid Kafka email command payload: {}", payload, e);
      throw new InvalidEmailCommandException("Kafka email command payload is not valid JSON", e);
    }

    Set<ConstraintViolation<SendEmailRequest>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      String violationMessage = violations.stream()
          .map(ConstraintViolation::getMessage)
          .collect(Collectors.joining(", "));
      metrics.recordInvalidPayload();
      log.error("Rejecting invalid Kafka email command: {}", violationMessage);
      throw new InvalidEmailCommandException(
          "Kafka email command violates DTO constraints: " + violationMessage);
    }

    return request;
  }
}
