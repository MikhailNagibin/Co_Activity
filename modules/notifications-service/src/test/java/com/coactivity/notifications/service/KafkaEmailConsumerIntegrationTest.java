package com.coactivity.notifications.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.coactivity.notifications.dto.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
    "notifications.kafka.topic=notifications.email.v1",
    "notifications.kafka.group-id=notifications-service-test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "logging.level.org.apache.kafka=WARN",
    "logging.level.kafka=WARN",
    "logging.level.org.apache.zookeeper=WARN",
    "logging.level.state.change.logger=WARN"
})
@EmbeddedKafka(
    partitions = 1,
    topics = {"notifications.email.v1"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
@DisplayName("Kafka Email Consumer Integration Tests")
class KafkaEmailConsumerIntegrationTest {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private EmailService emailService;

  @Test
  @DisplayName("Should consume Kafka email command and call EmailService")
  void shouldConsumeAndDispatchEmail() throws Exception {
    SendEmailRequest request = new SendEmailRequest(
        "student@example.com",
        "Welcome",
        "Your request was accepted"
    );

    String payload = objectMapper.writeValueAsString(request);

    kafkaTemplate.send("notifications.email.v1", request.to(), payload)
        .get(5, TimeUnit.SECONDS);

    ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
    verify(emailService, timeout(5000).times(1)).sendEmail(captor.capture());

    SendEmailRequest captured = captor.getValue();
    assertEquals(request.to(), captured.to());
    assertEquals(request.subject(), captured.subject());
    assertEquals(request.body(), captured.body());
  }
}
