package com.coactivity.notifications.config;

import com.coactivity.notifications.service.InvalidEmailCommandException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

  private static final String DEFAULT_NOTIFICATIONS_TOPIC = "notifications.email.v1";
  private static final String DEFAULT_DLT_SUFFIX = ".dlt";
  private static final long DEFAULT_RETRY_INTERVAL_MS = 1000L;
  private static final long DEFAULT_RETRY_ATTEMPTS = 2L;

  @Bean
  public DeadLetterPublishingRecoverer kafkaDeadLetterPublishingRecoverer(
      KafkaOperations<Object, Object> kafkaOperations,
      @Value("${notifications.kafka.topic:" + DEFAULT_NOTIFICATIONS_TOPIC + "}")
      String notificationsTopic,
      @Value("${notifications.kafka.dlt-topic:}") String dltTopic) {
    String resolvedDltTopic = resolveDltTopic(notificationsTopic, dltTopic);
    return new DeadLetterPublishingRecoverer(kafkaOperations,
        (record, exception) -> new TopicPartition(resolvedDltTopic, -1));
  }

  @Bean
  public NewTopic notificationsDeadLetterTopic(
      @Value("${notifications.kafka.topic:" + DEFAULT_NOTIFICATIONS_TOPIC + "}")
      String notificationsTopic,
      @Value("${notifications.kafka.dlt-topic:}") String dltTopic) {
    return TopicBuilder.name(resolveDltTopic(notificationsTopic, dltTopic))
        .partitions(1)
        .replicas(1)
        .build();
  }

  @Bean
  public CommonErrorHandler kafkaCommonErrorHandler(
      DeadLetterPublishingRecoverer kafkaDeadLetterPublishingRecoverer) {
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(
        kafkaDeadLetterPublishingRecoverer,
        new FixedBackOff(DEFAULT_RETRY_INTERVAL_MS, DEFAULT_RETRY_ATTEMPTS));
    errorHandler.addNotRetryableExceptions(InvalidEmailCommandException.class);
    return errorHandler;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> consumerFactory,
      CommonErrorHandler kafkaCommonErrorHandler) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, consumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    factory.setCommonErrorHandler(kafkaCommonErrorHandler);
    return factory;
  }

  private String resolveDltTopic(String notificationsTopic, String configuredDltTopic) {
    if (configuredDltTopic != null && !configuredDltTopic.isBlank()) {
      return configuredDltTopic;
    }
    return notificationsTopic + DEFAULT_DLT_SUFFIX;
  }
}
