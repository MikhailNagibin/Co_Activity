package com.coactivity.service;

import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.domain.UserNotification;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles asynchronous email notifications with user preference checking. All notification methods
 * run in background threads and respect user notification settings.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final MailService mailService;
  private final UserRepositoryImpl userRepository;

  public NotificationService(MailService mailService, UserRepositoryImpl userRepository) {
    this.mailService = mailService;
    this.userRepository = userRepository;
  }

  /**
   * Sends notification when a user's membership request is accepted. Runs asynchronously with user
   * preference checking.
   *
   * @param userId   the ID of the user who was accepted
   * @param roomName the name of the room they were accepted into
   */
  @Async("taskExecutor")
  public void sendMembershipAccepted(Integer userId, String roomName) {
    log.debug("Attempting to send membership accepted notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || user.getLogin() == null) {
      log.warn("Cannot send notification: user {} not found or has no email", userId);
      return;
    }

    if (!shouldNotifyUser(user, Notification.MEMBERSHIP_ACCEPTED)) {
      log.debug("User {} has disabled MEMBERSHIP_ACCEPTED notifications", userId);
      return;
    }

    String subject = "🎉 Welcome to " + roomName + "!";
    String message = String.format("""
         Hello!
        \s
         Your request to join "%s" has been accepted.\s
         You can now participate in all room activities and discussions.
        \s
         Happy collaborating!
         The CoActivity Team
        \s""", roomName);

    try {
      mailService.sendSimpleMessage(user.getLogin(), subject, message);
      log.info("✅ Membership accepted notification sent to userId={}, email={}", userId,
          user.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send membership accepted notification to userId={}, email={}", userId,
          user.getLogin(), e);
    }
  }

  /**
   * Sends notification when a user's membership request is rejected. Runs asynchronously with user
   * preference checking.
   *
   * @param userId   the ID of the user who was rejected
   * @param roomName the name of the room they were rejected from
   */
  @Async("taskExecutor")
  public void sendMembershipRejected(Integer userId, String roomName) {
    log.debug("Attempting to send membership rejected notification to userId={}, room={}", userId,
        roomName);

    User user = userRepository.getUserById(userId);
    if (user == null || user.getLogin() == null) {
      log.warn("Cannot send notification: user {} not found or has no email", userId);
      return;
    }

    if (!shouldNotifyUser(user, Notification.MEMBERSHIP_REJECTED)) {
      log.debug("User {} has disabled MEMBERSHIP_REJECTED notifications", userId);
      return;
    }

    String subject = "❌ Membership request update for " + roomName;
    String message = String.format("""
         Hello!
        \s
         Your request to join "%s" has been reviewed but unfortunately\s
         we cannot accept your participation at this time.
        \s
         You can explore other rooms that might be a better fit!
        \s
         The CoActivity Team
        \s""", roomName);

    try {
      mailService.sendSimpleMessage(user.getLogin(), subject, message);
      log.info("✅ Membership rejected notification sent to userId={}, email={}", userId,
          user.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send membership rejected notification to userId={}, email={}", userId,
          user.getLogin(), e);
    }
  }

  /**
   * Sends notification when an activity/room is closed. Runs asynchronously with user preference
   * checking.
   *
   * @param userId   the ID of the room participant
   * @param roomName the name of the room that was closed
   */
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
      mailService.sendSimpleMessage(user.getLogin(), subject, message);
      log.info("✅ Activity closed notification sent to userId={}, email={}", userId,
          user.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send activity closed notification to userId={}, email={}", userId,
          user.getLogin(), e);
    }
  }

  /**
   * Sends notification to room administrators about a new join request. Runs asynchronously with
   * user preference checking.
   *
   * @param adminId           the ID of the room administrator
   * @param roomName          the name of the room with the new request
   * @param requesterUsername the username of the person requesting to join
   */
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
      mailService.sendSimpleMessage(admin.getLogin(), subject, message);
      log.info("✅ New join request notification sent to adminId={}, email={}", adminId,
          admin.getLogin());
    } catch (Exception e) {
      log.error("❌ Failed to send new join request notification to adminId={}, email={}", adminId,
          admin.getLogin(), e);
    }
  }

  /**
   * Sends a login verification code to the user. This notification always sends regardless of user
   * preferences (security requirement). Runs asynchronously.
   *
   * @param userEmail        the email of the user attempting to log in
   * @param verificationCode the one-time verification code
   */
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
      mailService.sendSimpleMessage(userEmail, subject, message);
      log.info("✅ Login verification code sent to email={}", userEmail);
    } catch (Exception e) {
      log.error("❌ Failed to send login verification code to email={}", userEmail, e);
    }
  }

  /**
   * Checks if a user has enabled a specific notification type.
   *
   * @param user             the user to check
   * @param notificationType the type of notification
   * @return true if the user should receive this notification, false otherwise
   */
  private boolean shouldNotifyUser(User user, String notificationType) {
    try {
      for (UserNotification userNotification : user.getNotifications()) {
        if (userNotification.getNotification().getDescription().equals(notificationType)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      log.error("Error checking notification preferences for userId={}, type={}", user.getId(),
          notificationType, e);
      return false;
    }
  }
}
