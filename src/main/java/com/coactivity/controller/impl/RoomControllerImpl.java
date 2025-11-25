package com.coactivity.controller.impl;

import com.coactivity.controller.RoomController;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.domain.Role;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserWithRoomService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomControllerImpl implements RoomController {

  private final RoomService roomService;
  private final TokenService tokenService;
  private final UserWithRoomService userWithRoomService;
  private final BulletinBoardService bulletinBoardService;

  public RoomControllerImpl(RoomService roomService,
      TokenService tokenService,
      UserWithRoomService userWithRoomService,
      BulletinBoardService bulletinBoardService) {
    this.roomService = roomService;
    this.tokenService = tokenService;
    this.userWithRoomService = userWithRoomService;
    this.bulletinBoardService = bulletinBoardService;
  }

  @Override
  public ApiResponse<RoomCreationResponse> createRoom(String token, RoomCreationRequest request) {
    return roomService.createRoom(token, request);
  }

  @Override
  public ApiResponse<BulletinBoardResponse> updateBulletinBoard(String token, Integer roomId,
      String newContent) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (roomId == null || newContent == null || newContent.trim().isEmpty()) {
      return ApiResponse.error("400");
    }
    Integer authorId = tokenService.decodeToken(token).userId();
    return bulletinBoardService.updateBulletinBoard(roomId, newContent, authorId);
  }

  @Override
  public ApiResponse<Void> deleteBulletinBoard(String token, Integer roomId) {
    if (roomId == null) {
      return ApiResponse.error("400");
    }
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    return bulletinBoardService.deleteBulletinBoard(roomId);
  }

  @Override
  public ApiResponse<List<RoomSummaryResponse>> getRooms(String token, RoomFilter filter,
      RoomSort sortBy) {
    return roomService.getRooms(token, filter, sortBy);
  }

  @Override
  public ApiResponse<RoomDetailedResponse> getRoomById(Integer roomId, String token) {
    return roomService.getRoomById(roomId, token);
  }

  @Override
  public ApiResponse<List<RoomDetailedResponse>> getUserRooms(String token) {
    return userWithRoomService.getUserRooms(token);
  }

  @Override
  public ApiResponse<Void> joinRoom(String token, Integer roomId) {
    return userWithRoomService.joinRoom(token, roomId);
  }

  @Override
  public ApiResponse<Void> leaveRoom(String token, Integer roomId) {
    return userWithRoomService.leaveRoom(token, roomId);
  }

  @Override
  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (roomId == null) {
      return ApiResponse.error("400");
    }
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    return roomService.deleteRoom(token, roomId);
  }

  @Override
  public ApiResponse<List<RoomParticipantResponse>> getRoomParticipants(String token,
      Integer roomId, Role roleFilter) {
    return userWithRoomService.getRoomParticipants(token, roomId, roleFilter);
  }

  @Override
  public ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId,
      Integer userId) {
    return userWithRoomService.verifyUserMembership(token, roomId, userId);
  }
}
