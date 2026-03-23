package com.coactivity.service;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Publishes email commands to Kafka after checking user notification preferences.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private static final String DEFAULT_NOTIFICATIONS_KAFKA_TOPIC = "notifications.email.v1";
  private static final long DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS = 5000L;

  private final UserRepository userRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private KafkaTemplate<String, String> kafkaTemplate;
  private String notificationsKafkaTopic = DEFAULT_NOTIFICATIONS_KAFKA_TOPIC;
  private long notificationsKafkaSendTimeoutMs = DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS;

  public NotificationService(UserRepository userRepository) {
    this.userRepository = userRepository;
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
   * Synchronous variant used when the caller needs delivery status.
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

    String subject = "Welcome to " + roomName + "!";
    String message = String.format("""
         Hello!

         Your request to join "%s" has been accepted.
         You can now participate in all room activities and discussions.

         Happy collaborating!
         The CoActivity Team

""", roomName);

    try {
      boolean delivered = publishEmailCommand(user.getLogin(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish membership accepted notification for userId={}", userId);
        return false;
      }
      log.info("Membership accepted email command published to Kafka for userId={}, email={}", userId,
          user.getLogin());
      return true;
    } catch (Exception e) {
      log.error("Failed to send membership accepted notification to userId={}, email={}", userId,
          user.getLogin(), e);
      return false;
    }
  }

  /**
   * Synchronous variant used when the caller needs delivery status.
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

    String subject = "Membership request update for " + roomName;
    String message = String.format("""
         Hello!

         Your request to join "%s" has been reviewed but unfortunately
         we cannot accept your participation at this time.

         You can explore other rooms that might be a better fit!

         The CoActivity Team

""", roomName);

    try {
      boolean delivered = publishEmailCommand(user.getLogin(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish membership rejected notification for userId={}", userId);
        return false;
      }
      log.info("Membership rejected email command published to Kafka for userId={}, email={}", userId,
          user.getLogin());
      return true;
    } catch (Exception e) {
      log.error("Failed to send membership rejected notification to userId={}, email={}", userId,
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
      boolean delivered = publishEmailCommand(user.getLogin(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish activity closed notification for userId={}", userId);
        return;
      }
      log.info("Activity closed email command published to Kafka for userId={}, email={}", userId,
          user.getLogin());
    } catch (Exception e) {
      log.error("Failed to send activity closed notification to userId={}, email={}", userId,
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

    String subject = "New join request for " + roomName;
    String message = String.format("""
        Hello Room Administrator!

        There's a new join request for your room "%s".

        User: %s
        Action Required: Please review this request in your room administration panel.

        The CoActivity Team
        """, roomName, requesterUsername);

    try {
      boolean delivered = publishEmailCommand(admin.getLogin(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish new join request notification for adminId={}", adminId);
        return;
      }
      log.info("New join request email command published to Kafka for adminId={}, email={}", adminId,
          admin.getLogin());
    } catch (Exception e) {
      log.error("Failed to send new join request notification to adminId={}, email={}", adminId,
          admin.getLogin(), e);
    }
  }

  public boolean sendLoginVerificationCode(String userEmail, String verificationCode) {
    log.debug("Attempting to send login verification code to email={}", userEmail);

    String subject = "Your CoActivity verification code";
    String message = String.format("""
        Hello!

        Use the verification code below to finish signing in:

        %s

        The code expires in 10 minutes. If you didn't request this, you can safely ignore this email.

        Stay secure,
        The CoActivity Team
        """, verificationCode);

    try {
      boolean delivered = publishEmailCommand(userEmail, subject, message);
      if (!delivered) {
        log.warn("Failed to publish login verification code for email={}", userEmail);
        return false;
      }
      log.info("Login verification email command published to Kafka for email={}", userEmail);
      return true;
    } catch (Exception e) {
      log.error("Failed to send login verification code to email={}", userEmail, e);
      return false;
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

  private boolean publishEmailCommand(String to, String subject, String body) {
    if (to == null || to.isBlank()) {
      return true;
    }
    if (kafkaTemplate == null) {
      log.warn("KafkaTemplate is not configured, email command will be skipped");
      return false;
    }

    String payload;
    try {
      payload = objectMapper.writeValueAsString(new SendEmailRequest(to, subject, body));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize email command for Kafka", e);
      return false;
    }

    try {
      kafkaTemplate.send(notificationsKafkaTopic, to, payload)
          .get(notificationsKafkaSendTimeoutMs, TimeUnit.MILLISECONDS);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Kafka publish interrupted", e);
      return false;
    } catch (Exception e) {
      log.error("Failed to publish email command to Kafka", e);
      return false;
    }
  }

  private record SendEmailRequest(
      String to,
      String subject,
      String body) {
  }
}
