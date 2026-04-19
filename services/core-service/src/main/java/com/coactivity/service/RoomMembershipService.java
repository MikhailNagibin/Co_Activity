package com.coactivity.service;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.OwnershipTransferResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomMembershipStatusResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomMembershipStatus;
import com.coactivity.domain.RoomStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.RoomInvitationRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.event.JoinRequestDecisionEvent;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.util.AvatarUrlResolver;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMembershipService {

  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final RoomInvitationRepository roomInvitationRepository;
  private final RoomsRequestRepository roomsRequestRepository;
  private final RoomImageService roomImageService;
  private final BulletinBoardRepository bulletinBoardRepository;
  private final NotificationService notificationService;
  private final ApplicationEventPublisher applicationEventPublisher;

  public RoomMembershipService(UserRepository userRepository,
      RoomRepository roomRepository,
      RoomInvitationRepository roomInvitationRepository,
      RoomsRequestRepository roomsRequestRepository,
      RoomImageService roomImageService,
      BulletinBoardRepository bulletinBoardRepository,
      NotificationService notificationService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.roomInvitationRepository = roomInvitationRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.roomImageService = roomImageService;
    this.bulletinBoardRepository = bulletinBoardRepository;
    this.notificationService = notificationService;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public RoleAssignmentResponse assignAdminRole(Integer requesterId, Integer roomId,
      Integer targetUserId) {
    Room room = getExistingRoom(roomId);
    Integer ownerId = requireOwner(requesterId, room);
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
    Room room = getExistingRoom(roomId);
    Integer ownerId = requireOwner(requesterId, room);
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

  public RoomMembershipStatusResponse getCurrentUserMembershipStatus(Integer requesterId,
      Integer roomId) {
    Integer userId = requireUserId(requesterId);
    validateRoomId(roomId);
    Room room = getExistingRoom(roomId);
    getExistingUser(userId);
    return buildMembershipStatus(userId, room);
  }

  public MembershipVerificationResponse verifyUserMembership(Integer requesterId,
      Integer roomId, Integer targetUserId) {
    validateRoomId(roomId);
    Room room = getExistingRoom(roomId);
    Integer adminId = requireRoomParticipant(requesterId, room);
    enforceAdminPrivileges(room, adminId);

    User targetUser = getExistingUser(targetUserId);
    boolean isMember = roomRepository.isUserInMembers(roomId, targetUserId);
    Role memberRole = isMember ? roomRepository.getUserRoleByRoomId(roomId, targetUserId) : null;

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
      if (roomInvitationRepository.exists(roomId, userId)) {
        roomInvitationRepository.delete(roomId, userId);
      }
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
      if (roomInvitationRepository.exists(roomId, userId)) {
        roomRepository.addUserToRoom(roomId, userId, Role.PARTICIPANT);
        if (existingRequest != null && existingRequest.getStatus() == RequestStatus.CONSIDERATION) {
          roomsRequestRepository.updateRequest(existingRequest.getId(), RequestStatus.ACCEPTED);
        }
        roomInvitationRepository.delete(roomId, userId);
        return;
      }

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
    ensureParticipant(room, userId);

    Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
    if (role == Role.OWNER) {
      throw new AuthorizationException("Room owner cannot leave the room");
    }

    roomRepository.removeUserFromRoom(roomId, userId);
  }

  @Transactional
  public void removeParticipant(Integer requesterId, Integer roomId, Integer targetUserId) {
    validateRoomId(roomId);
    Room room = getExistingRoomForUpdate(roomId);
    requireUserId(targetUserId);
    Role requesterRole = requireGovernanceRole(requesterId, room);

    ensureParticipant(room, targetUserId);
    Role targetRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
    enforceRemovalAccess(requesterRole, targetRole);

    roomRepository.removeUserFromRoom(roomId, targetUserId);
  }

  @Transactional
  public void banUser(Integer requesterId, Integer roomId, Integer targetUserId) {
    validateRoomId(roomId);
    Room room = getExistingRoomForUpdate(roomId);
    requireUserId(targetUserId);
    getExistingUser(targetUserId);
    Role requesterRole = requireGovernanceRole(requesterId, room);

    boolean targetIsMember = roomRepository.isUserInMembers(roomId, targetUserId);
    if (targetIsMember) {
      Role targetRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
      enforceBanAccess(requesterRole, targetRole);
      roomRepository.removeUserFromRoom(roomId, targetUserId);
    }

    roomRepository.addUserBan(roomId, targetUserId);
    publishBanDecisionIfPendingRequestClosed(room, targetUserId);
  }

  public List<UserSummaryResponse> getBannedUsers(Integer requesterId, Integer roomId) {
    validateRoomId(roomId);
    Room room = getExistingRoom(roomId);
    requireGovernanceRole(requesterId, room);

    List<User> bannedUsers = roomRepository.getBannedUsers(room.getId());
    if (bannedUsers == null || bannedUsers.isEmpty()) {
      return Collections.emptyList();
    }

    return bannedUsers.stream()
        .map(this::mapUserToSummaryResponse)
        .sorted(Comparator
            .comparing((UserSummaryResponse user) ->
                user.getUserName() != null ? user.getUserName().toLowerCase() : "")
            .thenComparing(UserSummaryResponse::getId, Comparator.nullsLast(Integer::compareTo)))
        .toList();
  }

  @Transactional
  public void unbanUser(Integer requesterId, Integer roomId, Integer targetUserId) {
    validateRoomId(roomId);
    Room room = getExistingRoomForUpdate(roomId);
    requireUserId(targetUserId);
    requireGovernanceRole(requesterId, room);
    getExistingUser(targetUserId);

    if (!roomRepository.isUserBannedInRoom(roomId, targetUserId)) {
      throw new ResourceNotFoundException(
          "ROOM_BAN_NOT_FOUND",
          "User " + targetUserId + " is not banned in room " + roomId);
    }

    roomRepository.removeUserBan(roomId, targetUserId);
  }

  @Transactional
  public OwnershipTransferResponse transferOwnership(Integer requesterId, Integer roomId,
      Integer targetUserId) {
    validateRoomId(roomId);
    Room room = getExistingRoomForUpdate(roomId);
    Integer ownerId = requireOwner(requesterId, room);
    Integer effectiveTargetUserId = requireUserId(targetUserId);

    if (effectiveTargetUserId.equals(ownerId)) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership cannot be transferred to the current owner");
    }
    if (!roomRepository.isUserInMembers(roomId, effectiveTargetUserId)) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership can only be transferred to an existing room participant");
    }

    Role targetRole = roomRepository.getUserRoleByRoomId(roomId, effectiveTargetUserId);
    if (targetRole != Role.ADMIN && targetRole != Role.PARTICIPANT) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership can only be transferred to a room admin or participant");
    }

    roomRepository.setRoleByUserIdAndRoomId(effectiveTargetUserId, roomId, Role.OWNER);
    roomRepository.setRoleByUserIdAndRoomId(ownerId, roomId, Role.PARTICIPANT);
    return new OwnershipTransferResponse(roomId, ownerId, effectiveTargetUserId,
        Role.PARTICIPANT, Role.OWNER);
  }

  public List<RoomParticipantResponse> getRoomParticipants(Integer requesterId, Integer roomId,
      Role roleFilter) {
    validateRoomId(roomId);
    Room room = getExistingRoom(roomId);
    requireGovernanceRole(requesterId, room);
    Map<User, Role> users = roomRepository.getUsersInRoom(roomId);
    if (users == null || users.isEmpty()) {
      return Collections.emptyList();
    }

    return users.entrySet().stream()
        .filter(entry -> roleFilter == null || entry.getValue() == roleFilter)
        .map(entry -> mapParticipant(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Integer requireOwner(Integer requesterId, Room room) {
    Integer ownerId = requireRoomParticipant(requesterId, room);
    Role requesterRole = roomRepository.getUserRoleByRoomId(room.getId(), ownerId);
    if (requesterRole != Role.OWNER) {
      throw new AuthorizationException("Only room owner can perform this action");
    }
    return ownerId;
  }

  private Integer requireRoomParticipant(Integer requesterId, Room room) {
    Integer userId = requireUserId(requesterId);
    if (!roomRepository.isUserInMembers(room.getId(), userId)) {
      throw new AuthorizationException("User is not a participant of room " + room.getId());
    }
    return userId;
  }

  private void enforceAdminPrivileges(Room room, Integer participantId) {
    Role requesterRole = roomRepository.getUserRoleByRoomId(room.getId(), participantId);
    if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
      throw new AuthorizationException("Insufficient privileges for room " + room.getId());
    }
  }

  private Role requireGovernanceRole(Integer requesterId, Room room) {
    Integer participantId = requireRoomParticipant(requesterId, room);
    enforceAdminPrivileges(room, participantId);
    return roomRepository.getUserRoleByRoomId(room.getId(), participantId);
  }

  private void enforceRemovalAccess(Role requesterRole, Role targetRole) {
    if (targetRole == Role.OWNER) {
      throw new ValidationException("Room owner cannot be removed");
    }
    if (requesterRole == Role.ADMIN && targetRole != Role.PARTICIPANT) {
      throw new AuthorizationException("Admins can remove only ordinary participants");
    }
  }

  private void enforceBanAccess(Role requesterRole, Role targetRole) {
    if (targetRole == Role.OWNER) {
      throw new ValidationException("Room owner cannot be banned");
    }
    if (requesterRole == Role.ADMIN && targetRole != Role.PARTICIPANT) {
      throw new AuthorizationException("Admins can ban only ordinary participants");
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

  private void ensureParticipant(Room room, Integer userId) {
    if (!roomRepository.isUserInMembers(room.getId(), userId)) {
      throw new ResourceNotFoundException("ROOM_MEMBERSHIP_NOT_FOUND",
          "User " + userId + " is not in room " + room.getId());
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
      throw new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found: " + roomId);
    }
    return room;
  }

  private Room getExistingRoomForUpdate(Integer roomId) {
    Room room = roomRepository.getRoomByIdForUpdate(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found: " + roomId);
    }
    return room;
  }

  private User getExistingUser(Integer userId) {
    requireUserId(userId);
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + userId);
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
    target.setCity(source.getCity());
    target.setCountry(source.getCountry());
    target.setDateOfStartEvent(source.getDateOfStartEvent());
    target.setDateOfEndEvent(source.getDateOfEndEvent());
    target.setAgeRating(source.getAgeRating());
    target.setFrequency(source.getFrequency());
    target.setParticipantCount(source.getParticipantCount());
    target.setMaximumParticipants(source.getMaximumParticipants());
    target.setCreator(source.getCreator());
    target.setIsCurrentUserParticipant(source.getIsCurrentUserParticipant());
    target.setMembershipStatus(source.getMembershipStatus());
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
    response.setCity(room.getCity());
    response.setCountry(room.getCountry());
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

    if (currentUserId != null) {
      RoomMembershipStatusResponse status = buildMembershipStatus(currentUserId, room);
      response.setMembershipStatus(status);
      response.setIsCurrentUserParticipant(status.getStatus() == RoomMembershipStatus.PARTICIPANT);
    }
    populateRoomImages(response, room.getId());
    return response;
  }

  private RoomMembershipStatusResponse buildMembershipStatus(Integer userId, Room room) {
    Integer roomId = room.getId();
    if (roomRepository.isUserInMembers(roomId, userId)) {
      Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
      return new RoomMembershipStatusResponse(roomId, userId, RoomMembershipStatus.PARTICIPANT,
          role, null, false);
    }
    if (roomRepository.isUserBannedInRoom(roomId, userId)) {
      return new RoomMembershipStatusResponse(roomId, userId, RoomMembershipStatus.BANNED,
          null, null, false);
    }

    RoomsRequest request = roomsRequestRepository.getRequestByUserAndRoom(userId, roomId);
    if (request != null && request.getStatus() == RequestStatus.CONSIDERATION) {
      return new RoomMembershipStatusResponse(roomId, userId, RoomMembershipStatus.PENDING,
          null, request.getId(), false);
    }

    boolean canJoin = room.getStatus() == RoomStatus.ACTIVE;
    return new RoomMembershipStatusResponse(roomId, userId, RoomMembershipStatus.NOT_JOINED,
        null, null, canJoin);
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

  private void publishBanDecisionIfPendingRequestClosed(Room room, Integer targetUserId) {
    RoomsRequest existingRequest = roomsRequestRepository.getRequestByUserAndRoom(targetUserId,
        room.getId());
    if (existingRequest != null && existingRequest.getStatus() == RequestStatus.CONSIDERATION) {
      roomsRequestRepository.updateRequest(existingRequest.getId(), RequestStatus.REFUSED_WITH_BAN);
      applicationEventPublisher.publishEvent(
          new JoinRequestDecisionEvent(targetUserId, room.getName(), RequestStatus.REFUSED_WITH_BAN));
    }
  }
}
