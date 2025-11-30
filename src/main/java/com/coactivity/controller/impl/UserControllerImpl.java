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
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    try {
      RegistrationResponse response = userService.registerUser(request);
      URI location = response != null && response.getUserId() != null
          ? URI.create("/api/users/" + response.getUserId())
          : URI.create("/api/users");
      return ResponseEntity.created(location).body(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PostMapping("/login")
  public ResponseEntity<Void> loginUser(@Valid @RequestBody LoginRequest request) {
    try {
      userService.loginUser(request);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PostMapping("/login/verify")
  public ResponseEntity<LoginResponse> verifyLogin(
      @RequestParam("login") String login,
      @RequestParam("code") String verificationCode) {
    try {
      LoginResponse response = userService.verifyLogin(login, verificationCode);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PostMapping("/logout")
  public ResponseEntity<Void> logoutUser(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.logoutUser(authToken);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UserProfileResponse response = userService.getUserProfile(authToken);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/{userId}")
  public ResponseEntity<UserSummaryResponse> getPublicUserProfileById(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer userId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      UserSummaryResponse response = userService.getPublicUserProfileById(authToken, userId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateUserProfile(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      UserProfileResponse response = userService.updateUserProfile(authToken, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PutMapping("/me/password")
  public ResponseEntity<LoginResponse> updatePassword(
      @RequestHeader(name = "Authorization", required = false) String token,
      @RequestParam String currentPassword,
      @RequestParam String newPassword) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      LoginResponse response =
          userService.updatePassword(authToken, currentPassword, newPassword);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @PutMapping("/me/notifications")
  public ResponseEntity<NotificationSettingsResponse> configureNotificationSettings(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody NotificationSettingsRequest request) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      NotificationSettingsResponse response =
          userService.configureNotificationSettings(authToken, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userService.deleteAccount(authToken);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> assignAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId,
      @PathVariable Integer userId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      RoleAssignmentResponse response =
          userWithRoomService.assignAdminRole(authToken, roomId, userId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Override
  @DeleteMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> demoteAdminRole(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId,
      @PathVariable Integer userId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      RoleAssignmentResponse response =
          userWithRoomService.demoteAdminRole(authToken, roomId, userId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Override
  @GetMapping("/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Integer userId = tokenService.decodeToken(authToken).userId();
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @GetMapping("/rooms/{roomId}/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequestsForRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Integer userId = tokenService.decodeToken(authToken).userId();
    try {
      List<JoinRequestResponse> responses = joinRequestService.getPendingRequestsForRoom(userId,
          roomId);
      return ResponseEntity.ok(responses);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (SecurityException ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @Override
  @PostMapping("/requests/{requestId}")
  public ResponseEntity<Void> processJoinRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer requestId,
      @RequestParam("action") RequestStatus action) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Integer userId = tokenService.decodeToken(authToken).userId();
    try {
      joinRequestService.processJoinRequest(userId, requestId, action);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Override
  @GetMapping("/requests/sent")
  public ResponseEntity<List<JoinRequestResponse>> getSentRequests(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Integer userId = tokenService.decodeToken(authToken).userId();
    List<JoinRequestResponse> responses = joinRequestService.getSentRequests(userId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @DeleteMapping("/requests/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer requestId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Integer userId = tokenService.decodeToken(authToken).userId();
    try {
      joinRequestService.cancelRequest(userId, requestId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Override
  @GetMapping("/banned-rooms")
  public ResponseEntity<List<RoomSummaryResponse>> getBanRooms(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<RoomSummaryResponse> rooms = userWithRoomService.getBanRooms(authToken);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @GetMapping("/rooms/{roomId}/membership")
  public ResponseEntity<Boolean> isUserInRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Boolean result = userWithRoomService.isUserInRoom(authToken, roomId);
    return ResponseEntity.ok(result);
  }

  private boolean isInvalidToken(String token) {
    return token == null || token.isBlank() || !tokenService.isTokenActive(token);
  }

  private String extractToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
  }
}
