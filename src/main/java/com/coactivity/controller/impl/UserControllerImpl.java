package com.coactivity.controller.impl;

import com.coactivity.controller.UserController;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.UserWithRoomService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserControllerImpl implements UserController {

  private final UserProfileService userService;
  private final TokenService tokenService;
  private final UserWithRoomService userWithRoomService;
  private final JoinRequestService joinRequestService;

  public UserControllerImpl(UserProfileService userService, TokenService tokenService,
      UserWithRoomService userWithRoomService, JoinRequestService joinRequestService) {
    this.userService = userService;
    this.tokenService = tokenService;
    this.userWithRoomService = userWithRoomService;
    this.joinRequestService = joinRequestService;
  }

  private static boolean isWithinLast100Years(Instant instantToCheck) {
    if (instantToCheck == null) {
      return false;
    }

    Instant now = Instant.now();
    Instant hundredYearsAgo = LocalDate.now()
        .minusYears(100)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant();

    return !instantToCheck.isBefore(hundredYearsAgo) && !instantToCheck.isAfter(now);
  }

  @Override
  public ApiResponse<RegistrationResponse> registerUser(UserRegistrationRequest request) {
    return userService.registerUser(request);
  }

  @Override
  public ApiResponse<Void> loginUser(LoginRequest request) {
    return userService.loginUser(request);
  }

  @Override
  public ApiResponse<LoginResponse> verifyLogin(String login, String verificationCode) {
    return userService.verifyLogin(login, verificationCode);
  }

  @Override
  public ApiResponse<Void> logoutUser(String token) {
    return userService.logoutUser(token);
  }

  @Override
  public ApiResponse<UserProfileResponse> getUserProfile(String token) {
    if (!tokenService.isTokenActive(token)) {

      return ApiResponse.error("Invalid or expired token");
    }
    return userService.getUserProfile(token);
  }

  @Override
  public ApiResponse<UserSummaryResponse> getPublicUserProfileById(String token, Integer userId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return userService.getPublicUserProfileById(token, userId);
  }

  @Override
  public ApiResponse<String> updateUserProfile(String token, UserProfileUpdateRequest request) {

    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (request.getUsername() != null && (request.getUsername().length() > 50
      || request.getUsername().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getCity() != null && (request.getCity().length() > 50
      || request.getCity().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getCountry() != null && (request.getCountry().length() > 50
      || request.getCountry().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getDescription() != null && (request.getDescription().length() > 5000 ||
      request.getDescription().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getDateOfBirth() != null && !isWithinLast100Years(request.getDateOfBirth())) {
      return ApiResponse.error("400");
    }

    try {
      userService.updateUserProfile(token, request);
      return ApiResponse.success("200");
    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  @Override
  public ApiResponse<LoginResponse> updatePassword(String token, String currentPassword,
      String newPassword) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return userService.updatePassword(token, currentPassword, newPassword);
  }

  @Override
  public ApiResponse<Void> configureNotificationSettings(String token,
                                                         NotificationSettingsRequest request) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    try {
      return userService.configureNotificationSettings(token, request);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  @Override
  public ApiResponse<Integer> deleteAccount(String token) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    try {
      return userService.deleteAccount(token);
    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  @Override
  public ApiResponse<Void> assignAdminRole(String token, Integer roomId,
                                           Integer userId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    try {
      return userWithRoomService.assignAdminRole(token, roomId, userId);
    } catch (Exception e) {
      return ApiResponse.error("401");
    }
  }

  @Override
  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    try {
      return userWithRoomService.demoteAdminRole(token, roomId, userId);
    } catch (Exception e) {
      return ApiResponse.error("401");
    }
  }

  @Override
  public ApiResponse<List<JoinRequestResponse>> getPendingRequests(String token) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return joinRequestService.getPendingRequests(tokenService.decodeToken(token).userId());
  }

  @Override
  public ApiResponse<List<JoinRequestResponse>> getPendingRequestsForRoom(String token,
      Integer roomId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return joinRequestService.getPendingRequestsForRoom(tokenService.decodeToken(token).userId(), roomId);
  }

  @Override
  public ApiResponse<Void> processJoinRequest(String token, Integer requestId,
      RequestStatus action) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return joinRequestService.processJoinRequest(tokenService.decodeToken(token).userId(), requestId, action);
  }

  @Override
  public ApiResponse<List<JoinRequestResponse>> getSentRequests(String token) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return joinRequestService.getSentRequests(tokenService.decodeToken(token).userId());
  }

  @Override
  public ApiResponse<Void> cancelRequest(String token, Integer requestId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return joinRequestService.cancelRequest(tokenService.decodeToken(token).userId(), requestId);
  }

  @Override
  public ApiResponse<List<RoomSummaryResponse>> getBanRooms(String token) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }
    return userWithRoomService.getBanRooms(token);
  }

//  @Override
  public ApiResponse<Boolean> isUserInRoom(String token, Integer roomId) {
    if (!tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }

    return userWithRoomService.isUserInRoom(token, roomId);
  }

  private boolean validateProfileUpdateRequest(UserProfileUpdateRequest request) {
    if (request.getUsername() != null && (request.getUsername().length() > 50
        || request.getUsername().length() < 3)) {
      return false;
    }
    if (request.getCity() != null && (request.getCity().length() > 50
        || request.getCity().length() < 3)) {
      return false;
    }
    if (request.getCountry() != null && (request.getCountry().length() > 50
        || request.getCountry().length() < 3)) {
      return false;
    }
    if (request.getDescription() != null && (request.getDescription().length() > 5000
        || request.getDescription().length() < 3)) {
      return false;
    }
    if (request.getDateOfBirth() != null && !isWithinLast100Years(request.getDateOfBirth())) {
      return false;
    }
    return true;
  }
}
