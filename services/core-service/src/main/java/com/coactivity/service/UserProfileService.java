package com.coactivity.service;

import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

  private final UserRepository userRepository;
  private final TokenService tokenService;

  public UserProfileService(UserRepository userRepository, TokenService tokenService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public RegistrationResponse registerUser(UserRegistrationRequest request) {
    try {
      User createdUser = userRepository.createUser(request);
      return new RegistrationResponse(createdUser.getId(), createdUser.getUserName());
    } catch (Exception e) {
      String message = e.getMessage() != null && !e.getMessage().isBlank()
          ? e.getMessage()
          : "Unable to register user";
      throw new ValidationException(message, e);
    }
  }

  public UserSummaryResponse getPublicUserProfileById(String token, Integer userId) {
    requireAuthenticatedUser(token);
    if (userId == null) {
      throw new ValidationException("User id is required");
    }

    try {
      User user = userRepository.getUserById(userId);
      if (user == null) {
        throw new ResourceNotFoundException("User not found");
      }
      return mapToUserSummary(user);
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to load profile", e);
    }
  }

  public UserProfileResponse getUserProfile(String token) {
    Integer userId = requireAuthenticatedUser(token);
    try {
      User user = getExistingUser(userId);
      UserProfileResponse response = new UserProfileResponse();
      response.setId(user.getId());
      response.setCity(user.getCity());
      response.setAvatarId(user.getAvatarId());
      response.setDescription(user.getDescription());
      response.setCountry(user.getCountry());
      response.setLogin(user.getLogin());
      response.setUsername(user.getUserName());
      response.setDateOfBirth(user.getDataOfBirth());
      return response;
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to load profile", e);
    }
  }

  public void updateUserProfile(String token, UserProfileUpdateRequest request) {
    Integer userId = requireAuthenticatedUser(token);
    if (request == null) {
      throw new ValidationException("Update request is required");
    }

    try {
      User user = getExistingUser(userId);
      userRepository.updateUser(user.getId(), request);
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to update profile", e);
    }
  }

  public void deleteAccount(String token) {
    Integer userId = requireAuthenticatedUser(token);
    try {
      userRepository.deleteUser(userId);
    } catch (Exception e) {
      throw new ValidationException("Unable to delete account", e);
    }
  }

  public NotificationSettingsResponse configureNotificationSettings(String token,
      NotificationSettingsRequest request) {
    Integer userId = requireAuthenticatedUser(token);
    if (request == null) {
      throw new ValidationException("Notification settings are required");
    }

    try {
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
    } catch (Exception e) {
      throw new ValidationException("Unable to update notification settings", e);
    }
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

  private Integer requireAuthenticatedUser(String token) {
    if (isBlank(token) || !tokenService.isTokenActive(token)) {
      throw new TokenValidationException("Invalid token");
    }
    return tokenService.decodeToken(token).userId();
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found");
    }
    return user;
  }
}
