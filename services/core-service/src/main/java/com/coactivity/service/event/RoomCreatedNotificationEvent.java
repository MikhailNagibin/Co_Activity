package com.coactivity.service.event;

public record RoomCreatedNotificationEvent(
    Integer ownerId,
    String ownerUserName,
    Integer roomId,
    String roomName) {
}
