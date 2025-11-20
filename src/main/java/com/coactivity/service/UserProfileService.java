package com.coactivity.service;

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
import org.springframework.stereotype.Service;

// TODO: странные возвращаемые значение. Надо больше смысла. Касается всех методов.
//  Имею ввиду ApiResponse.success(null)
// TODO: implement all the methods
/**
 * Handles user profile operations including registration, profile management, and account
 * lifecycle.
 */
@Service
public class UserProfileService {

  private final UserRepositoryImpl userRepository;

  private final RoomRepositoryImpl roomRepository;
  private final TokenService tokenService;

  public UserProfileService(UserRepositoryImpl userRepository, RoomRepositoryImpl roomRepository,
      TokenService tokenService) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.tokenService = tokenService;
  }

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

  public ApiResponse<UserProfileResponse> getUserProfile(int token) {
    var response = new UserProfileResponse();
    try {
      User user = userRepository.getUserById(token);

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
    User user = userRepository.getUserById(token);
    try {
      userRepository.updateUser(user.getId(), request);
      return ApiResponse.success(null);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Integer> deleteAccount(int token) {
    try {
      userRepository.deleteUser(token);
      return ApiResponse.success(200);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Void> configureNotificationSettings(int token,
      NotificationSettingsRequest request) {
    try {
      if (request.getActivityClosed()) {
        userRepository.setNotification(token, "activityClosed");
      }
      if (request.getNewJoinRequest()) {
        userRepository.setNotification(token, "newJoinRequest");
      }
      if (request.getMembershipAccepted()) {
        userRepository.setNotification(token, "membershipAccepted");
      }
      if (request.getMembershipRejected()) {
        userRepository.setNotification(token, "membershipRejected");
      }
      return ApiResponse.success(null);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
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
