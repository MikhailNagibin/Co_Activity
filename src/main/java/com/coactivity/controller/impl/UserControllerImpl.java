package com.coactivity.controller.impl;

import com.coactivity.controller.UserController;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.UserWithRoomService;
import com.coactivity.service.exception.TokenValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserControllerImpl implements UserController {

  private final UserProfileService userService;
  private final TokenService tokenService;
  private final UserWithRoomService userWithRoomService;
  private final JoinRequestService joinRequestService;

  public UserControllerImpl(UserProfileService userService, TokenService tokenService,
      UserWithRoomService userWithRoomService, JoinRequestService joinRequestService) {
    this.userService = userService;
    this.tokenService = tokenService;
    this.userWithRoomService = userWithRoomService;
    this.joinRequestService = joinRequestService;
  }

  @Override
  @PostMapping
  public ResponseEntity<RegistrationResponse> registerUser(
      @Valid @RequestBody UserRegistrationRequest request) {
    RegistrationResponse response = userService.registerUser(request);
    URI location = Objects.requireNonNull(response != null && response.getUserId() != null
        ? URI.create("/api/users/" + response.getUserId())
        : URI.create("/api/users"));
    return ResponseEntity.created(location).body(response);
  }

  @Override
  @PostMapping("/login")
  public ResponseEntity<Void> loginUser(@Valid @RequestBody LoginRequest request) {
    userService.loginUser(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @PostMapping("/login/verify")
  public ResponseEntity<LoginResponse> verifyLogin(
      @RequestParam("login") @NotBlank String login,
      @RequestParam("code") @NotBlank String verificationCode) {
    LoginResponse response = userService.verifyLogin(login, verificationCode);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/logout")
  public ResponseEntity<Void> logoutUser(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    userService.logoutUser(authToken);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    UserProfileResponse response = userService.getUserProfile(authToken);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/{userId}")
  public ResponseEntity<UserSummaryResponse> getPublicUserProfileById(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer userId) {
    String authToken = requireAuthorizedToken(token);
    UserSummaryResponse response = userService.getPublicUserProfileById(authToken, userId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    String authToken = requireAuthorizedToken(token);
    userService.updateUserProfile(authToken, request);
    UserProfileResponse response = userService.getUserProfile(authToken);
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/me/password")
  public ResponseEntity<LoginResponse> updatePassword(
      @RequestHeader(name = "Authorization", required = false) String token,
      @RequestParam @NotBlank String currentPassword,
      @RequestParam @NotBlank String newPassword) {
    String authToken = requireAuthorizedToken(token);
    LoginResponse response = userService.updatePassword(authToken, currentPassword, newPassword);
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/me/notifications")
  public ResponseEntity<NotificationSettingsResponse> configureNotificationSettings(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody NotificationSettingsRequest request) {
    String authToken = requireAuthorizedToken(token);
    NotificationSettingsResponse response = userService.configureNotificationSettings(authToken,
        request);
    return ResponseEntity.ok(response);
  }

  @Override
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    userService.deleteAccount(authToken);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> assignAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId,
      @PathVariable @Positive Integer userId) {

    Integer requesterId = resolveAuthorizedUserId(token);
    RoleAssignmentResponse response =
        userWithRoomService.assignAdminRole(requesterId, roomId, userId);
    return ResponseEntity.ok(response);
  }

  @Override
  @DeleteMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> demoteAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId,
      @PathVariable @Positive Integer userId) {
    Integer requesterId = resolveAuthorizedUserId(token);
    RoleAssignmentResponse response =
        userWithRoomService.demoteAdminRole(requesterId, roomId, userId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);

    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @GetMapping("/rooms/{roomId}/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequestsForRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer userId = resolveAuthorizedUserId(token);
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequestsForRoom(userId,
        roomId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @PostMapping("/requests/{requestId}")
  public ResponseEntity<Void> processJoinRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer requestId,
      @RequestParam("action") @NotNull RequestStatus action) {
    Integer userId = resolveAuthorizedUserId(token);
    joinRequestService.processJoinRequest(userId, requestId, action);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/requests/sent")
  public ResponseEntity<List<JoinRequestResponse>> getSentRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);
    List<JoinRequestResponse> responses = joinRequestService.getSentRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @DeleteMapping("/requests/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer requestId) {
    Integer userId = resolveAuthorizedUserId(token);
    joinRequestService.cancelRequest(userId, requestId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/banned-rooms")
  public ResponseEntity<List<RoomSummaryResponse>> getBanRooms(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);
    List<RoomSummaryResponse> rooms = userWithRoomService.getBanRooms(userId);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @GetMapping("/rooms/{roomId}/membership")
  public ResponseEntity<Boolean> isUserInRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer userId = resolveAuthorizedUserId(token);
    Boolean result = userWithRoomService.isUserInRoom(userId, roomId);
    return ResponseEntity.ok(result);
  }

  private String requireAuthorizedToken(String rawToken) {
    String token = extractToken(rawToken);
    if (token == null || token.isBlank()) {
      throw new TokenValidationException("Authorization token is required");
    }
    if (!tokenService.isTokenActive(token)) {
      throw new TokenValidationException("Token is inactive or expired");
    }
    return token;
  }

  private Integer resolveAuthorizedUserId(String tokenHeader) {
    String token = requireAuthorizedToken(tokenHeader);
    return tokenService.decodeToken(token).userId();
  }

  private String extractToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
  }
}
