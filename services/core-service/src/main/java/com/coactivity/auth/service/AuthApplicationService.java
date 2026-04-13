package com.coactivity.auth.service;

import com.coactivity.auth.model.UserStatus;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.PasswordChangeRequest;
import com.coactivity.controller.dto.request.PasswordResetConfirmRequest;
import com.coactivity.controller.dto.request.PasswordResetRequest;
import com.coactivity.controller.dto.request.PasswordResetVerifyRequest;
import com.coactivity.controller.dto.request.RegisterVerificationRequest;
import com.coactivity.controller.dto.request.ResendRegistrationVerificationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.NotificationService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TooManyRequestsException;
import com.coactivity.service.exception.ValidationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserJpaRepository userJpaRepository;
  private final PasswordEncoder passwordEncoder;
  private final NotificationService notificationService;
  private final RedisChallengeStore challengeStore;
  private final RedisPasswordResetStore passwordResetStore;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final UserProfileService userProfileService;
  private final SessionInvalidationService sessionInvalidationService;
  private final CurrentUserDetailsService currentUserDetailsService;

  public AuthApplicationService(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder,
      NotificationService notificationService, RedisChallengeStore challengeStore,
      RedisPasswordResetStore passwordResetStore,
      AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
      UserProfileService userProfileService, SessionInvalidationService sessionInvalidationService,
      CurrentUserDetailsService currentUserDetailsService) {
    this.userJpaRepository = userJpaRepository;
    this.passwordEncoder = passwordEncoder;
    this.notificationService = notificationService;
    this.challengeStore = challengeStore;
    this.passwordResetStore = passwordResetStore;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
    this.userProfileService = userProfileService;
    this.sessionInvalidationService = sessionInvalidationService;
    this.currentUserDetailsService = currentUserDetailsService;
  }

  @Transactional
  public RegistrationResponse register(UserRegistrationRequest request) {
    if (request == null) {
      throw new ValidationException("Registration request is required");
    }
    validateRegistrationRequest(request);

    String normalizedEmail = normalizeEmail(request.getEmail());
    String normalizedUserName = normalizeUserName(request.getUserName());
    ensureEmailAndUserNameAvailable(normalizedEmail, normalizedUserName);

    UserEntity user = new UserEntity();
    user.setEmail(request.getEmail().trim());
    user.setEmailNormalized(normalizedEmail);
    user.setUserName(normalizedUserName);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setStatus(UserStatus.PENDING_VERIFICATION);
    user.setEmailVerifiedAt(null);
    user.setDataOfBirth(request.getDateOfBirth());
    user.setCountry(blankToNull(request.getCountry()));
    user.setCity(blankToNull(request.getCity()));
    user.setDescription(blankToNull(request.getDescription()));
    user.setAvatarId(request.getAvatarId());

    try {
      UserEntity saved = userJpaRepository.saveAndFlush(user);
      String code = generateCode();
      challengeStore.create(normalizedEmail, saved.getId(), code);
      if (!notificationService.sendRegistrationVerificationCode(saved.getEmail(), code)) {
        challengeStore.delete(normalizedEmail);
        userJpaRepository.delete(saved);
        throw new NotificationDeliveryException("Unable to deliver verification code");
      }
      challengeStore.markResendCooldown(normalizedEmail);
      return new RegistrationResponse(saved.getId(), saved.getUserName(), saved.getEmail(),
          saved.getStatus().name());
    } catch (DataIntegrityViolationException ex) {
      ensureEmailAndUserNameAvailable(normalizedEmail, normalizedUserName);
      throw new ConflictException("USER_REGISTRATION_CONFLICT",
          "Email or username is already registered", ex);
    }
  }

  @Transactional
  public void verifyRegistration(RegisterVerificationRequest request) {
    if (request == null) {
      throw new ValidationException("Verification request is required");
    }

    String normalizedEmail = normalizeEmail(request.getEmail());
    UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

    RedisChallengeStore.VerificationResult result = challengeStore.verify(normalizedEmail,
        request.getCode().trim());

    switch (result) {
      case SUCCESS -> {
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
      }
      case INVALID_CODE -> throw new AuthorizationException("INVALID_VERIFICATION_CODE",
          "Invalid verification code");
      case TOO_MANY_ATTEMPTS -> throw new AuthorizationException(
          "TOO_MANY_VERIFICATION_ATTEMPTS", "Too many verification attempts");
      case EXPIRED_OR_MISSING -> throw new AuthorizationException("VERIFICATION_CODE_EXPIRED",
          "Verification code is expired or missing");
      default -> throw new AuthorizationException("Unable to verify registration");
    }
  }

  @Transactional
  public void resendRegistrationCode(ResendRegistrationVerificationRequest request) {
    if (request == null) {
      throw new ValidationException("Verification resend request is required");
    }

    String normalizedEmail = normalizeEmail(request.getEmail());
    UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

    if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
      throw new ConflictException("EMAIL_ALREADY_VERIFIED", "Email is already verified");
    }

    if (!challengeStore.tryActivateResendCooldown(normalizedEmail)) {
      throw new TooManyRequestsException("REGISTRATION_CODE_RESEND_COOLDOWN",
          "Please wait before requesting a new verification code");
    }

    String code = generateCode();
    boolean challengeCreated = false;
    try {
      challengeStore.create(normalizedEmail, user.getId(), code);
      challengeCreated = true;
      if (!notificationService.sendRegistrationVerificationCode(user.getEmail(), code)) {
        throw new NotificationDeliveryException("Unable to deliver verification code");
      }
    } catch (RuntimeException ex) {
      if (challengeCreated) {
        challengeStore.delete(normalizedEmail);
      }
      challengeStore.clearResendCooldown(normalizedEmail);
      throw ex;
    }
  }

  public UserProfileResponse login(LoginRequest request, HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    if (request == null) {
      throw new ValidationException("Login request is required");
    }

    String normalizedEmail = normalizeEmail(request.getEmail());
    try {
      Authentication authentication = authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail,
              request.getPassword()));
      if (httpRequest.getSession(false) != null) {
        httpRequest.getSession(false).invalidate();
      }
      httpRequest.getSession(true);
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      securityContextRepository.saveContext(context, httpRequest, httpResponse);

      CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
      return userProfileService.getUserProfile(principal.getUserId());
    } catch (DisabledException ex) {
      UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail).orElse(null);
      if (user != null && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
        throw new AuthorizationException("EMAIL_NOT_VERIFIED", "Email is not verified");
      }
      throw new AuthorizationException("User account is disabled");
    } catch (BadCredentialsException ex) {
      throw new AuthorizationException("Invalid email or password");
    }
  }

  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
    new SecurityContextLogoutHandler().logout(request, response, auth);
    clearCookie(response, "COACTIVITY_SESSION");
  }

  @Transactional
  public void changePassword(CurrentUserPrincipal currentUser, PasswordChangeRequest request) {
    if (request == null) {
      throw new ValidationException("Password change request is required");
    }
    validateNewPassword(request.getCurrentPassword(), request.getNewPassword());

    UserEntity user = userJpaRepository.findById(currentUser.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new AuthorizationException("Current password is incorrect");
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    sessionInvalidationService.invalidateAllSessions(currentUser.getUsername());
  }

  @Transactional
  public void requestPasswordReset(PasswordResetRequest request) {
    if (request == null) {
      throw new ValidationException("Password reset request is required");
    }

    String normalizedEmail = normalizeEmail(request.getEmail());
    if (!passwordResetStore.tryActivateRequestCooldown(normalizedEmail)) {
      return;
    }

    UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail).orElse(null);
    if (user == null || user.getStatus() != UserStatus.ACTIVE) {
      return;
    }

    String code = generateCode();
    boolean challengeCreated = false;
    try {
      passwordResetStore.create(normalizedEmail, user.getId(), code);
      challengeCreated = true;
      if (!notificationService.sendPasswordResetCode(user.getEmail(), code)) {
        throw new NotificationDeliveryException("Unable to deliver password reset code");
      }
    } catch (RuntimeException ex) {
      if (challengeCreated) {
        passwordResetStore.delete(normalizedEmail);
      }
      passwordResetStore.clearRequestCooldown(normalizedEmail);
    }
  }

  public void verifyPasswordReset(PasswordResetVerifyRequest request) {
    if (request == null) {
      throw new ValidationException("Password reset verification request is required");
    }

    RedisPasswordResetStore.VerificationResult result = passwordResetStore.inspect(
        normalizeEmail(request.getEmail()), request.getCode().trim());
    handlePasswordResetVerificationResult(result);
  }

  @Transactional
  public void confirmPasswordReset(PasswordResetConfirmRequest request) {
    if (request == null) {
      throw new ValidationException("Password reset confirmation request is required");
    }

    String normalizedEmail = normalizeEmail(request.getEmail());
    validateNewPassword(null, request.getNewPassword());

    RedisPasswordResetStore.VerificationResult result = passwordResetStore.consume(normalizedEmail,
        request.getCode().trim());
    handlePasswordResetVerificationResult(result);

    UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail)
        .orElseThrow(() -> new AuthorizationException("INVALID_PASSWORD_RESET_CODE",
            "Invalid password reset code"));
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    passwordResetStore.clearRequestCooldown(normalizedEmail);
    sessionInvalidationService.invalidateAllSessions(user.getEmailNormalized());
  }

  public UserProfileResponse me(CurrentUserPrincipal currentUser) {
    return userProfileService.getUserProfile(currentUser.getUserId());
  }

  public void invalidateAllSessions(CurrentUserPrincipal currentUser) {
    sessionInvalidationService.invalidateAllSessions(currentUser.getUsername());
  }

  public CurrentUserPrincipal reloadPrincipal(Integer userId) {
    return currentUserDetailsService.loadCurrentUser(userId);
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new ValidationException("Email is required");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeUserName(String userName) {
    if (userName == null || userName.isBlank()) {
      throw new ValidationException("Username is required");
    }
    return userName.trim();
  }

  private void validateRegistrationRequest(UserRegistrationRequest request) {
    if (request.getDateOfBirth() == null) {
      throw new ValidationException("Date of birth is required");
    }
    if (!request.getDateOfBirth().isBefore(Instant.now())) {
      throw new ValidationException("Date of birth must be in the past");
    }
    if (request.getPassword() == null || request.getPassword().isBlank()) {
      throw new ValidationException("Password is required");
    }
    if (request.getPassword().length() < 8 || request.getPassword().length() > 128) {
      throw new ValidationException("Password length must be between 8 and 128 characters");
    }
    if (request.getUserName() != null) {
      String trimmedUserName = request.getUserName().trim();
      if (trimmedUserName.length() < 2 || trimmedUserName.length() > 20) {
        throw new ValidationException("Username length must be between 2 and 20 characters");
      }
    }
    if (request.getEmail() != null && request.getEmail().trim().length() > 255) {
      throw new ValidationException("Email must be at most 255 characters");
    }
    if (request.getCity() != null && request.getCity().length() > 100) {
      throw new ValidationException("City must be at most 100 characters");
    }
    if (request.getCountry() != null && request.getCountry().length() > 100) {
      throw new ValidationException("Country must be at most 100 characters");
    }
    if (request.getDescription() != null && request.getDescription().length() > 500) {
      throw new ValidationException("Description must be at most 500 characters");
    }
  }

  private void validateNewPassword(String currentSecret, String newPassword) {
    if (newPassword == null || newPassword.isBlank()) {
      throw new ValidationException("Password is required");
    }
    if (newPassword.length() < 8 || newPassword.length() > 128) {
      throw new ValidationException("Password length must be between 8 and 128 characters");
    }
    if (currentSecret != null && currentSecret.equals(newPassword)) {
      throw new ValidationException("New password must be different from current password");
    }
  }

  private void handlePasswordResetVerificationResult(
      RedisPasswordResetStore.VerificationResult result) {
    switch (result) {
      case SUCCESS -> {
      }
      case INVALID_CODE -> throw new AuthorizationException("INVALID_PASSWORD_RESET_CODE",
          "Invalid password reset code");
      case TOO_MANY_ATTEMPTS -> throw new AuthorizationException(
          "TOO_MANY_PASSWORD_RESET_ATTEMPTS", "Too many password reset attempts");
      case EXPIRED_OR_MISSING -> throw new AuthorizationException("PASSWORD_RESET_CODE_EXPIRED",
          "Password reset code is expired or missing");
      default -> throw new AuthorizationException("Unable to verify password reset code");
    }
  }

  private void ensureEmailAndUserNameAvailable(String normalizedEmail, String normalizedUserName) {
    if (userJpaRepository.existsByEmailNormalized(normalizedEmail)) {
      throw new ConflictException("EMAIL_ALREADY_REGISTERED", "Email is already registered");
    }
    if (userJpaRepository.existsByUserNameIgnoreCase(normalizedUserName)) {
      throw new ConflictException("USERNAME_ALREADY_TAKEN", "Username is already taken");
    }
  }

  private String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String generateCode() {
    return String.format("%06d", RANDOM.nextInt(1_000_000));
  }

  private void clearCookie(HttpServletResponse response, String cookieName) {
    Cookie cookie = new Cookie(cookieName, "");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
