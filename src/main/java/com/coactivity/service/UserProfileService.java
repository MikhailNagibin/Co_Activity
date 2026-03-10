package com.coactivity.service;

import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.domain.UserNotification;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.dto.PendingVerification;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Handles user profile operations including registration, profile management, and account
 * lifecycle.
 */
@Service
public class UserProfileService {

  private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(10);

  private final UserRepositoryImpl userRepository;
  private final TokenService tokenService;
  private final NotificationService notificationService;
  private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
  private final SecureRandom secureRandom = new SecureRandom();

  public UserProfileService(UserRepositoryImpl userRepository, TokenService tokenService,
      NotificationService notificationService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
    this.notificationService = notificationService;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public RegistrationResponse registerUser(UserRegistrationRequest request) {
    if (request == null
        || isBlank(request.getLogin())
        || isBlank(request.getUserName())
        || isBlank(request.getPassword())
        || request.getDateOfBirth() == null) {
      throw new ValidationException("Invalid registration data");
    }

    try {
      User createdUser = userRepository.createUser(request);
      return new RegistrationResponse(createdUser.getId(), createdUser.getUserName());
    } catch (Exception e) {
      throw new ValidationException("Unable to register user", e);
    }
  }

  public void loginUser(LoginRequest request) {
    if (request == null || isBlank(request.getLogin()) || isBlank(request.getPassword())) {
      throw new ValidationException("Invalid credentials");
    }

    try {
      User user = userRepository.getUser(request.getLogin(), request.getPassword());
      if (user == null) {
        throw new AuthorizationException("Invalid login or password");
      }

      String verificationCode = generateVerificationCode();
      pendingVerifications.put(normalizeLogin(request.getLogin()),
          new PendingVerification(user.getId(), verificationCode,
              Instant.now().plus(VERIFICATION_CODE_TTL)));

      notificationService.sendLoginVerificationCode(request.getLogin(), verificationCode);
    } catch (AuthorizationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to initiate login", e);
    }
  }

  public LoginResponse verifyLogin(String login, String verificationCode) {
    if (isBlank(login) || isBlank(verificationCode)) {
      throw new ValidationException("Invalid verification request");
    }

    String normalizedLogin = normalizeLogin(login);
    PendingVerification pending = pendingVerifications.get(normalizedLogin);

    if (pending == null) {
      throw new ResourceNotFoundException("No pending verification found");
    }

    if (pending.expiresAt().isBefore(Instant.now())) {
      pendingVerifications.remove(normalizedLogin);
      throw new ValidationException("Verification code expired");
    }

    if (!pending.code().equals(verificationCode)) {
      throw new AuthorizationException("Invalid verification code");
    }

    try {
      User user = userRepository.getUserById(pending.userId());
      if (user == null) {
        pendingVerifications.remove(normalizedLogin);
        throw new ResourceNotFoundException("User not found");
      }

      String token = tokenService.createToken(user.getId());
      tokenService.registerToken(user.getId(), token);
      pendingVerifications.remove(normalizedLogin);

      return new LoginResponse(token, user.getId(), user.getUserName());
    } catch (Exception e) {
      throw new ValidationException("Unable to verify login", e);
    }
  }

  public void logoutUser(String token) {
    if (isBlank(token) || !tokenService.isTokenActive(token)) {
      throw new TokenValidationException("Invalid or expired token");
    }

    tokenService.invalidateToken(token);
  }

  public LoginResponse updatePassword(String token, String currentPassword,
      String newPassword) {
    Integer userId = requireAuthenticatedUser(token);
    if (isBlank(currentPassword) || isBlank(newPassword)) {
      throw new ValidationException("Password values must not be empty");
    }
    if (currentPassword.equals(newPassword)) {
                  throw new ValidationException("New password must be different from current password");
    }

    try {
      User user = getExistingUser(userId);

      User verifiedUser = userRepository.getUser(user.getLogin(), currentPassword);
      if (verifiedUser == null) {
        throw new AuthorizationException("Current password is incorrect");
      }

      userRepository.updatePassword(userId, newPassword);

      tokenService.invalidateToken(token);
      String newToken = tokenService.createToken(userId);
      tokenService.registerToken(userId, newToken);

      return new LoginResponse(newToken, userId, user.getUserName());
    } catch (AuthorizationException | ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to update password", e);
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

      boolean membershipAccepted = false;
      boolean membershipRejected = false;
      boolean activityClosed = false;
      boolean newJoinRequest = false;

      for (UserNotification notification : user.getNotifications()) {
        switch (notification.getNotification().getDescription()) {
          case ("membershipAccepted") -> membershipAccepted = true;
          case ("membershipRejected") -> membershipRejected = true;
          case ("activityClosed") -> activityClosed = true;
          case ("newJoinRequest") -> newJoinRequest = true;
        }
      }
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

  private String generateVerificationCode() {
    int code = secureRandom.nextInt(90000) + 10000;
    return Integer.toString(code);
  }

  private String normalizeLogin(String login) {
    return login.toLowerCase(Locale.ROOT);
  }
}
