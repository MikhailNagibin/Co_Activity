package com.coactivity.domain;

public enum Notification {
  MEMBERSHIP_ACCEPTED("membershipAccepted"),
  MEMBERSHIP_REJECTED("membershipRejected"),
  ACTIVITY_CLOSED("activityClosed"),
  NEW_JOIN_REQUEST("newJoinRequest");

  private final String notificationName;

  public static Notification getByIndex(int index) {
    Notification[] notification = values();
    if (index >= 0 && index < notification.length) {
      return notification[index];
    }
    throw new IllegalArgumentException("Invalid role notification: " + index);
  }

  Notification(String notificationName) {
    this.notificationName = notificationName;
  }

  @Override
  public String toString() {
    return notificationName;
  }
}
