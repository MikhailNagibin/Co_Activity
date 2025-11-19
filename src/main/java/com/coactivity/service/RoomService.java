package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.domain.Role;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final DataRepository repository;
  private final RoomRepositoryImpl roomRepository;
  private final TokenService tokenService;

  public RoomService() {
    this.repository = new DataRepository();
    this.roomRepository = new RoomRepositoryImpl(repository);
    this.tokenService = new TokenService();
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
