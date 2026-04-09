package com.coactivity.domain;

public enum Notification {
  MEMBERSHIP_ACCEPTED("membershipAccepted"),
  MEMBERSHIP_REJECTED("membershipRejected"),
  ACTIVITY_CLOSED("activityClosed"),
  NEW_JOIN_REQUEST("newJoinRequest"),
  IMPORTANT_ROOM_UPDATES("importantRoomUpdates");

  private final String notificationName;

  public static Notification getByIndex(int index) {
    Notification[] notification = values();
    // Support both legacy 0-based and DB 1-based indexes.
    if (index >= 1 && index <= notification.length) {
      return notification[index - 1];
    }
    if (index >= 0 && index < notification.length) {
      return notification[index];
    }
    throw new IllegalArgumentException("Invalid role notification: " + index);
  }

  public static Notification fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Notification value is blank");
    }
    String normalized = value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    return switch (normalized) {
      case "membershipaccepted" -> MEMBERSHIP_ACCEPTED;
      case "membershiprejected" -> MEMBERSHIP_REJECTED;
      case "activityclosed" -> ACTIVITY_CLOSED;
      case "newjoinrequest" -> NEW_JOIN_REQUEST;
      case "importantroomupdates" -> IMPORTANT_ROOM_UPDATES;
      default -> throw new IllegalArgumentException("Unknown notification value: " + value);
    };
  }

  Notification(String notificationName) {
    this.notificationName = notificationName;
  }

  @Override
  public String toString() {
    return notificationName;
  }
}
