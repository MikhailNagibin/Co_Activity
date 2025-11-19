package com.coactivity.controller.impl;

import com.coactivity.service.AuthTokenService;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.service.RoomService;

public class RoomControllerImpl {
  private final RoomService roomService = new RoomService();


  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (!AuthTokenService.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }
    return roomService.deleteRoom(token, roomId);
  }

}
