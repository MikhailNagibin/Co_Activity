package com.coactivity.notifications.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@DisplayName("Notifications Kafka Topics Health Indicator Tests")
class NotificationsKafkaTopicsHealthIndicatorTest {

  @Test
  @DisplayName("Should report up when notifications and DLT topics exist")
  void shouldReportUpWhenTopicsExist() {
    Admin admin = Mockito.mock(Admin.class);
    DescribeTopicsResult describeTopicsResult = Mockito.mock(DescribeTopicsResult.class);
    Mockito.when(admin.describeTopics(List.of("notifications.email.v1", "notifications.email.v1.dlt")))
        .thenReturn(describeTopicsResult);
    Mockito.when(describeTopicsResult.allTopicNames()).thenReturn(
        completedKafkaFuture(Map.of(
            "notifications.email.v1", topicDescription("notifications.email.v1"),
            "notifications.email.v1.dlt", topicDescription("notifications.email.v1.dlt")
        ))
    );

    NotificationsKafkaTopicsHealthIndicator indicator =
        new NotificationsKafkaTopicsHealthIndicator(
            "notifications.email.v1",
            "notifications.email.v1.dlt",
            Duration.ofSeconds(5),
            () -> admin
        );

    Health health = indicator.health();

    assertEquals(Status.UP, health.getStatus());
    assertEquals("notifications.email.v1.dlt", health.getDetails().get("dltTopic"));
  }

  @Test
  @DisplayName("Should report down when topic lookup fails")
  void shouldReportDownWhenTopicLookupFails() {
    NotificationsKafkaTopicsHealthIndicator indicator =
        new NotificationsKafkaTopicsHealthIndicator(
            "notifications.email.v1",
            "notifications.email.v1.dlt",
            Duration.ofSeconds(5),
            () -> {
              throw new IllegalStateException("broker unavailable");
            }
        );

    Health health = indicator.health();

    assertEquals(Status.DOWN, health.getStatus());
    assertTrue(health.getDetails().containsKey("notificationsTopic"));
    assertFalse(health.getDetails().isEmpty());
  }

  private static TopicDescription topicDescription(String topicName) {
    return new TopicDescription(
        topicName,
        false,
        List.of(new TopicPartitionInfo(0, new Node(1, "broker", 9092), List.of(), List.of()))
    );
  }

  private static <T> KafkaFuture<T> completedKafkaFuture(T value) {
    CompletableFuture<T> future = CompletableFuture.completedFuture(value);
    return KafkaFuture.completedFuture(future.join());
  }
}
