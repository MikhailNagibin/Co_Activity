package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.NotificationDeliveryException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

  private UserRepository userRepository;
  private TokenService tokenService;
  private NotificationService notificationService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    tokenService = Mockito.mock(TokenService.class);
    notificationService = Mockito.mock(NotificationService.class);
    authService = new AuthService(userRepository, tokenService, notificationService);
  }

  @Test
  void loginUserRemovesPendingVerificationWhenCodeCannotBeDelivered() {
    String login = "student@example.com";
    User user = new User(
        7,
        login,
        "student",
        "hashedPassword",
        Instant.parse("2000-01-01T00:00:00Z"),
        "Russia",
        "Moscow",
        "About",
        1,
        List.of(),
        List.of());

    when(userRepository.getUser(login, "Password123")).thenReturn(user);
    when(notificationService.sendLoginVerificationCode(eq(login), anyString())).thenReturn(false);

    LoginRequest request = new LoginRequest(login, "Password123");

    assertThrows(NotificationDeliveryException.class, () -> authService.loginUser(request));

    @SuppressWarnings("unchecked")
    Map<String, ?> pendingVerifications =
        (Map<String, ?>) ReflectionTestUtils.getField(authService, "pendingVerifications");

    assertFalse(pendingVerifications.containsKey(login));
    verify(notificationService).sendLoginVerificationCode(eq(login), anyString());
  }

  @Test
  void loginUserKeepsPendingVerificationWhenKafkaFailsAndDevFlagEnabled() {
    String login = "student@example.com";
    User user = new User(
        7,
        login,
        "student",
        "hashedPassword",
        Instant.parse("2000-01-01T00:00:00Z"),
        "Russia",
        "Moscow",
        "About",
        1,
        List.of(),
        List.of());

    when(userRepository.getUser(login, "Password123")).thenReturn(user);
    when(notificationService.sendLoginVerificationCode(eq(login), anyString())).thenReturn(false);

    LoginRequest request = new LoginRequest(login, "Password123");

    ReflectionTestUtils.setField(authService, "allowWithoutKafkaDelivery", true);
    authService.loginUser(request);

    @SuppressWarnings("unchecked")
    Map<String, ?> pendingVerifications =
        (Map<String, ?>) ReflectionTestUtils.getField(authService, "pendingVerifications");

    assertTrue(pendingVerifications.containsKey(login.toLowerCase()));
    verify(notificationService).sendLoginVerificationCode(eq(login), anyString());
  }
}
