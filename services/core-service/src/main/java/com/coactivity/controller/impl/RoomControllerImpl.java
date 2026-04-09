package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.OwnershipTransferRequest;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.request.RoomUpdateRequest;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.OwnershipTransferResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Role;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomImageContent;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rooms")
@Validated
public class RoomControllerImpl {

  private final RoomService roomService;
  private final RoomMembershipService roomMembershipService;
  private final BulletinBoardService bulletinBoardService;
  private final RoomImageService roomImageService;

  public RoomControllerImpl(RoomService roomService,
      RoomMembershipService roomMembershipService,
      BulletinBoardService bulletinBoardService,
      RoomImageService roomImageService) {
    this.roomService = roomService;
    this.roomMembershipService = roomMembershipService;
    this.bulletinBoardService = bulletinBoardService;
    this.roomImageService = roomImageService;
  }

  @PostMapping("/createRoom")
  public ResponseEntity<RoomCreationResponse> createRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody RoomCreationRequest request) {
    RoomCreationResponse response = roomService.createRoom(currentUser.getUserId(), request);
    URI location = Objects.requireNonNull(response.getRoomId() != null
        ? URI.create("/api/rooms/" + response.getRoomId())
        : URI.create("/api/rooms"));
    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{roomId}")
  public ResponseEntity<RoomDetailedResponse> updateRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Valid @RequestBody RoomUpdateRequest request) {
    RoomDetailedResponse response =
        roomService.updateRoom(currentUser.getUserId(), roomId, request);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{roomId}/bulletin")
  public ResponseEntity<BulletinBoardResponse> updateBulletinBoard(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @NotBlank @RequestBody String newContent) {
    BulletinBoardResponse response =
        bulletinBoardService.updateBulletinBoard(roomId, newContent, currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/bulletin")
  public ResponseEntity<Void> deleteBulletinBoard(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    bulletinBoardService.deleteBulletinBoard(roomId, currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<List<RoomSummaryResponse>> getRooms(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @ModelAttribute RoomFilter filter,
      @RequestParam(name = "sortBy", required = false) RoomSort sortBy) {
    Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
    List<RoomSummaryResponse> rooms = roomService.getRooms(currentUserId, filter, sortBy);
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDetailedResponse> getRoomById(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
    RoomDetailedResponse response = roomService.getRoomById(roomId, currentUserId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/images/{imageId}")
  public ResponseEntity<ByteArrayResource> getRoomImage(
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer imageId) {
    RoomImageContent imageContent = roomImageService.getRoomImageContent(roomId, imageId);
    ByteArrayResource resource = new ByteArrayResource(imageContent.bytes());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(imageContent.contentType()))
        .contentLength(imageContent.sizeBytes())
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(resource);
  }

  @GetMapping("/me")
  public ResponseEntity<List<RoomDetailedResponse>> getUserRooms(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<RoomDetailedResponse> rooms = roomMembershipService.getUserRooms(currentUser.getUserId());
    return ResponseEntity.ok(rooms);
  }

  @PostMapping("/{roomId}/join")
  public ResponseEntity<Void> joinRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomMembershipService.joinRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/leave")
  public ResponseEntity<Void> leaveRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomMembershipService.leaveRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomService.deleteRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<List<RoomImageResponse>> uploadRoomImages(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @RequestParam("files") MultipartFile[] files) {
    List<RoomImageResponse> response =
        roomImageService.uploadRoomImages(currentUser.getUserId(), roomId, files);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/images/{imageId}")
  public ResponseEntity<List<RoomImageResponse>> deleteRoomImage(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer imageId) {
    List<RoomImageResponse> response =
        roomImageService.deleteRoomImage(currentUser.getUserId(), roomId, imageId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/participants")
  public ResponseEntity<List<RoomParticipantResponse>> getRoomParticipants(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @RequestParam(name = "role", required = false) Role roleFilter) {
    List<RoomParticipantResponse> participants =
        roomMembershipService.getRoomParticipants(currentUser.getUserId(), roomId, roleFilter);

    return ResponseEntity.ok(participants);
  }

  @GetMapping("/{roomId}/participants/{userId}")
  public ResponseEntity<MembershipVerificationResponse> isUserInRoom(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    MembershipVerificationResponse response =
        roomMembershipService.verifyUserMembership(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/participants/{userId}")
  public ResponseEntity<Void> removeParticipant(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.removeParticipant(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/bans/{userId}")
  public ResponseEntity<Void> banUser(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.banUser(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{roomId}/bans")
  public ResponseEntity<List<UserSummaryResponse>> getBannedUsers(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    List<UserSummaryResponse> response =
        roomMembershipService.getBannedUsers(currentUser.getUserId(), roomId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/bans/{userId}")
  public ResponseEntity<Void> unbanUser(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.unbanUser(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/ownership/transfer")
  public ResponseEntity<OwnershipTransferResponse> transferOwnership(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Valid @RequestBody OwnershipTransferRequest request) {
    OwnershipTransferResponse response =
        roomMembershipService.transferOwnership(currentUser.getUserId(), roomId,
            request.getTargetUserId());
    return ResponseEntity.ok(response);
  }
}
