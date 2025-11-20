package com.coactivity.service;

import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Notification;
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

  public ApiResponse<LoginResponse> updatePassword(String token, String currentPassword,
      String newPassword) {
    return ApiResponse.success(null);
  }

  public ApiResponse<UserSummaryResponse> getPublicUserProfileById(String token, Integer userId) {
    return ApiResponse.success(null);
  }

  public ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId) {
    try {
      roomRepository.isUserInMembers(roomId, tokenService.decodeToken(token).userId());
      return ApiResponse.success(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ApiResponse<UserProfileResponse> getUserProfile(String token) {
    var response = new UserProfileResponse();
    try {
      User user = userRepository.getUserById(tokenService.decodeToken(token).userId());

      response.setId(user.getId());
      response.setCity(user.getCity());
      response.setAvatarId(user.getAvatarId());
      response.setDescription(user.getDescription());
      response.setCountry(user.getCountry());
      response.setLogin(user.getLogin());
      response.setUsername(user.getUserName());
      response.setDateOfBirth(user.getDataOfBirth());

      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.success(null);
    }
  }

  public ApiResponse<Void> updateUserProfile(String token, UserProfileUpdateRequest request) {
    User user = userRepository.getUserById(tokenService.decodeToken(token).userId());
    try {
      userRepository.updateUser(user.getId(), request);
      return ApiResponse.success(null);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Integer> deleteAccount(String token) {
    try {
      userRepository.deleteUser(tokenService.decodeToken(token).userId());
      return ApiResponse.success(200);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Void> configureNotificationSettings(String token,
      NotificationSettingsRequest request) {
    try {
      Integer userId = tokenService.decodeToken(token).userId();

      if (request.getActivityClosed()) {
        userRepository.setNotification(userId, Notification.ACTIVITY_CLOSED);
      }
      if (request.getNewJoinRequest()) {
        userRepository.setNotification(userId, Notification.NEW_JOIN_REQUEST);
      }
      if (request.getMembershipAccepted()) {
        userRepository.setNotification(userId, Notification.MEMBERSHIP_ACCEPTED);
      }
      if (request.getMembershipRejected()) {
        userRepository.setNotification(userId, Notification.MEMBERSHIP_REJECTED);
      }
      return ApiResponse.success(null);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

}
