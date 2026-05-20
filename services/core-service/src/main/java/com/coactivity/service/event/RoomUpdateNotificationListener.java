package com.coactivity.service.event;

import com.coactivity.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoomUpdateNotificationListener {

  private final NotificationService notificationService;

  public RoomUpdateNotificationListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRoomUpdateNotification(RoomUpdateNotificationEvent event) {
    if (event == null || !event.hasNotifications()) {
      return;
    }

    if (event.importantUpdate() != null && event.importantUpdate().hasAnyChange()) {
      for (Integer participantId : event.participantIds()) {
        notificationService.sendImportantRoomUpdate(participantId, event.importantUpdate());
      }
    }

    for (RoomUpdateNotificationEvent.PendingRequestNotification notification
        : event.pendingRequestNotifications()) {
      if (notification.type()
          == RoomUpdateNotificationEvent.PendingRequestNotificationType.APPROVAL_NO_LONGER_NEEDED) {
        notificationService.sendPendingRequestApprovalNoLongerNeeded(notification.userId(),
            notification.roomName());
        continue;
      }
      notificationService.sendPendingRequestAutoDeclined(notification.userId(),
          notification.roomName(), notification.declineReason());
    }
  }
}
