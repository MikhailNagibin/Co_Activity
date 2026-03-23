package com.coactivity.notifications.service;

import com.coactivity.notifications.dto.SendEmailRequest;
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

  public KafkaEmailConsumer(EmailService emailService, ObjectMapper objectMapper,
      Validator validator) {
    this.emailService = emailService;
    this.objectMapper = objectMapper;
    this.validator = validator;
  }

  @KafkaListener(
      topics = "${notifications.kafka.topic:notifications.email.v1}",
      groupId = "${notifications.kafka.group-id:notifications-service-v1}")
  public void consumeEmailCommand(String payload) {
    try {
      SendEmailRequest request = objectMapper.readValue(payload, SendEmailRequest.class);
      Set<ConstraintViolation<SendEmailRequest>> violations = validator.validate(request);
      if (!violations.isEmpty()) {
        log.error("Rejecting invalid Kafka email command: {}",
            violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", ")));
        return;
      }
      emailService.sendEmail(request);
      log.info("Kafka email command delivered to {}", request.to());
    } catch (MailException e) {
      log.error("Failed to deliver Kafka email command: {}", payload, e);
      throw e;
    } catch (Exception e) {
      log.error("Rejecting invalid Kafka email command payload: {}", payload, e);
    }
  }
}
