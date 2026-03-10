package com.coactivity.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class NotificationsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(NotificationsServiceApplication.class, args);
  }
}
