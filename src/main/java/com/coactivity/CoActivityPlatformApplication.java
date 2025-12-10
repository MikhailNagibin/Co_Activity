package com.coactivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
public class CoActivityPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoActivityPlatformApplication.class, args);
  }

}
