package com.coactivity.notifications.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryMetrics {

  private final Counter deliveredCounter;
  private final Counter failedCounter;
  private final Counter invalidPayloadCounter;
  private final MeterRegistry meterRegistry;

  public NotificationDeliveryMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.deliveredCounter = Counter.builder("coactivity.notifications.email.commands")
        .description("Email command outcomes handled by notifications-service")
        .tag("result", "delivered")
        .register(meterRegistry);
    this.failedCounter = Counter.builder("coactivity.notifications.email.commands")
        .description("Email command outcomes handled by notifications-service")
        .tag("result", "failed")
        .register(meterRegistry);
    this.invalidPayloadCounter = Counter.builder("coactivity.notifications.email.commands")
        .description("Email command outcomes handled by notifications-service")
        .tag("result", "invalid_payload")
        .register(meterRegistry);
  }

  public void recordDelivered() {
    deliveredCounter.increment();
  }

  public void recordFailed() {
    failedCounter.increment();
  }

  public void recordInvalidPayload() {
    invalidPayloadCounter.increment();
  }

  public void recordDltPublish(String dltTopic, Exception exception) {
    Counter.builder("coactivity.notifications.email.dlt.published")
        .description("Email commands published to the dead-letter topic")
        .tag("dlt_topic", dltTopic)
        .tag("exception", exception.getClass().getSimpleName())
        .register(meterRegistry)
        .increment();
  }
}
