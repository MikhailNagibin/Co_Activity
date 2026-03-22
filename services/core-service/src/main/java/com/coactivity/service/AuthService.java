package com.coactivity.service;

import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.dto.PendingVerification;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(10);

  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final NotificationService notificationService;
  private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthService(UserRepository userRepository, TokenService tokenService,
      NotificationService notificationService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
    this.notificationService = notificationService;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
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

      String normalizedLogin = normalizeLogin(request.getLogin());
      String verificationCode = generateVerificationCode();
      pendingVerifications.put(normalizedLogin,
          new PendingVerification(user.getId(), verificationCode,
              Instant.now().plus(VERIFICATION_CODE_TTL)));

      boolean delivered = notificationService.sendLoginVerificationCode(request.getLogin(),
          verificationCode);
      if (!delivered) {
        pendingVerifications.remove(normalizedLogin);
        throw new NotificationDeliveryException("Unable to deliver verification code");
      }
    } catch (AuthorizationException | NotificationDeliveryException e) {
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
      pendingVerifications.remove(normalizedLogin);

      return new LoginResponse(token, user.getId(), user.getUserName());
    } catch (ResourceNotFoundException e) {
      throw e;
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

  public LoginResponse updatePassword(String token, String currentPassword, String newPassword) {
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

      return new LoginResponse(newToken, userId, user.getUserName());
    } catch (AuthorizationException | ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to update password", e);
    }
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
