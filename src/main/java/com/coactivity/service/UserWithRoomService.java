package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Picture;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.PictureRepositoryImpl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserWithRoomService {

  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;
  private final RoomsRequestRepositoryImpl roomsRequestRepository;
  private final TokenService tokenService;
  private final PictureRepositoryImpl pictureRepository;
  private final BulletinBoardRepositoryImpl bulletinBoardRepository;

  public UserWithRoomService(UserRepositoryImpl userRepository,
      RoomRepositoryImpl roomRepository,
      RoomsRequestRepositoryImpl roomsRequestRepository,
      TokenService tokenService,
      PictureRepositoryImpl pictureRepository,
      BulletinBoardRepositoryImpl bulletinBoardRepository) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.tokenService = tokenService;
    this.pictureRepository = pictureRepository;
    this.bulletinBoardRepository = bulletinBoardRepository;
  }

  public ApiResponse<Void> assignAdminRole(String token, Integer roomId,
                                           Integer userId) {

    Integer roomOwnerId = tokenService.decodeToken(token).userId();
    try {
      if (!roomRepository.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      roomRepository.setRoleByUserIdAndRoomId(userId, roomId, Role.ADMIN);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<Void> demoteAdminRole(String token, Integer roomId,
                                           Integer userId) {

    Integer roomOwnerId = tokenService.decodeToken(token).userId();
    try {
      if (!roomRepository.isUserOwnerOfRoom(roomOwnerId, roomId)) {
        return ApiResponse.error(null);
      }

      roomRepository.setRoleByUserIdAndRoomId(userId, roomId, Role.PARTICIPANT);
      return ApiResponse.success(null);

    } catch (Exception e) {
      return ApiResponse.error("400");
    }
  }

  public ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      Integer userId = tokenService.decodeToken(token).userId();
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }

      User user = userRepository.getUserById(userId);
      boolean isMember = isUserParticipant(room, userId);
      Role role = isMember ? roomRepository.getUserRoleByRoomId(roomId, userId) : null;

      MembershipVerificationResponse response = new MembershipVerificationResponse(
          isMember,
          role,
          mapUserToSummaryResponse(user),
          room.getName()
      );
      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<MembershipVerificationResponse> verifyUserMembership(String token,
      Integer roomId, Integer targetUserId) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (roomId == null || targetUserId == null) {
      return ApiResponse.error("400");
    }

    try {
      Integer requesterId = tokenService.decodeToken(token).userId();
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }
      User targetUser = userRepository.getUserById(targetUserId);
      if (targetUser == null) {
        return ApiResponse.error("404");
      }
      if (!isUserParticipant(room, requesterId)) {
        return ApiResponse.error("403");
      }
      Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, requesterId);
      if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
        return ApiResponse.error("403");
      }

      boolean isMember = isUserParticipant(room, targetUserId);
      Role memberRole = isMember ? roomRepository.getUserRoleByRoomId(roomId, targetUserId) : null;

      MembershipVerificationResponse response = new MembershipVerificationResponse(
          isMember,
          memberRole,
          mapUserToSummaryResponse(targetUser),
          room.getName()
      );
      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  /**
   * Handles room join logic for both public and private rooms.
   * <p>
   * - For public rooms: user is added immediately as PARTICIPANT (if not banned and capacity not exceeded).<br>
   * - For private rooms: a join request with status {@link RequestStatus#CONSIDERATION} is created.
   * </p>
   */
  public ApiResponse<Void> joinRoom(String token, Integer roomId) {
    if (token == null || roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      if (!tokenService.isTokenActive(token)) {
        return ApiResponse.error("401");
      }

      Integer userId = tokenService.decodeToken(token).userId();
      User user = userRepository.getUserById(userId);
      Room room = roomRepository.getRoomById(roomId);

      if (user == null || room == null) {
        return ApiResponse.error("404");
      }

      // Check if user is banned in this room
      if (room.getBans() != null && room.getBans().contains(user)) {
        return ApiResponse.error("403");
      }

      // Check if user is already a member
      if (roomRepository.isUserInMembers(roomId, userId)) {
        return ApiResponse.success(null);
      }

      // Capacity check
      int currentParticipants =
          room.getUsers() != null ? room.getUsers().size() : 0;
      if (currentParticipants >= room.getMaximumNumberOfPeople()) {
        return ApiResponse.error("409"); // capacity exceeded
      }

      if (room.isPublic()) {
        // Public room – add immediately as PARTICIPANT
        roomRepository.addUserToRoom(roomId, userId, Role.PARTICIPANT);
        return ApiResponse.success(null);
      } else {
        // Private room – create join request in CONSIDERATION state
        roomsRequestRepository.createRequest(userId, roomId, RequestStatus.CONSIDERATION);
        return ApiResponse.success(null);
      }

    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<List<RoomSummaryResponse>> getBanRooms(String token) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    try {
      Integer userId = tokenService.decodeToken(token).userId();
      List<Room> rooms = roomRepository.getAllRooms();
      if (rooms == null || rooms.isEmpty()) {
        return ApiResponse.success(Collections.emptyList());
      }

      List<RoomSummaryResponse> bannedRooms = rooms.stream()
          .filter(room -> room.getBans() != null
              && room.getBans().stream().anyMatch(banned -> banned.getId().equals(userId)))
          .map(room -> mapRoomToSummaryResponse(room, userId))
          .collect(Collectors.toList());

      return ApiResponse.success(bannedRooms);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<List<RoomDetailedResponse>> getUserRooms(String token) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    try {
      Integer userId = tokenService.decodeToken(token).userId();
      User user = userRepository.getUserById(userId);
      if (user == null) {
        return ApiResponse.error("404");
      }

      List<Room> rooms = user.getRooms();
      if (rooms == null || rooms.isEmpty()) {
        return ApiResponse.success(Collections.emptyList());
      }

      List<RoomDetailedResponse> responses = rooms.stream()
          .map(room -> mapRoomToDetailedResponse(room, userId, true))
          .collect(Collectors.toList());

      return ApiResponse.success(responses);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<Void> leaveRoom(String token, Integer roomId) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      Integer userId = tokenService.decodeToken(token).userId();
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }

      if (!isUserParticipant(room, userId)) {
        return ApiResponse.error("404");
      }

      Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
      if (role == Role.OWNER) {
        return ApiResponse.error("403");
      }

      roomRepository.removeUserFromRoom(roomId, userId);
      return ApiResponse.success(null);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<List<RoomParticipantResponse>> getRoomParticipants(String token, Integer roomId,
      Role roleFilter) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }
    if (roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      Integer requesterId = tokenService.decodeToken(token).userId();
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }

      Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, requesterId);
      if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
        return ApiResponse.error("403");
      }

      Map<User, Role> users = room.getUsers();
      if (users == null || users.isEmpty()) {
        return ApiResponse.success(Collections.emptyList());
      }

      List<RoomParticipantResponse> participants = users.entrySet().stream()
          .filter(entry -> roleFilter == null || entry.getValue() == roleFilter)
          .map(entry -> mapParticipant(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList());

      return ApiResponse.success(participants);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  private RoomParticipantResponse mapParticipant(User user, Role role) {
    return new RoomParticipantResponse(
        user.getId(),
        user.getUserName(),
        user.getDataOfBirth(),
        user.getCity(),
        user.getCountry(),
        user.getAvatarId(),
        user.getDescription(),
        role
    );
  }

  private RoomDetailedResponse mapRoomToDetailedResponse(Room room, Integer currentUserId,
      boolean hasProtectedAccess) {
    RoomSummaryResponse summary = mapRoomToSummaryResponse(room, currentUserId);
    RoomDetailedResponse detailed = new RoomDetailedResponse();
    copySummary(summary, detailed);
    detailed.setHasProtectedAccess(hasProtectedAccess);
    if (hasProtectedAccess) {
      detailed.setChatLink(room.getChatLink());
      BulletinBoard board = bulletinBoardRepository.getBulletinBoard(room.getId());
      detailed.setBulletinBoard(board != null ? mapBulletinBoardToResponse(board) : null);
    }
    return detailed;
  }

  private void copySummary(RoomSummaryResponse source, RoomDetailedResponse target) {
    target.setId(source.getId());
    target.setActive(source.isActive());
    target.setIsPublic(source.getIsPublic());
    target.setCategory(source.getCategory());
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setDateOfStartEvent(source.getDateOfStartEvent());
    target.setDateOfEndEvent(source.getDateOfEndEvent());
    target.setAgeRating(source.getAgeRating());
    target.setFrequency(source.getFrequency());
    target.setParticipantCount(source.getParticipantCount());
    target.setMaximumParticipants(source.getMaximumParticipants());
    target.setCreator(source.getCreator());
    target.setIsCurrentUserParticipant(source.getIsCurrentUserParticipant());
    target.setImageIds(source.getImageIds());
  }

  private RoomSummaryResponse mapRoomToSummaryResponse(Room room, Integer currentUserId) {
    RoomSummaryResponse response = new RoomSummaryResponse();
    response.setId(room.getId());
    response.setActive(room.isActive());
    response.setIsPublic(room.isPublic());
    response.setCategory(room.getCategory());
    response.setName(room.getName());
    response.setDescription(room.getDescription());
    response.setDateOfStartEvent(room.getDateOfStartEvent());
    response.setDateOfEndEvent(room.getDateOfEndEvent());
    response.setAgeRating(room.getAgeRating());
    response.setFrequency(room.getFrequency());

    Map<User, Role> users = room.getUsers();
    int participantCount = users != null ? users.size() : 0;
    response.setParticipantCount(participantCount);
    response.setMaximumParticipants(room.getMaximumNumberOfPeople());

    User owner = findOwner(users);
    if (owner != null) {
      response.setCreator(mapUserToSummaryResponse(owner));
    }

    response.setIsCurrentUserParticipant(
        currentUserId != null && isUserParticipant(room, currentUserId));
    response.setImageIds(loadImageIds(room.getId()));
    return response;
  }

  private User findOwner(Map<User, Role> users) {
    if (users == null) {
      return null;
    }
    return users.entrySet().stream()
        .filter(entry -> entry.getValue() == Role.OWNER)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private List<Integer> loadImageIds(Integer roomId) {
    try {
      List<Picture> pictures = pictureRepository.getRoomPictures(roomId);
      if (pictures == null) {
        return Collections.emptyList();
      }
      return pictures.stream()
          .map(Picture::getPhotoId)
          .collect(Collectors.toList());
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private BulletinBoardResponse mapBulletinBoardToResponse(BulletinBoard board) {
    return new BulletinBoardResponse(
        board.getId(),
        board.getContent(),
        mapUserToSummaryResponse(board.getAuthor()),
        board.getUpdatedAt()
    );
  }

  private UserSummaryResponse mapUserToSummaryResponse(User user) {
    if (user == null) {
      return null;
    }
    UserSummaryResponse response = new UserSummaryResponse();
    response.setId(user.getId());
    response.setUserName(user.getUserName());
    response.setDateOfBirth(user.getDataOfBirth());
    response.setCity(user.getCity());
    response.setCountry(user.getCountry());
    response.setDescription(user.getDescription());
    response.setAvatarId(user.getAvatarId());
    return response;
  }

  private boolean isUserParticipant(Room room, Integer userId) {
    Map<User, Role> users = room.getUsers();
    if (users == null) {
      return false;
    }
    return users.keySet().stream().anyMatch(participant -> participant.getId().equals(userId));
  }
}
