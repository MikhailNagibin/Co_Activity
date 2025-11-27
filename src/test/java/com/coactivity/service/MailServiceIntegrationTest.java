package com.coactivity.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for email sending functionality. Uses real Spring context and SMTP
 * configuration.
 * <p>
 * NOTE: This test is disabled by default to prevent accidental email sending during CI/CD. Enable
 * it manually when you want to test real email delivery.
 * <p>
 * This test uses the 'test' profile which disables database auto-configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
public class MailServiceIntegrationTest {

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private MailService mailService;

  /**
   * Sends a real test email to verify SMTP configuration.
   * <p>
   * To run this test: 1. Remove the @Disabled annotation 2. Run the test 3. Check your email inbox
   * at bumaginnicita@yandex.ru 4. Don't forget to check the spam folder if the email doesn't appear
   * in inbox
   */
  @Test
  @Disabled("Enable manually to send real email - remove this annotation to run")
  void sendRealEmailTest() {
    String testEmail = "bumaginnicita@yandex.ru";
    String roomName = "CoActivity Test Room";

    System.out.println("========================================");
    System.out.println("📧 SENDING REAL TEST EMAIL");
    System.out.println("========================================");
    System.out.println("Recipient: " + testEmail);
    System.out.println("Type: Membership Accepted notification");
    System.out.println("========================================");

    // Send membership accepted notification
    notificationService.sendMembershipAccepted(testEmail, roomName);

    System.out.println("✅ Email sending completed!");
    System.out.println("========================================");
    System.out.println("NEXT STEPS:");
    System.out.println("1. Check your inbox at: " + testEmail);
    System.out.println("2. If not in inbox, check SPAM folder");
    System.out.println("3. Check application logs for detailed info");
    System.out.println("========================================");
  }

  /**
   * Tests all four notification types.
   */
  @Test
  @Disabled("Enable manually to send all notification types")
  void sendAllNotificationTypesTest() {
    String testEmail = "bumaginnicita@yandex.ru";

    System.out.println("========================================");
    System.out.println("📧 SENDING ALL NOTIFICATION TYPES");
    System.out.println("========================================");

    // Test 1: Membership Accepted
    System.out.println("Sending: Membership Accepted...");
    notificationService.sendMembershipAccepted(testEmail, "Test Room 1");

    // Test 2: Membership Rejected
    System.out.println("Sending: Membership Rejected...");
    notificationService.sendMembershipRejected(testEmail, "Test Room 2");

    // Test 3: Activity Closed
    System.out.println("Sending: Activity Closed...");
    notificationService.sendActivityClosed(testEmail, "Test Room 3");

    // Test 4: New Join Request
    System.out.println("Sending: New Join Request...");
    notificationService.sendNewJoinRequest(testEmail, "Test Room 4", "TestUser123");

    System.out.println("========================================");
    System.out.println("✅ All 4 notification types sent!");
    System.out.println("Check your inbox at: " + testEmail);
    System.out.println("========================================");
  }

  /**
   * Tests login verification code email.
   */
  @Test
  @Disabled("Enable manually to test verification code email")
  void sendVerificationCodeTest() {
    String testEmail = "bumaginnicita@yandex.ru";
    String testCode = "ABC123";

    System.out.println("========================================");
    System.out.println("🔐 SENDING VERIFICATION CODE EMAIL");
    System.out.println("========================================");
    System.out.println("Recipient: " + testEmail);
    System.out.println("Code: " + testCode);
    System.out.println("========================================");

    notificationService.sendLoginVerificationCode(testEmail, testCode);

    System.out.println("✅ Verification code email sent!");
    System.out.println("Check your inbox at: " + testEmail);
    System.out.println("========================================");
  }

  /**
   * Direct test of MailService for debugging SMTP issues.
   */
  @Test
  @Disabled("Enable manually for low-level SMTP debugging")
  void sendSimpleEmailDirectTest() {
    String testEmail = "bumaginnicita@yandex.ru";
    String subject = "Direct SMTP Test";
    String body = "This is a direct test of MailService.\n\nIf you receive this, SMTP configuration is working correctly!";

    System.out.println("========================================");
    System.out.println("🔧 DIRECT SMTP TEST");
    System.out.println("========================================");
    System.out.println("Recipient: " + testEmail);
    System.out.println("Subject: " + subject);
    System.out.println("========================================");

    mailService.sendSimpleMessage(testEmail, subject, body);

    System.out.println("✅ Direct email sent!");
    System.out.println("Check your inbox at: " + testEmail);
    System.out.println("========================================");
  }
}
