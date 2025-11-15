package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.domain.Notification;
import com.coactivity.service.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class UserControllerImpl {
  private final UserService userService = new UserService();

  public UserControllerImpl() {}

  public ApiResponse<UserProfileResponse> getUserProfile(int token) {
    return userService.getUserProfile(token);
  }

  public ApiResponse updateUserProfile(int token, String username, Instant dateOfBirth,
                                                            String city, String country, String description,
                                                            Integer avatarId) {

    if (username != null && (username.length() > 50 || username.length() < 3)) {
      return ApiResponse.error(null);
    }
    if (city != null && (city.length() > 50 || city.length() < 3)) {
      return ApiResponse.error(null);
    }
    if (country != null && (country.length() > 50 || country.length() < 3)) {
      return ApiResponse.error(null);
    }
    if (description != null && (description.length() > 5000 || description.length() < 3)) {
      return ApiResponse.error(null);
    }
    if (dateOfBirth != null && !isWithinLast100Years(dateOfBirth)) {
      return ApiResponse.error(null);
    }
    try {
      var request = new UserProfileUpdateRequest();
      request.setUsername(username);
      request.setDateOfBirth(dateOfBirth);
      request.setCity(city);
      request.setCountry(country);
      request.setDescription(description);
      request.setAvatarId(avatarId);

      userService.updateUserProfile(token, request);

      return ApiResponse.success(200);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }


  public ApiResponse<Integer> deleteAccount(int token) {
    try {
      return userService.deleteAccount(token);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }

  public ApiResponse<Void> configureNotificationSettings(int token,
                                                                                Map<Notification, Boolean> notifications) {
    try {
      var request = new NotificationSettingsRequest();
      request.setActivityClosed(notifications.get("ACTIVITY_CLOSED"));
      request.setNewJoinRequest(notifications.get("NEW_JOIN_REQUEST"));
      request.setMembershipAccepted(notifications.get("MEMBERSHIP_ACCEPTED"));
      request.setMembershipRejected(notifications.get("MEMBERSHIP_REJECTED"));
      return userService.configureNotificationSettings(token, request);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
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
