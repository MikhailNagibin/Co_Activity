package com.coactivity.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.auth.model.UserStatus;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.NotificationService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.exception.ConflictException;
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
  private AuthApplicationService authApplicationService;

  @BeforeEach
  void setUp() {
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    authenticationManager = Mockito.mock(AuthenticationManager.class);
    securityContextRepository = Mockito.mock(SecurityContextRepository.class);
    userProfileService = Mockito.mock(UserProfileService.class);

    authApplicationService = new AuthApplicationService(
        userJpaRepository,
        Mockito.mock(PasswordEncoder.class),
        Mockito.mock(NotificationService.class),
        Mockito.mock(RedisChallengeStore.class),
        authenticationManager,
        securityContextRepository,
        userProfileService,
        Mockito.mock(SessionInvalidationService.class),
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
            null, List.of());

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
            null, List.of()));

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

  private CurrentUserPrincipal principal(Integer id, String email, String userName) {
    return new CurrentUserPrincipal(id, email, email, userName, "hash", UserStatus.ACTIVE,
        Instant.now());
  }
}
