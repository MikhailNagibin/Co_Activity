package com.coactivity.service;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
  private final ObjectMapper objectMapper;

  private KafkaTemplate<String, String> kafkaTemplate;
  private String notificationsKafkaTopic = DEFAULT_NOTIFICATIONS_KAFKA_TOPIC;
  private long notificationsKafkaSendTimeoutMs = DEFAULT_NOTIFICATIONS_KAFKA_SEND_TIMEOUT_MS;

  public NotificationService(UserRepository userRepository, ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
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
    if (user == null || !hasUsableEmail(user.getEmail())) {
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
      boolean delivered = publishEmailCommand(user.getEmail(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish membership accepted notification for userId={}", userId);
        return false;
      }
      log.info("Membership accepted email command published to Kafka for userId={}, email={}", userId,
          user.getEmail());
      return true;
    } catch (Exception e) {
      log.error("Failed to send membership accepted notification to userId={}, email={}", userId,
          user.getEmail(), e);
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
    if (user == null || !hasUsableEmail(user.getEmail())) {
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
      boolean delivered = publishEmailCommand(user.getEmail(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish membership rejected notification for userId={}", userId);
        return false;
      }
      log.info("Membership rejected email command published to Kafka for userId={}, email={}", userId,
          user.getEmail());
      return true;
    } catch (Exception e) {
      log.error("Failed to send membership rejected notification to userId={}, email={}", userId,
          user.getEmail(), e);
      return false;
    }
  }

  @Async("taskExecutor")
  public void sendActivityClosed(Integer userId, String roomName) {
    log.debug("Attempting to send activity closed notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || !hasUsableEmail(user.getEmail())) {
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
      boolean delivered = publishEmailCommand(user.getEmail(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish activity closed notification for userId={}", userId);
        return;
      }
      log.info("Activity closed email command published to Kafka for userId={}, email={}", userId,
          user.getEmail());
    } catch (Exception e) {
      log.error("Failed to send activity closed notification to userId={}, email={}", userId,
          user.getEmail(), e);
    }
  }

  @Async("taskExecutor")
  public void sendNewJoinRequest(Integer adminId, String roomName, String requesterUsername) {
    log.debug("Attempting to send new join request notification to adminId={}, room={}", adminId,
        roomName);

    User admin = userRepository.getUserById(adminId);
    if (admin == null || !hasUsableEmail(admin.getEmail())) {
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
      boolean delivered = publishEmailCommand(admin.getEmail(), subject, message);
      if (!delivered) {
        log.warn("Failed to publish new join request notification for adminId={}", adminId);
        return;
      }
      log.info("New join request email command published to Kafka for adminId={}, email={}", adminId,
          admin.getEmail());
    } catch (Exception e) {
      log.error("Failed to send new join request notification to adminId={}, email={}", adminId,
          admin.getEmail(), e);
    }
  }

  @Async("taskExecutor")
  public void sendImportantRoomUpdate(Integer userId, ImportantRoomUpdateEmail update) {
    sendImportantRoomUpdateSync(userId, update);
  }

  public boolean sendImportantRoomUpdateSync(Integer userId, ImportantRoomUpdateEmail update) {
    if (update == null || !update.hasAnyChange()) {
      return true;
    }

    User user = getNotifiableUser(userId, Notification.IMPORTANT_ROOM_UPDATES,
        "important room update");
    if (user == null) {
      return true;
    }

    String subject = "Important room update: " + update.roomName();
    String message = buildImportantRoomUpdateMessage(update);
    return publishWithLogging(user.getEmail(), subject, message,
        "important room update notification", userId);
  }

  @Async("taskExecutor")
  public void sendPendingRequestAutoDeclined(Integer userId, String roomName, String reason) {
    sendPendingRequestAutoDeclinedSync(userId, roomName, reason);
  }

  public boolean sendPendingRequestAutoDeclinedSync(Integer userId, String roomName, String reason) {
    User user = getNotifiableUser(userId, Notification.IMPORTANT_ROOM_UPDATES,
        "auto-declined pending request");
    if (user == null) {
      return true;
    }

    String subject = "Join request update for " + roomName;
    String message = String.format("""
        Hello!

        Room: %s
        Event type: Important room update
        What changed: The room update automatically affected your pending join request.
        What it means for you: Your request was declined because %s.

        The CoActivity Team
        """, roomName, reason);
    return publishWithLogging(user.getEmail(), subject, message,
        "auto-declined pending request notification", userId);
  }

  @Async("taskExecutor")
  public void sendPendingRequestApprovalNoLongerNeeded(Integer userId, String roomName) {
    sendPendingRequestApprovalNoLongerNeededSync(userId, roomName);
  }

  public boolean sendPendingRequestApprovalNoLongerNeededSync(Integer userId, String roomName) {
    User user = getNotifiableUser(userId, Notification.IMPORTANT_ROOM_UPDATES,
        "pending request removed notification");
    if (user == null) {
      return true;
    }

    String subject = "Room access update for " + roomName;
    String message = String.format("""
        Hello!

        Room: %s
        Event type: Important room update
        What changed: The room became public, so manual approval is no longer required.
        What it means for you: Your pending request was removed because you can now join directly.

        The CoActivity Team
        """, roomName);
    return publishWithLogging(user.getEmail(), subject, message,
        "pending request removed notification", userId);
  }

  public boolean sendNewRoomFromFollowedUser(String email, String authorName, Integer roomId,
      String roomName) {
    if (!hasUsableEmail(email)) {
      log.warn("Cannot send followed-user room notification: email is blank");
      return false;
    }

    String effectiveAuthorName = hasText(authorName) ? authorName : "Unknown user";
    String effectiveRoomName = hasText(roomName) ? roomName : "Untitled room";
    String roomPath = roomId != null ? "/api/rooms/" + roomId : "/api/rooms";

    String subject = "New room by " + effectiveAuthorName + ": " + effectiveRoomName;
    String message = String.format("""
        Hello!

        User you follow: %s
        New room: %s
        Room link: %s

        The CoActivity Team
        """, effectiveAuthorName, effectiveRoomName, roomPath);

    return publishWithLogging(email, subject, message, "followed-user room notification", null);
  }

  public boolean sendRegistrationVerificationCode(String userEmail, String verificationCode) {
    log.debug("Attempting to send registration verification code to email={}", userEmail);
    if (!hasUsableEmail(userEmail)) {
      log.warn("Cannot send registration verification code: email is blank");
      return false;
    }

    String subject = "Подтверждение регистрации в CoActivity";
    String message = String.format("""
        Hello!

        Use the verification code below to finish creating your account:

        %s

        The code expires in 10 minutes. If you did not register in CoActivity, you can ignore this email.

        Stay secure,
        The CoActivity Team
        """, verificationCode);

    try {
      boolean delivered = publishEmailCommand(userEmail, subject, message);
      if (!delivered) {
        log.warn("Failed to publish registration verification code for email={}", userEmail);
        return false;
      }
      log.info("Registration verification email command published to Kafka for email={}", userEmail);
      return true;
    } catch (Exception e) {
      log.error("Failed to send registration verification code to email={}", userEmail, e);
      return false;
    }
  }

  public boolean sendPasswordResetCode(String userEmail, String resetCode) {
    log.debug("Attempting to send password reset code to email={}", userEmail);
    if (!hasUsableEmail(userEmail)) {
      log.warn("Cannot send password reset code: email is blank");
      return false;
    }

    String subject = "Password reset for CoActivity";
    String message = String.format("""
        Hello!

        Use the code below to reset your password:

        %s

        The code expires in 10 minutes. If you did not request a password reset, you can ignore this email.

        Stay secure,
        The CoActivity Team
        """, resetCode);

    return publishWithLogging(userEmail, subject, message, "password reset code", null);
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

  private User getNotifiableUser(Integer userId, Notification notificationType, String logContext) {
    log.debug("Attempting to send {} to userId={}", logContext, userId);

    User user = userRepository.getUserById(userId);
    if (user == null || !hasUsableEmail(user.getEmail())) {
      log.warn("Cannot send {}: user {} not found or has no email", logContext, userId);
      return null;
    }

    if (!shouldNotifyUser(user, notificationType)) {
      log.debug("User {} has disabled {} notifications", userId, notificationType.name());
      return null;
    }
    return user;
  }

  private boolean publishWithLogging(String email, String subject, String body, String logContext,
      Integer userId) {
    try {
      boolean delivered = publishEmailCommand(email, subject, body);
      if (!delivered) {
        if (userId != null) {
          log.warn("Failed to publish {} for userId={}", logContext, userId);
        } else {
          log.warn("Failed to publish {} for email={}", logContext, email);
        }
        return false;
      }
      if (userId != null) {
        log.info("{} email command published to Kafka for userId={}, email={}", logContext, userId,
            email);
      } else {
        log.info("{} email command published to Kafka for email={}", logContext, email);
      }
      return true;
    } catch (Exception e) {
      if (userId != null) {
        log.error("Failed to send {} to userId={}, email={}", logContext, userId, email, e);
      } else {
        log.error("Failed to send {} to email={}", logContext, email, e);
      }
      return false;
    }
  }

  private String buildImportantRoomUpdateMessage(ImportantRoomUpdateEmail update) {
    StringBuilder message = new StringBuilder("""
        Hello!

        Room: %s
        Event type: Important room update
        """.formatted(update.roomName()));

    if (update.statusChanged()) {
      message.append(System.lineSeparator())
          .append("What changed: Room status changed.")
          .append(System.lineSeparator())
          .append("Old status: ").append(formatStatus(update.oldStatus())).append(System.lineSeparator())
          .append("New status: ").append(formatStatus(update.newStatus())).append(System.lineSeparator())
          .append("What it means for you: ").append(statusConsequence(update.newStatus()))
          .append(System.lineSeparator());
    }

    if (update.scheduleChanged()) {
      message.append(System.lineSeparator())
          .append("What changed: Room schedule changed.")
          .append(System.lineSeparator())
          .append("Old start: ").append(formatInstant(update.oldStart())).append(System.lineSeparator())
          .append("New start: ").append(formatInstant(update.newStart())).append(System.lineSeparator())
          .append("Old end: ").append(formatInstant(update.oldEnd())).append(System.lineSeparator())
          .append("New end: ").append(formatInstant(update.newEnd())).append(System.lineSeparator());
      if (!sameInstant(update.oldFrequency(), update.newFrequency())) {
        message.append("Old frequency: ").append(formatInstant(update.oldFrequency()))
            .append(System.lineSeparator())
            .append("New frequency: ").append(formatInstant(update.newFrequency()))
            .append(System.lineSeparator());
      }
      message.append("What it means for you: Please check the updated schedule before participating.")
          .append(System.lineSeparator());
    }

    if (update.chatLinkChanged()) {
      message.append(System.lineSeparator())
          .append("What changed: The room chat link was updated.")
          .append(System.lineSeparator())
          .append("New chat link: ").append(formatChatLink(update.newChatLink()))
          .append(System.lineSeparator())
          .append("What it means for you: Use this link for future room communication.")
          .append(System.lineSeparator());
    }

    message.append(System.lineSeparator())
        .append("The CoActivity Team").append(System.lineSeparator());
    return message.toString();
  }

  private String statusConsequence(com.coactivity.domain.RoomStatus status) {
    if (status == null) {
      return "Check the room for the latest state.";
    }
    return switch (status) {
      case INACTIVE -> "The room is no longer active, so new participation is paused.";
      case COMPLETED -> "The activity is finished, so treat this room as completed.";
      case ACTIVE -> "The room is active.";
    };
  }

  private String formatStatus(com.coactivity.domain.RoomStatus status) {
    return status != null ? status.name() : "not set";
  }

  private String formatInstant(Instant instant) {
    return instant != null ? instant.toString() : "not set";
  }

  private String formatChatLink(String chatLink) {
    return hasText(chatLink) ? chatLink : "removed";
  }

  private boolean sameInstant(Instant left, Instant right) {
    return left == null ? right == null : left.equals(right);
  }

  private boolean publishEmailCommand(String to, String subject, String body) {
    if (!hasUsableEmail(to)) {
      log.warn("Email command recipient is blank");
      return false;
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

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean hasUsableEmail(String email) {
    return hasText(email);
  }
}
