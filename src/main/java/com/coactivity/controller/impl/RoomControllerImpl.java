package com.coactivity.controller.impl;

import com.coactivity.controller.RoomController;
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
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserWithRoomService;
import com.coactivity.service.exception.TokenValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Validated
public class RoomControllerImpl implements RoomController {

  private final RoomService roomService;
  private final TokenService tokenService;
  private final UserWithRoomService userWithRoomService;
  private final BulletinBoardService bulletinBoardService;

  public RoomControllerImpl(RoomService roomService,
      TokenService tokenService,
      UserWithRoomService userWithRoomService,
      BulletinBoardService bulletinBoardService) {
    this.roomService = roomService;
    this.tokenService = tokenService;
    this.userWithRoomService = userWithRoomService;
    this.bulletinBoardService = bulletinBoardService;
  }

  @Override
  @PostMapping("/createRoom")
  public ResponseEntity<RoomCreationResponse> createRoom(

      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody RoomCreationRequest request) {

    Integer ownerId = resolveAuthorizedUserId(token);
    RoomCreationResponse response = roomService.createRoom(ownerId, request);
    URI location = Objects.requireNonNull(response.getRoomId() != null
        ? URI.create("/api/rooms/" + response.getRoomId())
        : URI.create("/api/rooms"));
    return ResponseEntity.created(location).body(response);
  }

  @Override
  @PutMapping("/{roomId}/bulletin")
  public ResponseEntity<BulletinBoardResponse> updateBulletinBoard(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId,
      @RequestBody @NotBlank String newContent) {

    Integer authorId = resolveAuthorizedUserId(token);
    BulletinBoardResponse response =
        bulletinBoardService.updateBulletinBoard(roomId, newContent, authorId);
    return ResponseEntity.ok(response);
  }

  @Override
  @DeleteMapping("/{roomId}/bulletin")
  public ResponseEntity<Void> deleteBulletinBoard(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer requesterId = resolveAuthorizedUserId(token);
    bulletinBoardService.deleteBulletinBoard(roomId, requesterId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping
  public ResponseEntity<List<RoomSummaryResponse>> getRooms(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @ModelAttribute RoomFilter filter,
      @RequestParam(name = "sortBy", required = false) RoomSort sortBy) {
    Integer currentUserId = resolveOptionalUserId(token);
    List<RoomSummaryResponse> rooms = roomService.getRooms(currentUserId, filter, sortBy);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDetailedResponse> getRoomById(@PathVariable @Positive Integer roomId,
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer currentUserId = resolveOptionalUserId(token);
    RoomDetailedResponse response = roomService.getRoomById(roomId, currentUserId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/me")
  public ResponseEntity<List<RoomDetailedResponse>> getUserRooms(
      @RequestHeader(name = "Authorization", required = false) String token) {
    Integer currentUserId = resolveAuthorizedUserId(token);
    List<RoomDetailedResponse> rooms = userWithRoomService.getUserRooms(currentUserId);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @PostMapping("/{roomId}/join")
  public ResponseEntity<Void> joinRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer currentUserId = resolveAuthorizedUserId(token);
    userWithRoomService.joinRoom(currentUserId, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{roomId}/leave")
  public ResponseEntity<Void> leaveRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer currentUserId = resolveAuthorizedUserId(token);
    userWithRoomService.leaveRoom(currentUserId, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId) {
    Integer currentUserId = resolveAuthorizedUserId(token);
    roomService.deleteRoom(currentUserId, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{roomId}/participants")
  public ResponseEntity<List<RoomParticipantResponse>> getRoomParticipants(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId,
      @RequestParam(name = "role", required = false) Role roleFilter) {

    Integer currentUserId = resolveAuthorizedUserId(token);
    List<RoomParticipantResponse> participants =
        userWithRoomService.getRoomParticipants(currentUserId, roomId, roleFilter);

    return ResponseEntity.ok(participants);
  }

  @Override
  @GetMapping("/{roomId}/participants/{userId}")
  public ResponseEntity<MembershipVerificationResponse> isUserInRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable @Positive Integer roomId,
      @PathVariable @Positive Integer userId) {
    Integer currentUserId = resolveAuthorizedUserId(token);
    MembershipVerificationResponse response =
        userWithRoomService.verifyUserMembership(currentUserId, roomId, userId);
    return ResponseEntity.ok(response);
  }

  private Integer resolveAuthorizedUserId(String tokenHeader) {
    String authToken = extractToken(tokenHeader);
    if (authToken == null || authToken.isBlank()) {
      throw new TokenValidationException("Authorization token is required");
    }
    if (!tokenService.isTokenActive(authToken)) {
      throw new TokenValidationException("Token is inactive or expired");
    }
    return tokenService.decodeToken(authToken).userId();
  }

  private Integer resolveOptionalUserId(String tokenHeader) {
    String authToken = extractToken(tokenHeader);
    if (authToken == null || authToken.isBlank()) {
      return null;
    }
    if (!tokenService.isTokenActive(authToken)) {
      return null;
    }
    return tokenService.decodeToken(authToken).userId();
  }

  private String extractToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
  }
}
