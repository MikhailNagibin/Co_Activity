package com.coactivity.service;

import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.util.AvatarUrlResolver;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

  private final UserRepository userRepository;

  public UserProfileService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserSummaryResponse getPublicUserProfileById(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User id is required");
    }
    return mapToUserSummary(getExistingUser(userId));
  }

  public UserProfileResponse getUserProfile(Integer userId) {
    User user = getExistingUser(userId);
    UserProfileResponse response = new UserProfileResponse();
    response.setId(user.getId());
    response.setCity(user.getCity());
    response.setAvatarId(user.getAvatarId());
    response.setDescription(user.getDescription());
    response.setCountry(user.getCountry());
    response.setEmail(user.getEmail());
    response.setUsername(user.getUserName());
    response.setDateOfBirth(user.getDataOfBirth());
    response.setAvatarUrl(
        AvatarUrlResolver.resolveUserAvatarUrl(user.getId(), user.getAvatarFileId()));
    response.setNotifications(user.getNotifications());
    return response;
  }

  public void updateUserProfile(Integer userId, UserProfileUpdateRequest request) {
    if (request == null) {
      throw new ValidationException("Update request is required");
    }
    User user = getExistingUser(userId);
    userRepository.updateUser(user.getId(), request);
  }

  public NotificationSettingsResponse getNotificationSettings(Integer userId) {
    User user = getExistingUser(userId);
    return toNotificationSettingsResponse(user);
  }

  public NotificationSettingsResponse configureNotificationSettings(Integer userId,
      NotificationSettingsRequest request) {
    if (request == null) {
      throw new ValidationException("Notification settings are required");
    }
    getExistingUser(userId);
    applyNotificationPreference(userId, request.getMembershipAccepted(),
        Notification.MEMBERSHIP_ACCEPTED);
    applyNotificationPreference(userId, request.getMembershipRejected(),
        Notification.MEMBERSHIP_REJECTED);
    applyNotificationPreference(userId, request.getActivityClosed(),
        Notification.ACTIVITY_CLOSED);
    applyNotificationPreference(userId, request.getNewJoinRequest(),
        Notification.NEW_JOIN_REQUEST);
    applyNotificationPreference(userId, request.getImportantRoomUpdates(),
        Notification.IMPORTANT_ROOM_UPDATES);

    User updatedUser = getExistingUser(userId);
    return toNotificationSettingsResponse(updatedUser);
  }

  private void applyNotificationPreference(Integer userId, Boolean enabled,
      Notification notification) {
    if (enabled == null) {
      return;
    }
    if (enabled) {
      userRepository.setNotification(userId, notification);
      return;
    }
    userRepository.removeNotification(userId, notification);
  }

  private NotificationSettingsResponse toNotificationSettingsResponse(User user) {
    List<Notification> enabledNotifications = user.getNotifications() != null
        ? user.getNotifications()
        : List.of();

    return new NotificationSettingsResponse(
        enabledNotifications.contains(Notification.MEMBERSHIP_ACCEPTED),
        enabledNotifications.contains(Notification.MEMBERSHIP_REJECTED),
        enabledNotifications.contains(Notification.ACTIVITY_CLOSED),
        enabledNotifications.contains(Notification.NEW_JOIN_REQUEST),
        enabledNotifications.contains(Notification.IMPORTANT_ROOM_UPDATES),
        Instant.now());
  }

  private UserSummaryResponse mapToUserSummary(User user) {
    UserSummaryResponse response = new UserSummaryResponse();
    response.setId(user.getId());
    response.setUserName(user.getUserName());
    response.setDateOfBirth(user.getDataOfBirth());
    response.setCity(user.getCity());
    response.setCountry(user.getCountry());
    response.setDescription(user.getDescription());
    response.setAvatarId(user.getAvatarId());
    response.setAvatarUrl(
        AvatarUrlResolver.resolveUserAvatarUrl(user.getId(), user.getAvatarFileId()));
    return response;
  }

  private User getExistingUser(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User id is required");
    }
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found");
    }
    return user;
  }
}
