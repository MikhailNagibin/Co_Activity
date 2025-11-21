package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.springframework.stereotype.Service;

// TODO: UserWithRoomService that contains following methods:
//  assignAdminRole, demoteAdminRole, getBanRooms, getUserRooms, joinRoom, leaveRoom,
//  getRoomParticipants, isUserInRoom
@Service
public class UserWithRoomService {

  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;
  private final RoomsRequestRepositoryImpl roomsRequestRepository;
  private final TokenService tokenService;

  public UserWithRoomService(UserRepositoryImpl userRepository,
      RoomRepositoryImpl roomRepository,
      RoomsRequestRepositoryImpl roomsRequestRepository,
      TokenService tokenService) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
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

  /**
   * Handles room join logic for both public and private rooms.
   * <p>
   * - For public rooms: user is added immediately as PARTICIPANT (if not banned and capacity not exceeded).<br>
   * - For private rooms: a join request with status {@link RequestStatus#CONSIDERATION} is created.
   * </p>
   */
  public ApiResponse<Void> joinRoom(String token, Integer roomId) {
    if (token == null || roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      if (!tokenService.isTokenActive(token)) {
        return ApiResponse.error("401");
      }

      Integer userId = tokenService.decodeToken(token).userId();
      User user = userRepository.getUserById(userId);
      Room room = roomRepository.getRoomById(roomId);

      if (user == null || room == null) {
        return ApiResponse.error("404");
      }

      // Check if user is banned in this room
      if (room.getBans() != null && room.getBans().contains(user)) {
        return ApiResponse.error("403");
      }

      // Check if user is already a member
      if (roomRepository.isUserInMembers(roomId, userId)) {
        return ApiResponse.success(null);
      }

      // Capacity check
      int currentParticipants =
          room.getUsers() != null ? room.getUsers().size() : 0;
      if (currentParticipants >= room.getMaximumNumberOfPeople()) {
        return ApiResponse.error("409"); // capacity exceeded
      }

      if (room.isPublic()) {
        // Public room – add immediately as PARTICIPANT
        roomRepository.addUserToRoom(roomId, userId, Role.PARTICIPANT);
        return ApiResponse.success(null);
      } else {
        // Private room – create join request in CONSIDERATION state
        roomsRequestRepository.createRequest(userId, roomId, RequestStatus.CONSIDERATION);
        return ApiResponse.success(null);
      }

    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }
}
