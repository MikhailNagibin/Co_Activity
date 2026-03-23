package com.coactivity.controller.impl;

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
import com.coactivity.service.AuthService;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.exception.TokenValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
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
public class UserControllerImpl {

  private final UserProfileService userProfileService;
  private final AuthService authService;
  private final TokenService tokenService;
  private final RoomMembershipService roomMembershipService;
  private final JoinRequestService joinRequestService;

  public UserControllerImpl(UserProfileService userProfileService, AuthService authService,
      TokenService tokenService, RoomMembershipService roomMembershipService,
      JoinRequestService joinRequestService) {
    this.userProfileService = userProfileService;
    this.authService = authService;
    this.tokenService = tokenService;
    this.roomMembershipService = roomMembershipService;
    this.joinRequestService = joinRequestService;
  }

  @PostMapping
  public ResponseEntity<RegistrationResponse> registerUser(
    @Valid @RequestBody UserRegistrationRequest request) {
    RegistrationResponse response = userProfileService.registerUser(request);
    URI location = URI.create("/api/users/" + response.getUserId());
    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<Void> loginUser(@Valid @RequestBody LoginRequest request) {
    authService.loginUser(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/login/verify")
  public ResponseEntity<LoginResponse> verifyLogin(
      @NotBlank @RequestParam("login") String login,
      @NotBlank @RequestParam("code") String verificationCode) {
    LoginResponse response = authService.verifyLogin(login, verificationCode);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logoutUser(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    authService.logoutUser(authToken);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    UserProfileResponse response = userProfileService.getUserProfile(authToken);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserSummaryResponse> getPublicUserProfileById(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer userId) {
    String authToken = requireAuthorizedToken(token);
    UserSummaryResponse response = userProfileService.getPublicUserProfileById(authToken, userId);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    String authToken = requireAuthorizedToken(token);
    userProfileService.updateUserProfile(authToken, request);
    UserProfileResponse response = userProfileService.getUserProfile(authToken);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me/password")
  public ResponseEntity<LoginResponse> updatePassword(
      @RequestHeader(name = "Authorization", required = false) String token,
      @NotBlank @RequestParam String currentPassword,
      @NotBlank @RequestParam String newPassword) {
    String authToken = requireAuthorizedToken(token);
    LoginResponse response = authService.updatePassword(authToken, currentPassword, newPassword);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me/notifications")
  public ResponseEntity<NotificationSettingsResponse> configureNotificationSettings(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody NotificationSettingsRequest request) {
    String authToken = requireAuthorizedToken(token);
    NotificationSettingsResponse response = userProfileService.configureNotificationSettings(authToken,
        request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = requireAuthorizedToken(token);
    userProfileService.deleteAccount(authToken);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> assignAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {

    Integer requesterId = resolveAuthorizedUserId(token);
    RoleAssignmentResponse response =
        roomMembershipService.assignAdminRole(requesterId, roomId, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> demoteAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    Integer requesterId = resolveAuthorizedUserId(token);
    RoleAssignmentResponse response =
        roomMembershipService.demoteAdminRole(requesterId, roomId, userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);

    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/rooms/{roomId}/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequestsForRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer roomId) {
    Integer userId = resolveAuthorizedUserId(token);
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequestsForRoom(userId,
        roomId);
    return ResponseEntity.ok(responses);
  }

  @PostMapping("/requests/{requestId}")
  public ResponseEntity<Void> processJoinRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer requestId,
      @NotNull @RequestParam("action") RequestStatus action) {
    Integer userId = resolveAuthorizedUserId(token);
    joinRequestService.processJoinRequest(userId, requestId, action);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/requests/sent")
  public ResponseEntity<List<JoinRequestResponse>> getSentRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);
    List<JoinRequestResponse> responses = joinRequestService.getSentRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/requests/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer requestId) {
    Integer userId = resolveAuthorizedUserId(token);
    joinRequestService.cancelRequest(userId, requestId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/banned-rooms")
  public ResponseEntity<List<RoomSummaryResponse>> getBanRooms(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer userId = resolveAuthorizedUserId(token);
    List<RoomSummaryResponse> rooms = roomMembershipService.getBanRooms(userId);
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/rooms/{roomId}/membership")
  public ResponseEntity<Boolean> isUserInRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Positive @PathVariable Integer roomId) {
    Integer userId = resolveAuthorizedUserId(token);
    Boolean result = roomMembershipService.isUserInRoom(userId, roomId);
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
