package com.coactivity;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
public class CoActivityApplication {

  public static void main(String[] args) {
    // Load .env file if it exists and set as system properties
    // Spring Boot will read these via ${VARIABLE_NAME} syntax in application.properties
    Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();
    
    // Set system properties from .env file so Spring Boot can access them
    dotenv.entries().forEach(entry -> {
      System.setProperty(entry.getKey(), entry.getValue());
    });
    
    SpringApplication.run(CoActivityApplication.class, args);
  }
}
