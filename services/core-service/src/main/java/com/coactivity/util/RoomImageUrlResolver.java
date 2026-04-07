package com.coactivity.util;

public final class RoomImageUrlResolver {

  private RoomImageUrlResolver() {
  }

  public static String resolveRoomImageUrl(Integer roomId, Integer imageId) {
    if (roomId == null || imageId == null) {
      return null;
    }
    return "/api/rooms/" + roomId + "/images/" + imageId;
  }
}
