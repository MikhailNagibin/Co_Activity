package com.coactivity.service.event;

import com.coactivity.repository.UserFollowRepository;
import com.coactivity.service.NotificationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoomCreatedNotificationListener {

  private static final Logger log = LoggerFactory.getLogger(RoomCreatedNotificationListener.class);

  private final UserFollowRepository userFollowRepository;
  private final NotificationService notificationService;

  public RoomCreatedNotificationListener(UserFollowRepository userFollowRepository,
      NotificationService notificationService) {
    this.userFollowRepository = userFollowRepository;
    this.notificationService = notificationService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRoomCreated(RoomCreatedNotificationEvent event) {
    if (event == null || event.ownerId() == null || event.roomId() == null) {
      return;
    }

    List<String> followerEmails = userFollowRepository.getFollowerEmails(event.ownerId());
    for (String followerEmail : followerEmails) {
      try {
        notificationService.sendNewRoomFromFollowedUser(
            followerEmail,
            event.ownerUserName(),
            event.roomId(),
            event.roomName());
      } catch (Exception exception) {
        log.warn("Failed to send followed-user room notification to {}", followerEmail, exception);
      }
    }
  }
}
