package com.coactivity.domain;

public enum Notification {
  MEMBERSHIP_ACCEPTED,
  MEMBERSHIP_REJECTED,
  ACTIVITY_CLOSED,
  NEW_JOIN_REQUEST;

  public static Notification getByIndex(int index) {
    Notification[] notification = values();
    if (index >= 0 && index < notification.length) {
      return notification[index];
    }
    throw new IllegalArgumentException("Invalid role notification: " + index);
  }
}
