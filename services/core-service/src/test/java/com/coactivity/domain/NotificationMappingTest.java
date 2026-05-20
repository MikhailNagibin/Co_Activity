package com.coactivity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notification mapping tests")
class NotificationMappingTest {

  @Test
  @DisplayName("Should parse DB and API values")
  void shouldParseValues() {
    assertEquals(Notification.MEMBERSHIP_ACCEPTED, Notification.fromValue("MembershipAccepted"));
    assertEquals(Notification.MEMBERSHIP_ACCEPTED, Notification.fromValue("membershipAccepted"));
    assertEquals(Notification.NEW_JOIN_REQUEST, Notification.fromValue("NewJoinRequest"));
    assertEquals(Notification.NEW_JOIN_REQUEST, Notification.fromValue("new_join_request"));
    assertEquals(Notification.IMPORTANT_ROOM_UPDATES,
        Notification.fromValue("importantRoomUpdates"));
  }

  @Test
  @DisplayName("Should support both 1-based and 0-based indexes")
  void shouldSupportIndexes() {
    assertEquals(Notification.MEMBERSHIP_ACCEPTED, Notification.getByIndex(1));
    assertEquals(Notification.MEMBERSHIP_ACCEPTED, Notification.getByIndex(0));
  }

  @Test
  @DisplayName("Should reject unknown values")
  void shouldRejectUnknownValues() {
    assertThrows(IllegalArgumentException.class, () -> Notification.fromValue("unknown"));
  }
}
