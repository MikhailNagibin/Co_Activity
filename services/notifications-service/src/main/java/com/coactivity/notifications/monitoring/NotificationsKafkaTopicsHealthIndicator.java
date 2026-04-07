package com.coactivity.notifications.monitoring;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("notificationsKafkaTopics")
public class NotificationsKafkaTopicsHealthIndicator implements HealthIndicator {

  private static final String DEFAULT_DLT_SUFFIX = ".dlt";

  private final String notificationsTopic;
  private final String dltTopic;
  private final Duration timeout;
  private final Supplier<Admin> adminSupplier;

  @Autowired
  public NotificationsKafkaTopicsHealthIndicator(
      @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
      @Value("${notifications.kafka.topic:notifications.email.v1}") String notificationsTopic,
      @Value("${notifications.kafka.dlt-topic:}") String configuredDltTopic,
      @Value("${notifications.health.kafka-topics-timeout-ms:5000}") long timeoutMs) {
    this(
        notificationsTopic,
        resolveDltTopic(notificationsTopic, configuredDltTopic),
        Duration.ofMillis(Math.max(timeoutMs, 1000L)),
        () -> AdminClient.create(Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))
    );
  }

  NotificationsKafkaTopicsHealthIndicator(
      String notificationsTopic,
      String dltTopic,
      Duration timeout,
      Supplier<Admin> adminSupplier) {
    this.notificationsTopic = notificationsTopic;
    this.dltTopic = dltTopic;
    this.timeout = timeout;
    this.adminSupplier = adminSupplier;
  }

  @Override
  public Health health() {
    try (Admin admin = adminSupplier.get()) {
      Map<String, TopicDescription> topicDescriptions = admin.describeTopics(
          List.of(notificationsTopic, dltTopic)
      ).allTopicNames().get(timeout.toMillis(), TimeUnit.MILLISECONDS);

      TopicDescription notificationsDescription = topicDescriptions.get(notificationsTopic);
      TopicDescription dltDescription = topicDescriptions.get(dltTopic);

      return Health.up()
          .withDetail("notificationsTopic", notificationsTopic)
          .withDetail("notificationsTopicPartitions", notificationsDescription.partitions().size())
          .withDetail("dltTopic", dltTopic)
          .withDetail("dltTopicPartitions", dltDescription.partitions().size())
          .build();
    } catch (Exception e) {
      return Health.down(e)
          .withDetail("notificationsTopic", notificationsTopic)
          .withDetail("dltTopic", dltTopic)
          .build();
    }
  }

  private static String resolveDltTopic(String notificationsTopic, String configuredDltTopic) {
    if (configuredDltTopic != null && !configuredDltTopic.isBlank()) {
      return configuredDltTopic;
    }
    return notificationsTopic + DEFAULT_DLT_SUFFIX;
  }
}
