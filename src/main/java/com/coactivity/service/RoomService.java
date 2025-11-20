package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.domain.Role;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final RoomRepositoryImpl roomRepository;
  private final TokenService tokenService;

  public RoomService(RoomRepositoryImpl roomRepository, TokenService tokenService) {
    this.roomRepository = roomRepository;
    this.tokenService = tokenService;
  }

  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (roomRepository.getUserRoleByRoomId(roomId, tokenService.decodeToken(token).userId()).equals(
        Role.OWNER)) {
      roomRepository.deleteRoom(roomId);
      return ApiResponse.success(null);
    } else {
      return ApiResponse.error("401");
    }
  }
  // TODO: add methods: createRoom, getRooms, getRoomById, deleteRoom
  // TODO: BulletinBoardService: updateBulletinBoard
}
