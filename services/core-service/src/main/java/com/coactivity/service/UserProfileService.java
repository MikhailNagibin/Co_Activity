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

  public void deleteAccount(Integer userId) {
    userRepository.deleteUser(userId);
  }

  public NotificationSettingsResponse configureNotificationSettings(Integer userId,
      NotificationSettingsRequest request) {
    if (request == null) {
      throw new ValidationException("Notification settings are required");
    }
    if (request.getMembershipAccepted() != null) {
      if (request.getMembershipAccepted()) {
        userRepository.setNotification(userId, Notification.MEMBERSHIP_ACCEPTED);
      } else {
        userRepository.removeNotification(userId, Notification.MEMBERSHIP_ACCEPTED);
      }
    }

    if (request.getMembershipRejected() != null) {
      if (request.getMembershipRejected()) {
        userRepository.setNotification(userId, Notification.MEMBERSHIP_REJECTED);
      } else {
        userRepository.removeNotification(userId, Notification.MEMBERSHIP_REJECTED);
      }
    }

    if (request.getActivityClosed() != null) {
      if (request.getActivityClosed()) {
        userRepository.setNotification(userId, Notification.ACTIVITY_CLOSED);
      } else {
        userRepository.removeNotification(userId, Notification.ACTIVITY_CLOSED);
      }
    }

    if (request.getNewJoinRequest() != null) {
      if (request.getNewJoinRequest()) {
        userRepository.setNotification(userId, Notification.NEW_JOIN_REQUEST);
      } else {
        userRepository.removeNotification(userId, Notification.NEW_JOIN_REQUEST);
      }
    }

    User user = userRepository.getUserById(userId);
    List<Notification> enabledNotifications = user != null ? user.getNotifications() : List.of();

    boolean membershipAccepted = enabledNotifications.contains(Notification.MEMBERSHIP_ACCEPTED);
    boolean membershipRejected = enabledNotifications.contains(Notification.MEMBERSHIP_REJECTED);
    boolean activityClosed = enabledNotifications.contains(Notification.ACTIVITY_CLOSED);
    boolean newJoinRequest = enabledNotifications.contains(Notification.NEW_JOIN_REQUEST);

    return new NotificationSettingsResponse(
        membershipAccepted,
        membershipRejected,
        activityClosed,
        newJoinRequest,
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
    return response;
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found");
    }
    return user;
  }
}
