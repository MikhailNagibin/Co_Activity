package com.coactivity.service;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.util.AvatarUrlResolver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMembershipService {

  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final RoomsRequestRepository roomsRequestRepository;
  private final RoomImageService roomImageService;
  private final BulletinBoardRepository bulletinBoardRepository;
  private final NotificationService notificationService;

  public RoomMembershipService(UserRepository userRepository,
      RoomRepository roomRepository,
      RoomsRequestRepository roomsRequestRepository,
      RoomImageService roomImageService,
      BulletinBoardRepository bulletinBoardRepository,
      NotificationService notificationService) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.roomImageService = roomImageService;
    this.bulletinBoardRepository = bulletinBoardRepository;
    this.notificationService = notificationService;
  }

  public RoleAssignmentResponse assignAdminRole(Integer requesterId, Integer roomId,
      Integer targetUserId) {
    Integer ownerId = requireOwner(requesterId, roomId);
    getExistingUser(targetUserId);

    // Validate that the target user is a member of the room before assigning admin role
    if (!roomRepository.isUserInMembers(roomId, targetUserId)) {
      throw new ValidationException("Target user is not a member of the room and cannot be assigned admin role.");
    }
    Role previousRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
    if (previousRole == Role.OWNER) {
      throw new ValidationException("Room owner cannot be reassigned as admin");
    }
    if (previousRole == Role.ADMIN) {
      return new RoleAssignmentResponse(targetUserId, roomId, Role.ADMIN, previousRole, ownerId);
    }
    roomRepository.setRoleByUserIdAndRoomId(targetUserId, roomId, Role.ADMIN);
    return new RoleAssignmentResponse(targetUserId, roomId, Role.ADMIN, previousRole, ownerId);
  }

  public RoleAssignmentResponse demoteAdminRole(Integer requesterId, Integer roomId,
      Integer targetUserId) {
    Integer ownerId = requireOwner(requesterId, roomId);
    getExistingUser(targetUserId);
    if (!roomRepository.isUserInMembers(roomId, targetUserId)) {
      throw new ValidationException("Target user is not a member of the room and cannot be demoted.");
    }

    Role previousRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
    if (previousRole != Role.ADMIN) {
      throw new ValidationException("Only admin users can be demoted");
    }
    roomRepository.setRoleByUserIdAndRoomId(targetUserId, roomId, Role.PARTICIPANT);
    return new RoleAssignmentResponse(targetUserId, roomId, Role.PARTICIPANT, previousRole, ownerId);
  }

  public boolean isUserInRoom(Integer requesterId, Integer roomId) {
    Integer effectiveRequesterId = requireUserId(requesterId);
    validateRoomId(roomId);
    getExistingRoom(roomId);
    return roomRepository.isUserInMembers(roomId, effectiveRequesterId);
  }

  public MembershipVerificationResponse verifyUserMembership(Integer requesterId,
      Integer roomId, Integer targetUserId) {
    validateRoomId(roomId);
    Integer adminId = requireRoomParticipant(requesterId, roomId);
    enforceAdminPrivileges(roomId, adminId);

    User targetUser = getExistingUser(targetUserId);
    boolean isMember = roomRepository.isUserInMembers(roomId, targetUserId);
    Role memberRole = isMember ? roomRepository.getUserRoleByRoomId(roomId, targetUserId) : null;

    Room room = getExistingRoom(roomId);
    return new MembershipVerificationResponse(isMember, memberRole,
        mapUserToSummaryResponse(targetUser), room.getName());
  }

  @Transactional
  public void joinRoom(Integer userId, Integer roomId) {
    validateRoomId(roomId);
    User user = getExistingUser(userId);
    Room room = getExistingRoomForUpdate(roomId);
    RoomsRequest existingRequest = roomsRequestRepository.getRequestByUserAndRoom(userId, roomId);

    enforceJoinEligibility(user, room);

    if (roomRepository.isUserInMembers(roomId, userId)) {
      if (room.isPublic() && existingRequest != null
          && existingRequest.getStatus() == RequestStatus.CONSIDERATION) {
        roomsRequestRepository.updateRequest(existingRequest.getId(), RequestStatus.ACCEPTED);
      }
      return;
    }

    if (room.isPublic()) {
      roomRepository.addUserToRoom(roomId, userId, Role.PARTICIPANT);
      if (existingRequest != null && existingRequest.getStatus() == RequestStatus.CONSIDERATION) {
        roomsRequestRepository.updateRequest(existingRequest.getId(), RequestStatus.ACCEPTED);
      }
    } else {
      if (existingRequest != null) {
        if (existingRequest.getStatus() == RequestStatus.CONSIDERATION) {
          return;
        }
        roomsRequestRepository.updateRequest(existingRequest.getId(), RequestStatus.CONSIDERATION);
      } else {
        roomsRequestRepository.createRequest(userId, roomId, RequestStatus.CONSIDERATION);
      }

      // Notify all admins and owner about the new join request
      Map<User, Role> roomUsers = roomRepository.getUsersInRoom(roomId);
      if (roomUsers != null) {
        for (Entry<User, Role> entry : roomUsers.entrySet()) {
          User roomUser = entry.getKey();
          Role role = entry.getValue();
          if (roomUser != null && roomUser.getId() != null
              && (role == Role.OWNER || role == Role.ADMIN)) {
            notificationService.sendNewJoinRequest(roomUser.getId(), room.getName(),
                user.getUserName());
          }
        }
      }
    }
  }

  public List<RoomSummaryResponse> getBanRooms(Integer userId) {
    Integer requesterId = requireUserId(userId);
    List<Room> rooms = roomRepository.getAllRooms();
    if (rooms == null || rooms.isEmpty()) {
      return Collections.emptyList();
    }

    return rooms.stream()
        .filter(room -> roomRepository.isUserBannedInRoom(room.getId(), requesterId))
        .map(room -> mapRoomToSummaryResponse(room, requesterId))
        .collect(Collectors.toList());
  }

  public List<RoomDetailedResponse> getUserRooms(Integer userId) {
    User user = getExistingUser(userId);
    List<Room> rooms = user.getRooms();
    if (rooms == null || rooms.isEmpty()) {
      return Collections.emptyList();
    }

    return rooms.stream()
        .map(room -> mapRoomToDetailedResponse(room, userId, true))
        .collect(Collectors.toList());
  }

  public void leaveRoom(Integer userId, Integer roomId) {
    validateRoomId(roomId);
    Room room = getExistingRoom(roomId);
    ensureParticipant(roomId, userId);

    Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
    if (role == Role.OWNER) {
      throw new AuthorizationException("Room owner cannot leave the room");
    }

    roomRepository.removeUserFromRoom(roomId, userId);
  }

  public List<RoomParticipantResponse> getRoomParticipants(Integer requesterId, Integer roomId,
      Role roleFilter) {
    validateRoomId(roomId);
    Integer adminId = requireRoomParticipant(requesterId, roomId);
    enforceAdminPrivileges(roomId, adminId);

    Room room = getExistingRoom(roomId);
    Map<User, Role> users = roomRepository.getUsersInRoom(roomId);
    if (users == null || users.isEmpty()) {
      return Collections.emptyList();
    }

    return users.entrySet().stream()
        .filter(entry -> roleFilter == null || entry.getValue() == roleFilter)
        .map(entry -> mapParticipant(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Integer requireOwner(Integer requesterId, Integer roomId) {
    Integer ownerId = requireRoomParticipant(requesterId, roomId);
    Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, ownerId);
    if (requesterRole != Role.OWNER) {
      throw new AuthorizationException("Only room owner can perform this action");
    }
    return ownerId;
  }

  private Integer requireRoomParticipant(Integer requesterId, Integer roomId) {
    validateRoomId(roomId);
    Integer userId = requireUserId(requesterId);
    if (!roomRepository.isUserInMembers(roomId, userId)) {
      throw new AuthorizationException("User is not a participant of room " + roomId);
    }
    return userId;
  }

  private void enforceAdminPrivileges(Integer roomId, Integer participantId) {
    Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, participantId);
    if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
      throw new AuthorizationException("Insufficient privileges for room " + roomId);
    }
  }

  private void enforceJoinEligibility(User user, Room room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw new ValidationException("Only active rooms can accept new participants");
    }
    if (roomRepository.isUserBannedInRoom(room.getId(), user.getId())) {
      throw new AuthorizationException("User is banned from this room");
    }

    int currentParticipants = roomRepository.getRoomParticipantCount(room.getId());
    if (currentParticipants >= room.getMaximumNumberOfPeople()) {
      throw new ValidationException("Room capacity exceeded");
    }
  }

  private void ensureParticipant(Integer roomId, Integer userId) {
    if (!roomRepository.isUserInMembers(roomId, userId)) {
      throw new ResourceNotFoundException("User " + userId + " is not in room " + roomId);
    }
  }

  private void validateRoomId(Integer roomId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
  }

  private Integer requireUserId(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User id is required");
    }
    return userId;
  }

  private Room getExistingRoom(Integer roomId) {
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found: " + roomId);
    }
    return room;
  }

  private Room getExistingRoomForUpdate(Integer roomId) {
    Room room = roomRepository.getRoomByIdForUpdate(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found: " + roomId);
    }
    return room;
  }

  private User getExistingUser(Integer userId) {
    requireUserId(userId);
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }
    return user;
  }

  private RoomParticipantResponse mapParticipant(User user, Role role) {
    return new RoomParticipantResponse(
        user.getId(),
        user.getUserName(),
        user.getDataOfBirth(),
        user.getCity(),
        user.getCountry(),
        user.getAvatarId(),
        AvatarUrlResolver.resolveUserAvatarUrl(user.getId(), user.getAvatarFileId()),
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
    target.setStatus(source.getStatus());
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
    target.setImages(source.getImages());
  }

  private RoomSummaryResponse mapRoomToSummaryResponse(Room room, Integer currentUserId) {
    RoomSummaryResponse response = new RoomSummaryResponse();
    response.setId(room.getId());
    response.setStatus(room.getStatus());
    response.setIsPublic(room.isPublic());
    response.setCategory(room.getCategory());
    response.setName(room.getName());
    response.setDescription(room.getDescription());
    response.setDateOfStartEvent(room.getDateOfStartEvent());
    response.setDateOfEndEvent(room.getDateOfEndEvent());
    response.setAgeRating(room.getAgeRating());
    response.setFrequency(room.getFrequency());

    Map<User, Role> users = loadRoomUsers(room);
    int participantCount = users.size();
    response.setParticipantCount(participantCount);
    response.setMaximumParticipants(room.getMaximumNumberOfPeople());

    User owner = findOwner(users);
    if (owner != null) {
      response.setCreator(mapUserToSummaryResponse(owner));
    }

    response.setIsCurrentUserParticipant(
        currentUserId != null && roomRepository.isUserInMembers(room.getId(), currentUserId));
    populateRoomImages(response, room.getId());
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

  private void populateRoomImages(RoomSummaryResponse response, Integer roomId) {
    List<RoomImageResponse> images = roomImageService.listRoomImages(roomId);
    response.setImages(images);
    response.setImageIds(images.stream().map(RoomImageResponse::getId).toList());
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
    response.setAvatarUrl(AvatarUrlResolver.resolveUserAvatarUrl(user.getId(), user.getAvatarFileId()));
    return response;
  }

  private Map<User, Role> loadRoomUsers(Room room) {
    if (room.getUsers() != null) {
      return room.getUsers();
    }

    Map<User, Role> users = roomRepository.getUsersInRoom(room.getId());
    return users != null ? users : Collections.emptyMap();
  }
}
