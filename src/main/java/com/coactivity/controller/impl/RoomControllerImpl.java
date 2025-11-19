package com.coactivity.controller.impl;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.service.RoomService;
import com.coactivity.service.TokenService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomControllerImpl {

  private final RoomService roomService;
  private final TokenService tokenService;

  public RoomControllerImpl(RoomService roomService, TokenService tokenService) {
    this.roomService = roomService;
    this.tokenService = tokenService;
  }

  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    return roomService.deleteRoom(token, roomId);
  }
}
