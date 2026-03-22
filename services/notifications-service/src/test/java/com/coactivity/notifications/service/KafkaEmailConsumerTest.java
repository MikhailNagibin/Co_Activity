package com.coactivity.notifications.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Email Consumer Tests")
class KafkaEmailConsumerTest {

  @Mock
  private EmailService emailService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  @DisplayName("Should deserialize Kafka payload and call EmailService")
  void shouldConsumeAndDispatchEmail() throws Exception {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper, validator);
    SendEmailRequest request = new SendEmailRequest(
        "student@example.com",
        "Welcome",
        "Your request was accepted"
    );

    consumer.consumeEmailCommand(objectMapper.writeValueAsString(request));

    verify(emailService).sendEmail(request);
  }

  @Test
  @DisplayName("Should skip malformed Kafka payload without calling EmailService")
  void shouldSkipMalformedPayload() {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper, validator);

    consumer.consumeEmailCommand("not-json");
    verify(emailService, never()).sendEmail(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Should skip invalid Kafka payload that violates email DTO constraints")
  void shouldSkipInvalidDtoPayload() throws Exception {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper, validator);
    SendEmailRequest request = new SendEmailRequest(
        "not-an-email",
        "",
        "body"
    );

    consumer.consumeEmailCommand(objectMapper.writeValueAsString(request));

    verify(emailService, never()).sendEmail(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Should rethrow mail delivery errors so Kafka retries can happen")
  void shouldRethrowMailDeliveryError() throws Exception {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper, validator);
    SendEmailRequest request = new SendEmailRequest(
        "student@example.com",
        "Welcome",
        "Your request was accepted"
    );
    doThrow(new MailSendException("smtp down"))
        .when(emailService)
        .sendEmail(request);

    org.junit.jupiter.api.Assertions.assertThrows(MailSendException.class,
        () -> consumer.consumeEmailCommand(objectMapper.writeValueAsString(request)));
  }
}
