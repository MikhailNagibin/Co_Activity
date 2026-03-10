package com.coactivity.notifications.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Email Consumer Tests")
class KafkaEmailConsumerTest {

  @Mock
  private EmailService emailService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should deserialize Kafka payload and call EmailService")
  void shouldConsumeAndDispatchEmail() throws Exception {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper);
    SendEmailRequest request = new SendEmailRequest(
        "student@example.com",
        "Welcome",
        "Your request was accepted"
    );

    consumer.consumeEmailCommand(objectMapper.writeValueAsString(request));

    verify(emailService).sendEmail(request);
  }

  @Test
  @DisplayName("Should throw when Kafka payload is invalid")
  void shouldFailOnInvalidPayload() {
    KafkaEmailConsumer consumer = new KafkaEmailConsumer(emailService, objectMapper);

    assertThrows(IllegalStateException.class, () -> consumer.consumeEmailCommand("not-json"));
    verify(emailService, never()).sendEmail(org.mockito.ArgumentMatchers.any());
  }
}
