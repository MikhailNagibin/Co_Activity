package com.coactivity.service.event;

import com.coactivity.service.ImportantRoomUpdateEmail;
import java.util.List;

public record RoomUpdateNotificationEvent(
    List<Integer> participantIds,
    ImportantRoomUpdateEmail importantUpdate,
    List<PendingRequestNotification> pendingRequestNotifications) {

  public RoomUpdateNotificationEvent {
    participantIds = participantIds != null ? List.copyOf(participantIds) : List.of();
    pendingRequestNotifications = pendingRequestNotifications != null
        ? List.copyOf(pendingRequestNotifications)
        : List.of();
  }

  public boolean hasNotifications() {
    return (importantUpdate != null && importantUpdate.hasAnyChange())
        || !pendingRequestNotifications.isEmpty();
  }

  public record PendingRequestNotification(
      Integer userId,
      String roomName,
      PendingRequestNotificationType type,
      String declineReason) {
  }

  public enum PendingRequestNotificationType {
    AUTO_DECLINED,
    APPROVAL_NO_LONGER_NEEDED
  }
}
