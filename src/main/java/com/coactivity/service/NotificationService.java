package com.coactivity.service;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Handles notifications with user preference checking.
 *
 * <p>Mode switch:
 * <ul>
 *   <li>INPROC: send email directly using local MailService</li>
 *   <li>SERVICE: call notifications-service over HTTP</li>
 *   <li>KAFKA: publish email command to Kafka topic</li>
 * </ul>
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private static final String MODE_INPROC = "INPROC";
  private static final String MODE_SERVICE = "SERVICE";
  private static final String MODE_KAFKA = "KAFKA";
  private static final String DEFAULT_NOTIFICATIONS_BASE_URL = "http://localhost:8082";
  private static final String DEFAULT_NOTIFICATIONS_KAFKA_TOPIC = "notifications.email.v1";
  private static final long DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS = 5000L;

  private final MailService mailService;
  private final UserRepositoryImpl userRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private String notificationsMode = MODE_INPROC;
  private KafkaTemplate<String, String> kafkaTemplate;
  private String notificationsKafkaTopic = DEFAULT_NOTIFICATIONS_KAFKA_TOPIC;
  private long notificationsKafkaSendTimeoutMs = DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS;
  private RestClient notificationsRestClient =
      RestClient.builder().baseUrl(DEFAULT_NOTIFICATIONS_BASE_URL).build();

  public NotificationService(MailService mailService, UserRepositoryImpl userRepository) {
    this.mailService = mailService;
    this.userRepository = userRepository;
  }

  @Value("${notifications.mode:" + MODE_INPROC + "}")
  public void setNotificationsMode(String notificationsMode) {
    this.notificationsMode = notificationsMode != null ? notificationsMode : MODE_INPROC;
  }

  @Value("${notifications.service.base-url:" + DEFAULT_NOTIFICATIONS_BASE_URL + "}")
  public void setNotificationsServiceBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      this.notificationsRestClient =
          RestClient.builder().baseUrl(DEFAULT_NOTIFICATIONS_BASE_URL).build();
      return;
    }
    this.notificationsRestClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Autowired(required = false)
  public void setKafkaTemplate(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Value("${notifications.kafka.topic:" + DEFAULT_NOTIFICATIONS_KAFKA_TOPIC + "}")
  public void setNotificationsKafkaTopic(String notificationsKafkaTopic) {
    if (notificationsKafkaTopic == null || notificationsKafkaTopic.isBlank()) {
      this.notificationsKafkaTopic = DEFAULT_NOTIFICATIONS_KAFKA_TOPIC;
      return;
    }
    this.notificationsKafkaTopic = notificationsKafkaTopic;
  }

  @Value("${notifications.kafka.send-timeout-ms:" + DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS
      + "}")
  public void setNotificationsKafkaSendTimeoutMs(long notificationsKafkaSendTimeoutMs) {
    this.notificationsKafkaSendTimeoutMs =
        Math.max(notificationsKafkaSendTimeoutMs, 1000L);
  }

  @Async("taskExecutor")
  public void sendMembershipAccepted(Integer userId, String roomName) {
    sendMembershipAcceptedSync(userId, roomName);
  }

  @Async("taskExecutor")
  public void sendMembershipRejected(Integer userId, String roomName) {
    sendMembershipRejectedSync(userId, roomName);
  }

  /**
   * Synchronous dispatch used by outbox worker to know whether delivery succeeded.
   */
  public boolean sendMembershipAcceptedSync(Integer userId, String roomName) {
    log.debug("Attempting to send membership accepted notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || user.getLogin() == null) {
      log.warn("Cannot send notification: user {} not found or has no email", userId);
      return true;
    }

    if (!shouldNotifyUser(user, Notification.MEMBERSHIP_ACCEPTED)) {
      log.debug("User {} has disabled MEMBERSHIP_ACCEPTED notifications", userId);
      return true;
    }

    String subject = "🎉 Welcome to " + roomName + "!";
    String message = String.format("""
         Hello!

         Your request to join "%s" has been accepted.
         You can now participate in all room activities and discussions.

         Happy collaborating!
         The CoActivity Team

""", roomName);

    try {
      dispatchEmail(user.getLogin(), subject, message);
      log.info("✅ Membership accepted notification sent to userId={}, email={}", userId,
          user.getLogin());
      return true;
    } catch (Exception e) {
      log.error("❌ Failed to send membership accepted notification to userId={}, email={}", userId,
          user.getLogin(), e);
      return false;
    }
  }

  /**
   * Synchronous dispatch used by outbox worker to know whether delivery succeeded.
   */
  public boolean sendMembershipRejectedSync(Integer userId, String roomName) {
    log.debug("Attempting to send membership rejected notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || user.getLogin() == null) {
      log.warn("Cannot send notification: user {} not found or has no email", userId);
      return true;
    }

    if (!shouldNotifyUser(user, Notification.MEMBERSHIP_REJECTED)) {
      log.debug("User {} has disabled MEMBERSHIP_REJECTED notifications", userId);
      return true;
    }

    String subject = "❌ Membership request update for " + roomName;
    String message = String.format("""
         Hello!

         Your request to join "%s" has been reviewed but unfortunately
         we cannot accept your participation at this time.

         You can explore other rooms that might be a better fit!

         The CoActivity Team

""", roomName);

    try {
      dispatchEmail(user.getLogin(), subject, message);
      log.info("✅ Membership rejected notification sent to userId={}, email={}", userId,
          user.getLogin());
      return true;
    } catch (Exception e) {
      log.error("❌ Failed to send membership rejected notification to userId={}, email={}", userId,
          user.getLogin(), e);
      return false;
    }
  }

  @Async("taskExecutor")
  public void sendActivityClosed(Integer userId, String roomName) {
    log.debug("Attempting to send activity closed notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || user.getLogin() == null) {
      log.warn("Cannot send notification: user {} not found or has no email", userId);
      return;
    }

    if (!shouldNotifyUser(user, Notification.ACTIVITY_CLOSED)) {
      log.debug("User {} has disabled ACTIVITY_CLOSED notifications", userId);
      return;
    }

    String subject = "🔒 Activity closed: " + roomName;
    String message = String.format("""
        Hello!

        The activity "%s" has been closed.
        All participants have been removed and the room is no longer active.

        Thank you for your participation!

        The CoActivity Team
        """, roomName);

    try {
      dispatchEmail(user.getLogin(), subject, message);
      log.info("✅ Activity closed notification sent to userId={}, email={}", userId,
          user.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send activity closed notification to userId={}, email={}", userId,
          user.getLogin(), e);
    }
  }

  @Async("taskExecutor")
  public void sendNewJoinRequest(Integer adminId, String roomName, String requesterUsername) {
    log.debug("Attempting to send new join request notification to adminId={}, room={}", adminId,
        roomName);

    User admin = userRepository.getUserById(adminId);
    if (admin == null || admin.getLogin() == null) {
      log.warn("Cannot send notification: admin {} not found or has no email", adminId);
      return;
    }

    if (!shouldNotifyUser(admin, Notification.NEW_JOIN_REQUEST)) {
      log.debug("Admin {} has disabled NEW_JOIN_REQUEST notifications", adminId);
      return;
    }

    String subject = "📥 New join request for " + roomName;
    String message = String.format("""
        Hello Room Administrator!

        There's a new join request for your room "%s".

        User: %s
        Action Required: Please review this request in your room administration panel.

        The CoActivity Team
        """, roomName, requesterUsername);

    try {
      dispatchEmail(admin.getLogin(), subject, message);
      log.info("✅ New join request notification sent to adminId={}, email={}", adminId,
          admin.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send new join request notification to adminId={}, email={}", adminId,
          admin.getLogin(), e);
    }
  }

  @Async("taskExecutor")
  public void sendLoginVerificationCode(String userEmail, String verificationCode) {
    log.debug("Attempting to send login verification code to email={}", userEmail);

    String subject = "🔐 Your CoActivity verification code";
    String message = String.format("""
        Hello!

        Use the verification code below to finish signing in:

        %s

        The code expires in 10 minutes. If you didn't request this, you can safely ignore this email.

        Stay secure,
        The CoActivity Team
        """, verificationCode);

    try {
      dispatchEmail(userEmail, subject, message);
      log.info("✅ Login verification code sent to email={}", userEmail);
    } catch (Exception e) {
      log.error("❌ Failed to send login verification code to email={}", userEmail, e);
    }
  }

  private boolean shouldNotifyUser(User user, Notification notificationType) {
    try {
      List<Notification> enabledNotifications = user.getNotifications();
      if (enabledNotifications == null || enabledNotifications.isEmpty()) {
        log.debug("User {} has no notification preferences set", user.getId());
        return false;
      }

      return enabledNotifications.contains(notificationType);
    } catch (Exception e) {
      log.error("Error checking notification preferences for userId={}, type={}", user.getId(),
          notificationType, e);
      return false;
    }
  }

  private void dispatchEmail(String to, String subject, String message) {
    if (to == null || to.isBlank()) {
      return;
    }

    if (isKafkaMode()) {
      publishEmailCommand(to, subject, message);
      return;
    }

    if (isServiceMode()) {
      notificationsRestClient.post()
          .uri("/api/notifications/email")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new SendEmailRequest(to, subject, message))
          .retrieve()
          .toBodilessEntity();
      return;
    }

    mailService.sendSimpleMessage(to, subject, message);
  }

  private void publishEmailCommand(String to, String subject, String body) {
    if (kafkaTemplate == null) {
      throw new IllegalStateException(
          "KafkaTemplate is not configured while NOTIFICATIONS_MODE=KAFKA");
    }

    String payload;
    try {
      payload = objectMapper.writeValueAsString(new SendEmailRequest(to, subject, body));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize email command for Kafka", e);
    }

    try {
      kafkaTemplate.send(notificationsKafkaTopic, to, payload)
          .get(notificationsKafkaSendTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka publish interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to publish email command to Kafka", e);
    }
  }

  private boolean isServiceMode() {
    String mode = notificationsMode == null ? MODE_INPROC : notificationsMode;
    return MODE_SERVICE.equalsIgnoreCase(mode.trim().toUpperCase(Locale.ROOT));
  }

  private boolean isKafkaMode() {
    String mode = notificationsMode == null ? MODE_INPROC : notificationsMode;
    return MODE_KAFKA.equalsIgnoreCase(mode.trim().toUpperCase(Locale.ROOT));
  }

  private record SendEmailRequest(
      String to,
      String subject,
      String body) {
  }
}
