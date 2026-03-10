package com.coactivity.notifications.service;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaEmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaEmailConsumer.class);

  private final EmailService emailService;
  private final ObjectMapper objectMapper;

  public KafkaEmailConsumer(EmailService emailService, ObjectMapper objectMapper) {
    this.emailService = emailService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${notifications.kafka.topic:notifications.email.v1}",
      groupId = "${notifications.kafka.group-id:notifications-service-v1}")
  public void consumeEmailCommand(String payload) {
    try {
      SendEmailRequest request = objectMapper.readValue(payload, SendEmailRequest.class);
      emailService.sendEmail(request);
      log.info("Kafka email command delivered to {}", request.to());
    } catch (Exception e) {
      log.error("Failed to process Kafka email command: {}", payload, e);
      throw new IllegalStateException("Failed to process Kafka email command", e);
    }
  }
}
