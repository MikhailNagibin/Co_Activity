package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.domain.Role;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.springframework.stereotype.Service;

@Service
// TODO: UserWithRoomService that contains following methods:
//  assignAdminRole, demoteAdminRole, getBanRooms, getUserRooms, joinRoom, leaveRoom,
//  getRoomParticipants, isUserInRoom
public class UserWithRoomService {
  private final UserRepositoryImpl userRepository;
  private final DataRepository dataRepository;
  private final RoomRepositoryImpl roomRepository;
  private final TokenService tokenService;

  public UserWithRoomService(DataRepository repository,
                            UserRepositoryImpl userRepository,
                            RoomRepositoryImpl roomRepository,
                            TokenService tokenService) {
    this.dataRepository = repository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.tokenService = tokenService;
  }

  public ApiResponse<Void> assignAdminRole(String token, Integer roomId,
                                           Integer userId) {

    Integer roomOwnerId = tokenService.decodeToken(token).userId();
    try {
      if (!roomRepository.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      roomRepository.setRoleByUserIdAndRoomId(userId, roomId, Role.ADMIN);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {

    Integer roomOwnerId = tokenService.decodeToken(token).userId();
    try {
      if (!roomRepository.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      roomRepository.setRoleByUserIdAndRoomId(userId, roomId, Role.PARTICIPANT);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }
}
