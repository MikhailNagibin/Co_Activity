package com.coactivity.qa;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
class QaServiceApplicationTests {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
      .withDatabaseName("qa_service_test_db")
      .withUsername("postgres")
      .withPassword("postgres")
      .withInitScript("sql/test_init_full.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("security.jwt.secret-base64",
        () -> "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
    registry.add("security.jwt.issuer", () -> "coactivity-core");
    registry.add("security.jwt.audience", () -> "coactivity-api");
  }

  @Test
  void contextLoads() {
  }
}
