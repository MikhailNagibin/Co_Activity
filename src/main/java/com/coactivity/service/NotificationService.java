package com.coactivity.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private final MailService mailService;

  public NotificationService(MailService mailService) {
    this.mailService = mailService;
  }

  /**
   * Sends notification when a user's membership request is accepted.
   *
   * @param userEmail the email of the user who was accepted
   * @param roomName  the name of the room they were accepted into
   */
  public void sendMembershipAccepted(String userEmail, String roomName) {
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

    mailService.sendSimpleMessage(userEmail, subject, message);
  }

  /**
   * Sends notification when a user's membership request is rejected.
   *
   * @param userEmail the email of the user who was rejected
   * @param roomName  the name of the room they were rejected from
   */
  public void sendMembershipRejected(String userEmail, String roomName) {
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

    mailService.sendSimpleMessage(userEmail, subject, message);
  }

  /**
   * Sends notification when an activity/room is closed.
   *
   * @param userEmail the email of the room participant
   * @param roomName  the name of the room that was closed
   */
  public void sendActivityClosed(String userEmail, String roomName) {
    String subject = "🔒 Activity closed: " + roomName;
    String message = String.format("""
        Hello!
        
        The activity "%s" has been closed.
        All participants have been removed and the room is no longer active.
        
        Thank you for your participation!
        
        The CoActivity Team
        """, roomName);

    mailService.sendSimpleMessage(userEmail, subject, message);
  }

  /**
   * Sends notification to room administrators about a new join request.
   *
   * @param adminEmail        the email of the room administrator
   * @param roomName          the name of the room with the new request
   * @param requesterUsername the username of the person requesting to join
   */
  public void sendNewJoinRequest(String adminEmail, String roomName, String requesterUsername) {
    String subject = "📥 New join request for " + roomName;
    String message = String.format("""
        Hello Room Administrator!
        
        There's a new join request for your room "%s".
        
        User: %s
        Action Required: Please review this request in your room administration panel.
        
        The CoActivity Team
        """, roomName, requesterUsername);

    mailService.sendSimpleMessage(adminEmail, subject, message);
  }
}
