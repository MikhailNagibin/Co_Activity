package com.coactivity.notifications.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.notifications.monitoring.NotificationDeliveryMetrics;
import com.coactivity.notifications.service.InvalidEmailCommandException;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.mail.MailSendException;

@DisplayName("Kafka Consumer Config Tests")
class KafkaConsumerConfigTest {

  private static final String TOPIC = "notifications.email.v1";

  private final KafkaConsumerConfig config = new KafkaConsumerConfig();

  @Test
  @DisplayName("Should publish failed record to default DLT topic")
  void shouldPublishFailedRecordToDefaultDltTopic() {
    KafkaOperations<Object, Object> kafkaOperations = mockKafkaOperations();
    NotificationDeliveryMetrics metrics = Mockito.mock(NotificationDeliveryMetrics.class);
    DeadLetterPublishingRecoverer recoverer =
        config.kafkaDeadLetterPublishingRecoverer(kafkaOperations, metrics, TOPIC, "");
    ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 1, 42L,
        "student@example.com", "{\"to\":\"student@example.com\"}");

    recoverer.accept(record, null, new MailSendException("smtp down"));

    ArgumentCaptor<ProducerRecord<Object, Object>> captor = producerRecordCaptor();
    verify(kafkaOperations).send(captor.capture());

    ProducerRecord<Object, Object> dltRecord = captor.getValue();
    assertEquals(TOPIC + ".dlt", dltRecord.topic());
    assertEquals("student@example.com", dltRecord.key());
    assertEquals("{\"to\":\"student@example.com\"}", dltRecord.value());
    verify(metrics).recordDltPublish(eq(TOPIC + ".dlt"), any(MailSendException.class));
  }

  @Test
  @DisplayName("Should use configured DLT topic override")
  void shouldUseConfiguredDltTopicOverride() {
    KafkaOperations<Object, Object> kafkaOperations = mockKafkaOperations();
    NotificationDeliveryMetrics metrics = Mockito.mock(NotificationDeliveryMetrics.class);
    DeadLetterPublishingRecoverer recoverer =
        config.kafkaDeadLetterPublishingRecoverer(kafkaOperations, metrics, TOPIC,
            "notifications.email.failures");
    ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 0, 11L,
        "student@example.com", "payload");

    recoverer.accept(record, null, new MailSendException("smtp down"));

    ArgumentCaptor<ProducerRecord<Object, Object>> captor = producerRecordCaptor();
    verify(kafkaOperations).send(captor.capture());
    assertEquals("notifications.email.failures", captor.getValue().topic());
    verify(metrics).recordDltPublish(eq("notifications.email.failures"), any(MailSendException.class));
  }

  @Test
  @DisplayName("Should send invalid payloads to recoverer without retry classification")
  void shouldSendInvalidPayloadsToRecovererWithoutRetryClassification() {
    DeadLetterPublishingRecoverer recoverer = Mockito.mock(DeadLetterPublishingRecoverer.class);
    CommonErrorHandler commonErrorHandler = config.kafkaCommonErrorHandler(recoverer);
    DefaultErrorHandler errorHandler = assertInstanceOf(DefaultErrorHandler.class,
        commonErrorHandler);
    ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 0, 0L, "key", "payload");
    @SuppressWarnings("unchecked")
    Consumer<String, String> consumer = Mockito.mock(Consumer.class);
    MessageListenerContainer container = Mockito.mock(MessageListenerContainer.class);

    errorHandler.handleOne(new InvalidEmailCommandException("invalid payload"), record, consumer,
        container);

    verify(recoverer).accept(eq(record), eq(consumer), any(InvalidEmailCommandException.class));
  }

  private KafkaOperations<Object, Object> mockKafkaOperations() {
    @SuppressWarnings("unchecked")
    KafkaOperations<Object, Object> kafkaOperations = Mockito.mock(KafkaOperations.class);
    CompletableFuture<SendResult<Object, Object>> sendResult =
        CompletableFuture.completedFuture(null);
    when(kafkaOperations.send(Mockito.<ProducerRecord<Object, Object>>any())).thenReturn(
        sendResult);
    return kafkaOperations;
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<ProducerRecord<Object, Object>> producerRecordCaptor() {
    return (ArgumentCaptor<ProducerRecord<Object, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(
        ProducerRecord.class);
  }
}
