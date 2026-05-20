package com.coactivity.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.auth.domain.UserStatus;
import com.coactivity.auth.port.PasswordResetStore;
import com.coactivity.auth.port.RegistrationChallengeStore;
import com.coactivity.auth.port.VerificationResult;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.RegisterVerificationRequest;
import com.coactivity.controller.dto.request.ResendRegistrationVerificationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.request.PasswordResetConfirmRequest;
import com.coactivity.controller.dto.request.PasswordResetRequest;
import com.coactivity.controller.dto.request.PasswordResetVerifyRequest;
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
import com.coactivity.service.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

@DisplayName("AuthApplicationService tests")
class AuthApplicationServiceTest {

  private UserJpaRepository userJpaRepository;
  private AuthenticationManager authenticationManager;
  private SecurityContextRepository securityContextRepository;
  private UserProfileService userProfileService;
  private PasswordEncoder passwordEncoder;
  private NotificationService notificationService;
  private RegistrationChallengeStore challengeStore;
  private PasswordResetStore passwordResetStore;
  private SessionInvalidationService sessionInvalidationService;
  private AuthApplicationService authApplicationService;

  @BeforeEach
  void setUp() {
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    authenticationManager = Mockito.mock(AuthenticationManager.class);
    securityContextRepository = Mockito.mock(SecurityContextRepository.class);
    userProfileService = Mockito.mock(UserProfileService.class);
    passwordEncoder = Mockito.mock(PasswordEncoder.class);
    notificationService = Mockito.mock(NotificationService.class);
    challengeStore = Mockito.mock(RegistrationChallengeStore.class);
    passwordResetStore = Mockito.mock(PasswordResetStore.class);
    sessionInvalidationService = Mockito.mock(SessionInvalidationService.class);

    authApplicationService = new AuthApplicationService(
        userJpaRepository,
        passwordEncoder,
        notificationService,
        challengeStore,
        passwordResetStore,
        authenticationManager,
        securityContextRepository,
        userProfileService,
        sessionInvalidationService,
        Mockito.mock(CurrentUserDetailsService.class));
  }

  @Test
  void loginCreatesSessionWhenNoSessionExists() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    HttpSession newSession = Mockito.mock(HttpSession.class);
    LoginRequest loginRequest = new LoginRequest("fresh@example.com", "Password123");
    CurrentUserPrincipal principal = principal(7, "fresh@example.com", "freshUser");
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    UserProfileResponse expectedProfile =
        new UserProfileResponse(7, "fresh@example.com", "freshUser", null, null, null, null,
            null, null, List.of());

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(request.getSession(false)).thenReturn(null);
    when(request.getSession(true)).thenReturn(newSession);
    when(userProfileService.getUserProfile(7)).thenReturn(expectedProfile);

    UserProfileResponse responseBody =
        authApplicationService.login(loginRequest, request, response);

    assertSame(expectedProfile, responseBody);
    verify(request).getSession(false);
    verify(request).getSession(true);
    verify(securityContextRepository).saveContext(any(), Mockito.eq(request), Mockito.eq(response));
  }

  @Test
  void loginInvalidatesExistingSessionBeforeCreatingNewOne() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    HttpSession existingSession = Mockito.mock(HttpSession.class);
    HttpSession newSession = Mockito.mock(HttpSession.class);
    CurrentUserPrincipal principal = principal(9, "existing@example.com", "existingUser");
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(request.getSession(false)).thenReturn(existingSession);
    when(request.getSession(true)).thenReturn(newSession);
    when(userProfileService.getUserProfile(9)).thenReturn(
        new UserProfileResponse(9, "existing@example.com", "existingUser", null, null, null, null,
            null, null, List.of()));

    authApplicationService.login(new LoginRequest("existing@example.com", "Password123"), request,
        response);

    verify(existingSession).invalidate();
    verify(request).getSession(true);
  }

  @Test
  void registerRejectsCaseInsensitiveDuplicateUsername() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("duplicate@example.com");
    request.setUserName("TakenName");
    request.setPassword("Password123");
    request.setDateOfBirth(Instant.parse("2000-01-01T00:00:00Z"));

    when(userJpaRepository.existsByEmailNormalized("duplicate@example.com")).thenReturn(false);
    when(userJpaRepository.existsByUserNameIgnoreCase("TakenName")).thenReturn(true);

    ConflictException exception = assertThrows(ConflictException.class,
        () -> authApplicationService.register(request));

    assertEquals("USERNAME_ALREADY_TAKEN", exception.getCode());
  }

  @Test
  void registerRejectsMissingDateOfBirth() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("new-user@example.com");
    request.setUserName("newUser");
    request.setPassword("Password123");
    request.setDateOfBirth(null);

    var exception = assertThrows(com.coactivity.service.exception.ValidationException.class,
        () -> authApplicationService.register(request));

    assertEquals("Date of birth is required", exception.getMessage());
    verify(userJpaRepository, never()).saveAndFlush(any(UserEntity.class));
  }

  @Test
  void registerRejectsTooShortPassword() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("new-user@example.com");
    request.setUserName("newUser");
    request.setPassword("short");
    request.setDateOfBirth(Instant.parse("2000-01-01T00:00:00Z"));

    var exception = assertThrows(com.coactivity.service.exception.ValidationException.class,
        () -> authApplicationService.register(request));

    assertEquals("Password length must be between 8 and 128 characters", exception.getMessage());
    verify(userJpaRepository, never()).saveAndFlush(any(UserEntity.class));
  }

  @Test
  void registerRollsBackPendingUserWhenVerificationEmailCannotBeDelivered() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("new-user@example.com");
    request.setUserName("newUser");
    request.setPassword("Password123");
    request.setDateOfBirth(Instant.parse("2000-01-01T00:00:00Z"));

    when(userJpaRepository.existsByEmailNormalized("new-user@example.com")).thenReturn(false);
    when(userJpaRepository.existsByUserNameIgnoreCase("newUser")).thenReturn(false);
    when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
    when(userJpaRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
      UserEntity entity = invocation.getArgument(0);
      entity.setId(42);
      return entity;
    });
    when(notificationService.sendRegistrationVerificationCode(eq("new-user@example.com"),
        anyString())).thenReturn(false);

    assertThrows(NotificationDeliveryException.class, () -> authApplicationService.register(request));

    verify(challengeStore).create(eq("new-user@example.com"), eq(42), anyString());
    verify(challengeStore).delete("new-user@example.com");
    verify(userJpaRepository).delete(any(UserEntity.class));
  }

  @Test
  void registerCreatesVerificationChallengeAndStartsResendCooldown() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("new-user@example.com");
    request.setUserName("newUser");
    request.setPassword("Password123");
    request.setDateOfBirth(Instant.parse("2000-01-01T00:00:00Z"));

    when(userJpaRepository.existsByEmailNormalized("new-user@example.com")).thenReturn(false);
    when(userJpaRepository.existsByUserNameIgnoreCase("newUser")).thenReturn(false);
    when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
    when(userJpaRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
      UserEntity entity = invocation.getArgument(0);
      entity.setId(42);
      return entity;
    });
    when(notificationService.sendRegistrationVerificationCode(eq("new-user@example.com"),
        anyString())).thenReturn(true);

    RegistrationResponse response = authApplicationService.register(request);

    assertEquals("PENDING_VERIFICATION", response.getStatus());
    verify(challengeStore).create(eq("new-user@example.com"), eq(42), anyString());
    verify(challengeStore).markResendCooldown("new-user@example.com");
  }

  @Test
  void verifyRegistrationActivatesPendingUserAndSetsTimestamp() {
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(12);
    pendingUser.setEmail("verify@example.com");
    pendingUser.setEmailNormalized("verify@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(pendingUser));
    when(challengeStore.verify("verify@example.com", "123456"))
        .thenReturn(VerificationResult.SUCCESS);

    authApplicationService.verifyRegistration(
        new RegisterVerificationRequest("verify@example.com", "123456"));

    assertEquals(UserStatus.ACTIVE, pendingUser.getStatus());
    assertNotNull(pendingUser.getEmailVerifiedAt());
  }

  @Test
  void verifyRegistrationRejectsInvalidCode() {
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(12);
    pendingUser.setEmail("verify@example.com");
    pendingUser.setEmailNormalized("verify@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(pendingUser));
    when(challengeStore.verify("verify@example.com", "000000"))
        .thenReturn(VerificationResult.INVALID_CODE);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> authApplicationService.verifyRegistration(
            new RegisterVerificationRequest("verify@example.com", "000000")));

    assertEquals("INVALID_VERIFICATION_CODE", exception.getCode());
    assertEquals("Invalid verification code", exception.getMessage());
  }

  @Test
  void resendRegistrationCodeReissuesCodeForPendingUser() {
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(12);
    pendingUser.setEmail("verify@example.com");
    pendingUser.setEmailNormalized("verify@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(pendingUser));
    when(challengeStore.tryActivateResendCooldown("verify@example.com")).thenReturn(true);
    when(notificationService.sendRegistrationVerificationCode(eq("verify@example.com"), anyString()))
        .thenReturn(true);

    authApplicationService.resendRegistrationCode(
        new ResendRegistrationVerificationRequest("verify@example.com"));

    verify(challengeStore).tryActivateResendCooldown("verify@example.com");
    verify(challengeStore).create(eq("verify@example.com"), eq(12), anyString());
    verify(notificationService).sendRegistrationVerificationCode(eq("verify@example.com"),
        anyString());
  }

  @Test
  void resendRegistrationCodeRejectsRequestsDuringCooldown() {
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(12);
    pendingUser.setEmail("verify@example.com");
    pendingUser.setEmailNormalized("verify@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(pendingUser));
    when(challengeStore.tryActivateResendCooldown("verify@example.com")).thenReturn(false);

    TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
        () -> authApplicationService.resendRegistrationCode(
            new ResendRegistrationVerificationRequest("verify@example.com")));

    assertEquals("REGISTRATION_CODE_RESEND_COOLDOWN", exception.getCode());
    verify(challengeStore, never()).create(anyString(), any(), anyString());
    verify(notificationService, never()).sendRegistrationVerificationCode(anyString(), anyString());
  }

  @Test
  void resendRegistrationCodeRollsBackNewCodeWhenEmailDeliveryFails() {
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(12);
    pendingUser.setEmail("verify@example.com");
    pendingUser.setEmailNormalized("verify@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(pendingUser));
    when(challengeStore.tryActivateResendCooldown("verify@example.com")).thenReturn(true);
    when(notificationService.sendRegistrationVerificationCode(eq("verify@example.com"), anyString()))
        .thenReturn(false);

    assertThrows(NotificationDeliveryException.class,
        () -> authApplicationService.resendRegistrationCode(
            new ResendRegistrationVerificationRequest("verify@example.com")));

    verify(challengeStore).create(eq("verify@example.com"), eq(12), anyString());
    verify(challengeStore).delete("verify@example.com");
    verify(challengeStore).clearResendCooldown("verify@example.com");
  }

  @Test
  void resendRegistrationCodeRejectsAlreadyVerifiedUser() {
    UserEntity activeUser = new UserEntity();
    activeUser.setId(12);
    activeUser.setEmail("verify@example.com");
    activeUser.setEmailNormalized("verify@example.com");
    activeUser.setStatus(UserStatus.ACTIVE);

    when(userJpaRepository.findByEmailNormalized("verify@example.com"))
        .thenReturn(Optional.of(activeUser));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> authApplicationService.resendRegistrationCode(
            new ResendRegistrationVerificationRequest("verify@example.com")));

    assertEquals("EMAIL_ALREADY_VERIFIED", exception.getCode());
    verify(challengeStore, never()).create(anyString(), any(), anyString());
    verify(notificationService, never()).sendRegistrationVerificationCode(anyString(), anyString());
  }

  @Test
  void requestPasswordResetCreatesChallengeForActiveUser() {
    UserEntity activeUser = new UserEntity();
    activeUser.setId(55);
    activeUser.setEmail("reset@example.com");
    activeUser.setEmailNormalized("reset@example.com");
    activeUser.setStatus(UserStatus.ACTIVE);

    when(passwordResetStore.tryActivateRequestCooldown("reset@example.com")).thenReturn(true);
    when(userJpaRepository.findByEmailNormalized("reset@example.com"))
        .thenReturn(Optional.of(activeUser));
    when(notificationService.sendPasswordResetCode(eq("reset@example.com"), anyString()))
        .thenReturn(true);

    authApplicationService.requestPasswordReset(new PasswordResetRequest("reset@example.com"));

    verify(passwordResetStore).create(eq("reset@example.com"), eq(55), anyString());
    verify(notificationService).sendPasswordResetCode(eq("reset@example.com"), anyString());
  }

  @Test
  void requestPasswordResetDoesNotRevealMissingAccount() {
    when(passwordResetStore.tryActivateRequestCooldown("missing@example.com")).thenReturn(true);
    when(userJpaRepository.findByEmailNormalized("missing@example.com")).thenReturn(Optional.empty());

    authApplicationService.requestPasswordReset(new PasswordResetRequest("missing@example.com"));

    verify(passwordResetStore, never()).create(anyString(), any(), anyString());
    verify(notificationService, never()).sendPasswordResetCode(anyString(), anyString());
  }

  @Test
  void verifyPasswordResetRejectsInvalidCode() {
    when(passwordResetStore.inspect("reset@example.com", "000000"))
        .thenReturn(VerificationResult.INVALID_CODE);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> authApplicationService.verifyPasswordReset(
            new PasswordResetVerifyRequest("reset@example.com", "000000")));

    assertEquals("INVALID_PASSWORD_RESET_CODE", exception.getCode());
  }

  @Test
  void verifyPasswordResetRejectsExpiredCode() {
    when(passwordResetStore.inspect("reset@example.com", "111111"))
        .thenReturn(VerificationResult.EXPIRED_OR_MISSING);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> authApplicationService.verifyPasswordReset(
            new PasswordResetVerifyRequest("reset@example.com", "111111")));

    assertEquals("PASSWORD_RESET_CODE_EXPIRED", exception.getCode());
  }

  @Test
  void confirmPasswordResetUpdatesPasswordAndInvalidatesSessions() {
    UserEntity activeUser = new UserEntity();
    activeUser.setId(55);
    activeUser.setEmail("reset@example.com");
    activeUser.setEmailNormalized("reset@example.com");
    activeUser.setStatus(UserStatus.ACTIVE);

    when(passwordResetStore.consume("reset@example.com", "123456"))
        .thenReturn(VerificationResult.SUCCESS);
    when(userJpaRepository.findByEmailNormalized("reset@example.com"))
        .thenReturn(Optional.of(activeUser));
    when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

    authApplicationService.confirmPasswordReset(
        new PasswordResetConfirmRequest("reset@example.com", "123456", "NewPassword123"));

    assertEquals("encoded-new-password", activeUser.getPasswordHash());
    verify(passwordResetStore).clearRequestCooldown("reset@example.com");
    verify(sessionInvalidationService).invalidateAllSessions("reset@example.com");
  }

  @Test
  void loginRejectsPendingVerificationUser() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    UserEntity pendingUser = new UserEntity();
    pendingUser.setId(17);
    pendingUser.setEmail("pending@example.com");
    pendingUser.setEmailNormalized("pending@example.com");
    pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);

    when(authenticationManager.authenticate(any()))
        .thenThrow(new DisabledException("disabled"));
    when(userJpaRepository.findByEmailNormalized("pending@example.com"))
        .thenReturn(Optional.of(pendingUser));

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> authApplicationService.login(
            new LoginRequest("pending@example.com", "Password123"), request, response));

    assertEquals("EMAIL_NOT_VERIFIED", exception.getCode());
    assertEquals("Email is not verified", exception.getMessage());
  }

  @Test
  void loginRejectsDisabledUserWithoutInternalError() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    when(authenticationManager.authenticate(any()))
        .thenThrow(new LockedException("locked"));

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> authApplicationService.login(
            new LoginRequest("disabled@example.com", "Password123"), request, response));

    assertEquals("ACCOUNT_DISABLED", exception.getCode());
    assertEquals("User account is disabled", exception.getMessage());
  }

  private CurrentUserPrincipal principal(Integer id, String email, String userName) {
    return new CurrentUserPrincipal(id, email, email, userName, "hash", UserStatus.ACTIVE,
        Instant.now());
  }
}
