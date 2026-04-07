package com.coactivity.controller.impl;

import com.coactivity.auth.service.AuthApplicationService;
import com.coactivity.controller.dto.request.AccountDeletionRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.AccountDeletionPreviewResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.service.AccountDeletionService;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.UserAvatarContent;
import com.coactivity.service.UserAvatarService;
import com.coactivity.service.UserProfileService;
import com.coactivity.auth.service.SessionInvalidationService;
import com.coactivity.security.CurrentUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserControllerImpl {

  private final UserProfileService userProfileService;
  private final RoomMembershipService roomMembershipService;
  private final JoinRequestService joinRequestService;
  private final SessionInvalidationService sessionInvalidationService;
  private final AccountDeletionService accountDeletionService;
  private final AuthApplicationService authApplicationService;
  private final UserAvatarService userAvatarService;

  public UserControllerImpl(UserProfileService userProfileService,
      RoomMembershipService roomMembershipService,
      JoinRequestService joinRequestService,
      SessionInvalidationService sessionInvalidationService,
      AccountDeletionService accountDeletionService,
      AuthApplicationService authApplicationService,
      UserAvatarService userAvatarService) {
    this.userProfileService = userProfileService;
    this.roomMembershipService = roomMembershipService;
    this.joinRequestService = joinRequestService;
    this.sessionInvalidationService = sessionInvalidationService;
    this.accountDeletionService = accountDeletionService;
    this.authApplicationService = authApplicationService;
    this.userAvatarService = userAvatarService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserSummaryResponse> getPublicUserProfileById(
      @Positive @PathVariable Integer userId) {
    UserSummaryResponse response = userProfileService.getPublicUserProfileById(userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}/avatar")
  public ResponseEntity<ByteArrayResource> getUserAvatar(
      @Positive @PathVariable Integer userId) {
    UserAvatarContent avatarContent = userAvatarService.getAvatarContent(userId);
    ByteArrayResource resource = new ByteArrayResource(avatarContent.bytes());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(avatarContent.contentType()))
        .contentLength(avatarContent.sizeBytes())
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(resource);
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateUserProfile(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    userProfileService.updateUserProfile(currentUser.getUserId(), request);
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserProfileResponse> uploadAvatar(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @RequestParam("file") MultipartFile file) {
    userAvatarService.uploadAvatar(currentUser.getUserId(), file);
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/me/avatar")
  public ResponseEntity<Void> deleteAvatar(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    userAvatarService.deleteAvatar(currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/notifications")
  public ResponseEntity<NotificationSettingsResponse> configureNotificationSettings(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody NotificationSettingsRequest request) {
    NotificationSettingsResponse response = userProfileService.configureNotificationSettings(
        currentUser.getUserId(), request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me/deletion-preview")
  public ResponseEntity<AccountDeletionPreviewResponse> getDeletionPreview(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    AccountDeletionPreviewResponse response =
        accountDeletionService.getDeletionPreview(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    accountDeletionService.deleteAccountIfNoOwnedRooms(currentUser.getUserId());
    completeAccountDeletion(currentUser, authentication, request, response);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/me/deletion")
  public ResponseEntity<Void> deleteAccountWithOwnedRooms(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      @Valid @RequestBody AccountDeletionRequest deletionRequest) {
    accountDeletionService.deleteAccount(currentUser.getUserId(), deletionRequest);
    completeAccountDeletion(currentUser, authentication, request, response);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> assignAdminRole(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    RoleAssignmentResponse response =
        roomMembershipService.assignAdminRole(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/rooms/{roomId}/admins/{userId}")
  public ResponseEntity<RoleAssignmentResponse> demoteAdminRole(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    RoleAssignmentResponse response =
        roomMembershipService.demoteAdminRole(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(
        currentUser.getUserId());
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/rooms/{roomId}/requests/pending")
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequestsForRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequestsForRoom(
        currentUser.getUserId(),
        roomId);
    return ResponseEntity.ok(responses);
  }

  @PostMapping("/requests/{requestId}")
  public ResponseEntity<Void> processJoinRequest(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer requestId,
      @NotNull @RequestParam("action") RequestStatus action) {
    joinRequestService.processJoinRequest(currentUser.getUserId(), requestId, action);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/requests/sent")
  public ResponseEntity<List<JoinRequestResponse>> getSentRequests(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<JoinRequestResponse> responses = joinRequestService.getSentRequests(
        currentUser.getUserId());
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/requests/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer requestId) {
    joinRequestService.cancelRequest(currentUser.getUserId(), requestId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/banned-rooms")
  public ResponseEntity<List<RoomSummaryResponse>> getBanRooms(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<RoomSummaryResponse> rooms = roomMembershipService.getBanRooms(currentUser.getUserId());
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/rooms/{roomId}/membership")
  public ResponseEntity<Boolean> isUserInRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    Boolean result = roomMembershipService.isUserInRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.ok(result);
  }

  private void completeAccountDeletion(CurrentUserPrincipal currentUser,
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    authApplicationService.logout(request, response, authentication);
    sessionInvalidationService.invalidateAllSessions(currentUser.getUsername());
  }
}
