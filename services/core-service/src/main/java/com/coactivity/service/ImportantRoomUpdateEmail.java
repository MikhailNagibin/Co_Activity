package com.coactivity.service;

import com.coactivity.domain.RoomStatus;
import java.time.Instant;

public record ImportantRoomUpdateEmail(
    String roomName,
    RoomStatus oldStatus,
    RoomStatus newStatus,
    boolean statusChanged,
    Instant oldStart,
    Instant newStart,
    Instant oldEnd,
    Instant newEnd,
    Instant oldFrequency,
    Instant newFrequency,
    boolean scheduleChanged,
    String oldChatLink,
    String newChatLink,
    boolean chatLinkChanged) {

  public boolean hasAnyChange() {
    return statusChanged || scheduleChanged || chatLinkChanged;
  }
}
