package com.coactivity.domain.enums;

public enum Notification {
  membershipAccepted,
  membershipRejected,
  activityClosed,
  newJoinRequest;

  public static Notification getByIndex(int index) {
    Notification[] notification = values();
    if (index >= 0 && index < notification.length) {
      return notification[index];
    }
    throw new IllegalArgumentException("Invalid role notification: " + index);
  }
}
