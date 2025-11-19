package com.coactivity.service;

import com.coactivity.AuthToken;
import com.coactivity.DataRepository;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;


public class RoomService {
  private DataRepository repository;
  private RoomRepositoryImpl rooms;

  public RoomService() {
    repository = new DataRepository();
    rooms = new RoomRepositoryImpl(repository);
  }

  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (rooms.getUserRoleByRoomId(roomId, AuthToken.getId(token)).equals("Owner")) {
      rooms.deleteRoom(roomId);
      return ApiResponse.success(null);
    } else {
      return ApiResponse.error("401");
    }
  }

  // TODO: add methods: createRoom, getRooms, getRoomById, deleteRoom

  // TODO: BulletinBoardService: updateBulletinBoard
}
