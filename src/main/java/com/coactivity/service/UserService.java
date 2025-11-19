package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;

public class UserService {
  private UserRepositoryImpl users;
  private DataRepository repository;
  private RoomRepositoryImpl rooms;
  private RoomsRequestRepositoryImpl requests;

  public UserService() {
    repository = new DataRepository();
    users = new UserRepositoryImpl(repository);
    rooms = new RoomRepositoryImpl(repository);
    requests = new RoomsRequestRepositoryImpl(repository);
  }
л
  public ApiResponse<UserProfileResponse> getUserProfile(int token) {
    var response = new UserProfileResponse();
    try {
      User user = users.getUserById(token);

      response.setId(user.getId());
      response.setCity(user.getCity());
      response.setAvatarId(user.getAvatarId());
      response.setDescription(user.getDescription());
      response.setCountry(user.getCountry());
      response.setEmail(user.getLogin());
      response.setUsername(user.getUsername());
      response.setDateOfBirth(user.getDataOfBirth());

      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.success(null);
    }
  }

  public ApiResponse<Void> updateUserProfile(int token, UserProfileUpdateRequest request) {
    User user = users.getUserById(token);
    try {
      users.updateUser(user.getId(), request);
      return ApiResponse.success(null);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Integer> deleteAccount(int token) {
    try {
      users.deleteUser(token);
      return ApiResponse.success(200);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Void> configureNotificationSettings(int token, NotificationSettingsRequest request) {
    try {
      if (request.getActivityClosed()) {
        users.setNotification(token, "activityClosed");
      }
      if (request.getNewJoinRequest()) {
        users.setNotification(token, "newJoinRequest");
      }
      if (request.getMembershipAccepted()) {
        users.setNotification(token, "membershipAccepted");
      }
      if (request.getMembershipRejected()) {
        users.setNotification(token, "membershipRejected");
      }
      return ApiResponse.success(null);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  public ApiResponse<Void> assignAdminRole(String token, Integer roomId,
                                           Integer userId) {

    int roomOwnerId = AuthTokenService.getId(token);
    try {
      if (!rooms.isUserOwnerInRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      rooms.getRoleByUserIdAndRoomId(userId, roomId, "admin");
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {

    int roomOwnerId = AuthTokenService.getId(token);
    try {
      if (!rooms.isUserOwnerInRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      rooms.getRoleByUserIdAndRoomId(userId, roomId, "Participant");
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> cancelRequest(String token, Integer requestId) {
    if (!requests.checkRequest(requestId, AuthTokenService.getId(token))) {
      return ApiResponse.error("401");
    }
    try {
      requests.deleteRequest(requestId);
      return ApiResponse.success(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId) {
    try {
      rooms.isUserInMembers(roomId, AuthTokenService.getId(token));
      return ApiResponse.success(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
