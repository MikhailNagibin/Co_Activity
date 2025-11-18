package com.coactivity.controller.impl;

import com.coactivity.AuthToken;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.service.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class UserControllerImpl {
  private final UserService userService = new UserService();

  public UserControllerImpl() {}

  public ApiResponse<UserProfileResponse> getUserProfile(String token) {
    return userService.getUserProfile(AuthToken.getId(token));
  }

  public ApiResponse<String> updateUserProfile(String token, UserProfileUpdateRequest request) {

    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }
    if (request.getUsername() != null && (request.getUsername().length() > 50 || request.getUsername().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getCity() != null && (request.getCity().length() > 50 || request.getCity().length() < 3)) {
      return ApiResponse.error("400");
    }
    if (request.getCountry() != null && (request.getCountry().length() > 50 || request.getCountry().length() < 3)) {
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
      userService.updateUserProfile(AuthToken.getId(token), request);
      return ApiResponse.success("200");
    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Integer> deleteAccount(String token) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }
    try {
      return userService.deleteAccount(AuthToken.getId(token));
    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> configureNotificationSettings(String token,
                                                         NotificationSettingsRequest request) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }
    try {
      return userService.configureNotificationSettings(AuthToken.getId(token), request);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Void> assignAdminRole(String token, Integer roomId,
                                                             Integer userId) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }

    try {
        return userService.assignAdminRole(token, roomId, userId);
    } catch (Exception e) {
      return ApiResponse.error("401");
    }
  }

  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }

    try {
      return userService.demoteAdminRole(token, roomId, userId);
    } catch (Exception e) {
      return ApiResponse.error("401");
    }
  }

  public ApiResponse<Void> cancelRequest(String token, Integer requestId) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }
    try {
      return userService.cancelRequest(token, requestId);
    } catch (Exception e) {
      return ApiResponse.error("401");
    }
  }

  public ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId) {
    if (!AuthToken.isTokenExpired(token)) {
      return ApiResponse.error("401");
    }

    return userService.isUserInRoom(token, roomId);
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
}
