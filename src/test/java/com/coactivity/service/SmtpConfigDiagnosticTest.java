package com.coactivity.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

/**
 * Диагностический тест для проверки конфигурации SMTP.
 * Запустите этот тест чтобы увидеть текущие настройки подключения.
 */
@SpringBootTest
@ActiveProfiles("test")
public class SmtpConfigDiagnosticTest {

  @Autowired
  private JavaMailSender mailSender;

  @Test
  @Disabled("Раскомментируйте чтобы увидеть конфигурацию SMTP")
  void printSmtpConfiguration() {
    System.out.println("========================================");
    System.out.println("🔍 SMTP CONFIGURATION DIAGNOSTIC");
    System.out.println("========================================");

    if (mailSender instanceof JavaMailSenderImpl impl) {
      System.out.println("Host: " + impl.getHost());
      System.out.println("Port: " + impl.getPort());
      System.out.println("Username: " + impl.getUsername());
      System.out.println("Password: " + (impl.getPassword() != null ? "***" + impl.getPassword().substring(Math.max(0, impl.getPassword().length() - 4)) : "NOT SET"));
      System.out.println("Protocol: " + impl.getProtocol());
      System.out.println("Default Encoding: " + impl.getDefaultEncoding());
      System.out.println("\nJavaMailProperties:");
      impl.getJavaMailProperties().forEach((key, value) ->
        System.out.println("  " + key + " = " + value)
      );
    }

    System.out.println("========================================");
    System.out.println("✅ Проверьте что:");
    System.out.println("1. Host = smtp.yandex.com");
    System.out.println("2. Port = 587");
    System.out.println("3. Username = bumagin.nicita@yandex.ru");
    System.out.println("4. Password заканчивается на последние 4 символа app-пароля");
    System.out.println("5. mail.smtp.starttls.enable = true");
    System.out.println("========================================");
  }
}

