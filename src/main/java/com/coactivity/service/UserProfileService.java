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
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.dto.PendingVerification;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(10);

  private final UserRepositoryImpl userRepository;
  private final TokenService tokenService;
  private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
  private final SecureRandom secureRandom = new SecureRandom();

  public UserProfileService(UserRepositoryImpl userRepository, TokenService tokenService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
  }

  public ApiResponse<RegistrationResponse> registerUser(UserRegistrationRequest request) {
    if (request == null
        || isBlank(request.getLogin())
        || isBlank(request.getUserName())
        || isBlank(request.getPassword())
        || request.getDateOfBirth() == null) {
      return ApiResponse.error("Invalid registration data");
    }

    try {
      User createdUser = userRepository.createUser(request);
      RegistrationResponse response =
          new RegistrationResponse(createdUser.getId(), createdUser.getUserName());
      return ApiResponse.success("User registered successfully", response);
    } catch (Exception e) {
      return ApiResponse.error("Unable to register user");
    }
  }

  public ApiResponse<Void> loginUser(LoginRequest request) {
    if (request == null || isBlank(request.getLogin()) || isBlank(request.getPassword())) {
      return ApiResponse.error("Invalid credentials");
    }

    try {
      User user = userRepository.getUser(request.getLogin(), request.getPassword());
      if (user == null) {
        return ApiResponse.error("Invalid login or password");
      }

      String verificationCode = generateVerificationCode();
      pendingVerifications.put(normalizeLogin(request.getLogin()),
          new PendingVerification(user.getId(), verificationCode,
              Instant.now().plus(VERIFICATION_CODE_TTL)));

      return ApiResponse.success("Verification code sent to email", null);
    } catch (Exception e) {
      return ApiResponse.error("Unable to initiate login");
    }
  }

  public ApiResponse<LoginResponse> verifyLogin(String login, String verificationCode) {
    if (isBlank(login) || isBlank(verificationCode)) {
      return ApiResponse.error("Invalid verification request");
    }

    String normalizedLogin = normalizeLogin(login);
    PendingVerification pending = pendingVerifications.get(normalizedLogin);

    if (pending == null) {
      return ApiResponse.error("No pending verification found");
    }

    if (pending.expiresAt().isBefore(Instant.now())) {
      pendingVerifications.remove(normalizedLogin);
      return ApiResponse.error("Verification code expired");
    }

    if (!pending.code().equals(verificationCode)) {
      return ApiResponse.error("Invalid verification code");
    }

    try {
      User user = userRepository.getUserById(pending.userId());
      if (user == null) {
        pendingVerifications.remove(normalizedLogin);
        return ApiResponse.error("User not found");
      }

      String token = tokenService.createToken(user.getId());
      tokenService.registerToken(user.getId(), token);
      pendingVerifications.remove(normalizedLogin);

      LoginResponse response = new LoginResponse(token, user.getId(), user.getUserName());
      return ApiResponse.success("Login verified successfully", response);
    } catch (Exception e) {
      return ApiResponse.error("Unable to verify login");
    }
  }

  public ApiResponse<Void> logoutUser(String token) {
    if (isBlank(token) || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("Invalid or expired token");
    }

    tokenService.invalidateToken(token);
    return ApiResponse.success("Logged out successfully", null);
  }

  public ApiResponse<LoginResponse> updatePassword(String token, String currentPassword,
      String newPassword) {
    if (isBlank(token) || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    if (isBlank(currentPassword) || isBlank(newPassword)) {
      return ApiResponse.error("Password values must not be empty");
    }

    if (currentPassword.equals(newPassword)) {
      return ApiResponse.error("New password must be different from current password");
    }

    try {
      Integer userId = tokenService.decodeToken(token).userId();
      User user = userRepository.getUserById(userId);
      if (user == null) {
        return ApiResponse.error("User not found");
      }

      User verifiedUser = userRepository.getUser(user.getLogin(), currentPassword);
      if (verifiedUser == null) {
        return ApiResponse.error("Current password is incorrect");
      }

      userRepository.updatePassword(userId, newPassword);

      tokenService.invalidateToken(token);
      String newToken = tokenService.createToken(userId);
      tokenService.registerToken(userId, newToken);

      LoginResponse response = new LoginResponse(newToken, userId, user.getUserName());
      return ApiResponse.success("Password updated successfully", response);
    } catch (Exception e) {
      return ApiResponse.error("Unable to update password");
    }
  }

  public ApiResponse<UserSummaryResponse> getPublicUserProfileById(String token, Integer userId) {
    if (isBlank(token) || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    if (userId == null) {
      return ApiResponse.error("400");
    }

    try {
      User user = userRepository.getUserById(userId);
      if (user == null) {
        return ApiResponse.error("404");
      }

      UserSummaryResponse response = mapToUserSummary(user);
      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.error("500");
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

  private String generateVerificationCode() {
    int code = secureRandom.nextInt(90000) + 10000;
    return Integer.toString(code);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String normalizeLogin(String login) {
    return login.toLowerCase(Locale.ROOT);
  }
}
