package com.coactivity.service;

import com.coactivity.AuthToken;
import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Role;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;

public class UserProfileService {
  private UserRepositoryImpl users;
  private DataRepository repository;
  private RoomRepositoryImpl rooms;

  public UserProfileService() {
    repository = new DataRepository();
    users = new UserRepositoryImpl(repository);
    rooms = new RoomRepositoryImpl(repository);
  }

//  TODO: implement following methods:
  public ApiResponse<RegistrationResponse> registerUser(UserRegistrationRequest request) {
    return ApiResponse.success(null);
  }

  public ApiResponse<Void> loginUser(LoginRequest request) {
    return ApiResponse.success(null);
  }

  public ApiResponse<LoginResponse> verifyLogin(String login, String verificationCode) {
    return ApiResponse.success(null);
  }

  public ApiResponse<Void> logoutUser(String token) {
    return ApiResponse.success(null);
  }

  public ApiResponse<UserSummaryResponse> getPublicUserProfileById(String token, Integer userId) {
    return ApiResponse.success(null);
  }

  public ApiResponse<LoginResponse> updatePassword(String token, String currentPassword,
      String newPassword) {
    return ApiResponse.success(null);
  }


// TODO: UserWithRoomService that contains following methods:
//  assignAdminRole, demoteAdminRole, getBanRooms, getUserRooms, joinRoom, leaveRoom,
//  getRoomParticipants, isUserInRoom
// TODO: JoinRequestsService that contains following methods:
//  getPendingRequests, getPendingRequestsForRoom, processJoinRequest,
//  getSentRequests, cancelRequest
// TODO:
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

  public ApiResponse<Void> configureNotificationSettings(int token,
                                                         NotificationSettingsRequest request) {
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

    int roomOwnerId = AuthToken.getId(token);
    try {
      if (!rooms.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      rooms.setRoleByUserIdAndRoomId(userId, roomId, Role.ADMIN);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {

    int roomOwnerId = AuthToken.getId(token);
    try {
      if (!rooms.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      rooms.setRoleByUserIdAndRoomId(userId, roomId, Role.PARTICIPANT);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }
}
