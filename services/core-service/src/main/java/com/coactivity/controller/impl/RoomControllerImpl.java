package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.domain.Role;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import java.util.Objects;
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

@RestController
@RequestMapping("/api/rooms")
@Validated
public class RoomControllerImpl {

  private final RoomService roomService;
  private final RoomMembershipService roomMembershipService;
  private final BulletinBoardService bulletinBoardService;

  public RoomControllerImpl(RoomService roomService,
      RoomMembershipService roomMembershipService,
      BulletinBoardService bulletinBoardService) {
    this.roomService = roomService;
    this.roomMembershipService = roomMembershipService;
    this.bulletinBoardService = bulletinBoardService;
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
    List<RoomSummaryResponse> rooms = roomService.getRooms(currentUser.getUserId(), filter, sortBy);
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDetailedResponse> getRoomById(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    RoomDetailedResponse response = roomService.getRoomById(roomId, currentUser.getUserId());
    return ResponseEntity.ok(response);
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
}
