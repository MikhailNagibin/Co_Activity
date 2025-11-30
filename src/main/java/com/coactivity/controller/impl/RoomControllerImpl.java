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
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  @PostMapping
  public ResponseEntity<RoomCreationResponse> createRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody RoomCreationRequest request) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    RoomCreationResponse response = roomService.createRoom(authToken, request);
    URI location = response != null && response.roomId() != null
        ? URI.create("/api/rooms/" + response.roomId())
        : URI.create("/api/rooms");
    return ResponseEntity.created(location).body(response);
  }

  @Override
  @PutMapping("/{roomId}/bulletin")
  public ResponseEntity<BulletinBoardResponse> updateBulletinBoard(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId,
      @RequestBody String newContent) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (roomId == null || newContent == null || newContent.trim().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    Integer authorId = tokenService.decodeToken(authToken).userId();
    BulletinBoardResponse response =
        bulletinBoardService.updateBulletinBoard(roomId, newContent, authorId);
    return ResponseEntity.ok(response);
  }

  @Override
  @DeleteMapping("/{roomId}/bulletin")
  public ResponseEntity<Void> deleteBulletinBoard(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (roomId == null) {
      return ResponseEntity.badRequest().build();
    }
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    bulletinBoardService.deleteBulletinBoard(roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping
  public ResponseEntity<List<RoomSummaryResponse>> getRooms(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @ModelAttribute RoomFilter filter,
      @RequestParam(name = "sortBy", required = false) RoomSort sortBy) {
    String authToken = extractToken(token);
    List<RoomSummaryResponse> rooms = roomService.getRooms(authToken, filter, sortBy);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDetailedResponse> getRoomById(@PathVariable Integer roomId,
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    RoomDetailedResponse response = roomService.getRoomById(roomId, authToken);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/me")
  public ResponseEntity<List<RoomDetailedResponse>> getUserRooms(
      @RequestHeader(name = "Authorization", required = false) String token) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<RoomDetailedResponse> rooms = userWithRoomService.getUserRooms(authToken);
    return ResponseEntity.ok(rooms);
  }

  @Override
  @PostMapping("/{roomId}/join")
  public ResponseEntity<Void> joinRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userWithRoomService.joinRoom(authToken, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{roomId}/leave")
  public ResponseEntity<Void> leaveRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    userWithRoomService.leaveRoom(authToken, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId) {
    String authToken = extractToken(token);
    if (roomId == null) {
      return ResponseEntity.badRequest().build();
    }
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    roomService.deleteRoom(authToken, roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{roomId}/participants")
  public ResponseEntity<List<RoomParticipantResponse>> getRoomParticipants(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId,
      @RequestParam(name = "role", required = false) Role roleFilter) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<RoomParticipantResponse> participants =
        userWithRoomService.getRoomParticipants(authToken, roomId, roleFilter);
    return ResponseEntity.ok(participants);
  }

  @Override
  @GetMapping("/{roomId}/participants/{userId}")
  public ResponseEntity<MembershipVerificationResponse> isUserInRoom(
      @RequestHeader(name = "Authorization", required = false) String token,
      @PathVariable Integer roomId,
      @PathVariable Integer userId) {
    String authToken = extractToken(token);
    if (isInvalidToken(authToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    MembershipVerificationResponse response =
        userWithRoomService.verifyUserMembership(authToken, roomId, userId);
    return ResponseEntity.ok(response);
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
