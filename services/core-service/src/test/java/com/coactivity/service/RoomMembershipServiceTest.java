package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
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
import com.coactivity.service.event.JoinRequestDecisionEvent;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("RoomMembershipService Tests")
class RoomMembershipServiceTest {

  private UserRepository userRepository;
  private RoomRepository roomRepository;
  private RoomsRequestRepository roomsRequestRepository;
  private RoomImageService roomImageService;
  private BulletinBoardRepository bulletinBoardRepository;
  private NotificationService notificationService;
  private ApplicationEventPublisher applicationEventPublisher;
  private RoomMembershipService roomMembershipService;

  private Integer userId;
  private Integer roomId;
  private Room roomWithoutUsers;
  private Map<User, Role> roomUsers;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    roomRepository = Mockito.mock(RoomRepository.class);
    roomsRequestRepository = Mockito.mock(RoomsRequestRepository.class);
    roomImageService = Mockito.mock(RoomImageService.class);
    bulletinBoardRepository = Mockito.mock(BulletinBoardRepository.class);
    notificationService = Mockito.mock(NotificationService.class);
    applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);

    roomMembershipService = new RoomMembershipService(
        userRepository,
        roomRepository,
        roomsRequestRepository,
        roomImageService,
        bulletinBoardRepository,
        notificationService,
        applicationEventPublisher);
    when(roomImageService.listRoomImages(Mockito.anyInt())).thenReturn(List.of());

    userId = 7;
    roomId = 100;

    User owner = createUser(10, "owner@example.com", "owner");
    User participant = createUser(userId, "student@example.com", "student");

    roomUsers = new LinkedHashMap<>();
    roomUsers.put(owner, Role.OWNER);
    roomUsers.put(participant, Role.PARTICIPANT);

    roomWithoutUsers = createRoom(roomId, null);
  }

  @Test
  @DisplayName("leaveRoom should use repository membership check when room entity is not hydrated")
  void leaveRoom_usesRepositoryMembershipCheck() {
    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.PARTICIPANT);

    roomMembershipService.leaveRoom(userId, roomId);

    verify(roomRepository).removeUserFromRoom(roomId, userId);
  }

  @Test
  @DisplayName("getUserRooms should load creator and participant count from repository when room entity is not hydrated")
  void getUserRooms_loadsUsersFromRepositoryWhenRoomEntityHasNoUsers() {
    User currentUser = createUser(userId, "student@example.com", "student");
    currentUser.setRooms(List.of(roomWithoutUsers));

    when(userRepository.getUserById(userId)).thenReturn(currentUser);
    when(roomRepository.getUsersInRoom(roomId)).thenReturn(roomUsers);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);

    List<RoomDetailedResponse> responses = roomMembershipService.getUserRooms(userId);

    assertEquals(1, responses.size());
    RoomDetailedResponse response = responses.getFirst();
    assertNotNull(response.getCreator());
    assertEquals("owner", response.getCreator().getUserName());
    assertEquals(2, response.getParticipantCount());
  }

  @Test
  @DisplayName("getUserRooms should reject null user id")
  void getUserRooms_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.getUserRooms(null));

    assertEquals("User id is required", exception.getMessage());
    verifyNoInteractions(userRepository);
    verifyNoInteractions(roomRepository);
  }

  @Test
  @DisplayName("joinRoom should add user directly for public rooms")
  void joinRoom_addsUserDirectlyForPublicRoom() {
    User user = createUser(userId, "student@example.com", "student");
    Room publicRoom = createRoom(roomId, null);
    publicRoom.setPublic(true);

    when(userRepository.getUserById(userId)).thenReturn(user);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(publicRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);

    roomMembershipService.joinRoom(userId, roomId);

    verify(roomRepository).addUserToRoom(roomId, userId, Role.PARTICIPANT);
    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("joinRoom should reject null user id")
  void joinRoom_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.joinRoom(null, roomId));

    assertEquals("User id is required", exception.getMessage());
    verifyNoInteractions(userRepository);
    verify(roomRepository, never()).getRoomByIdForUpdate(roomId);
  }

  @Test
  @DisplayName("joinRoom should create request and notify owner and admins for private rooms")
  void joinRoom_createsRequestAndNotifiesModeratorsForPrivateRoom() {
    User requester = createUser(userId, "student@example.com", "student");
    User owner = createUser(10, "owner@example.com", "owner");
    User admin = createUser(11, "admin@example.com", "admin");
    User participant = createUser(12, "participant@example.com", "participant");

    Room privateRoom = createRoom(roomId, null);
    privateRoom.setPublic(false);
    Map<User, Role> users = new LinkedHashMap<>();
    users.put(owner, Role.OWNER);
    users.put(admin, Role.ADMIN);
    users.put(participant, Role.PARTICIPANT);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(privateRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomsRequestRepository.getRequestByUserAndRoom(userId, roomId)).thenReturn(null);
    when(roomRepository.getUsersInRoom(roomId)).thenReturn(users);

    roomMembershipService.joinRoom(userId, roomId);

    verify(roomsRequestRepository).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verify(notificationService).sendNewJoinRequest(10, "Morning Run", "student");
    verify(notificationService).sendNewJoinRequest(11, "Morning Run", "student");
    verify(notificationService, never()).sendNewJoinRequest(12, "Morning Run", "student");
    verify(roomRepository, never()).addUserToRoom(roomId, userId, Role.PARTICIPANT);
  }

  @Test
  @DisplayName("joinRoom should not create duplicate request for private room")
  void joinRoom_doesNothingWhenPendingRequestAlreadyExists() {
    User requester = createUser(userId, "student@example.com", "student");
    Room privateRoom = createRoom(roomId, null);
    privateRoom.setPublic(false);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(privateRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomsRequestRepository.getRequestByUserAndRoom(userId, roomId))
        .thenReturn(new RoomsRequest(55, requester, privateRoom, Instant.now(),
            RequestStatus.CONSIDERATION));

    roomMembershipService.joinRoom(userId, roomId);

    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("joinRoom should allow resubmitting private request after ordinary refusal")
  void joinRoom_allowsResubmittingAfterRefusal() {
    User requester = createUser(userId, "student@example.com", "student");
    User owner = createUser(10, "owner@example.com", "owner");
    User admin = createUser(11, "admin@example.com", "admin");

    Room privateRoom = createRoom(roomId, null);
    privateRoom.setPublic(false);
    Map<User, Role> users = new LinkedHashMap<>();
    users.put(owner, Role.OWNER);
    users.put(admin, Role.ADMIN);

    RoomsRequest refusedRequest = new RoomsRequest(55, requester, privateRoom, Instant.now(),
        RequestStatus.REFUSED);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(privateRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomsRequestRepository.getRequestByUserAndRoom(userId, roomId)).thenReturn(refusedRequest);
    when(roomRepository.getUsersInRoom(roomId)).thenReturn(users);

    roomMembershipService.joinRoom(userId, roomId);

    verify(roomsRequestRepository).updateRequest(55, RequestStatus.CONSIDERATION);
    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verify(notificationService).sendNewJoinRequest(10, "Morning Run", "student");
    verify(notificationService).sendNewJoinRequest(11, "Morning Run", "student");
  }

  @Test
  @DisplayName("joinRoom should reject banned user")
  void joinRoom_rejectsBannedUser() {
    User requester = createUser(userId, "student@example.com", "student");
    Room privateRoom = createRoom(roomId, null);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(privateRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(true);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> roomMembershipService.joinRoom(userId, roomId));

    assertEquals("User is banned from this room", exception.getMessage());
    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verify(roomRepository, never()).addUserToRoom(roomId, userId, Role.PARTICIPANT);
  }

  @Test
  @DisplayName("joinRoom should reject inactive room")
  void joinRoom_rejectsInactiveRoom() {
    User requester = createUser(userId, "student@example.com", "student");
    Room inactiveRoom = createRoom(roomId, null);
    inactiveRoom.setStatus(RoomStatus.INACTIVE);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(inactiveRoom);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.joinRoom(userId, roomId));

    assertEquals("Only active rooms can accept new participants", exception.getMessage());
    verify(roomRepository, never()).addUserToRoom(roomId, userId, Role.PARTICIPANT);
    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
  }

  @Test
  @DisplayName("joinRoom should accept stale pending request when room is already public")
  void joinRoom_acceptsStalePendingRequestForPublicRoom() {
    User requester = createUser(userId, "student@example.com", "student");
    Room publicRoom = createRoom(roomId, null);
    publicRoom.setPublic(true);
    RoomsRequest pendingRequest = new RoomsRequest(55, requester, publicRoom, Instant.now(),
        RequestStatus.CONSIDERATION);

    when(userRepository.getUserById(userId)).thenReturn(requester);
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(publicRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomsRequestRepository.getRequestByUserAndRoom(userId, roomId)).thenReturn(pendingRequest);

    roomMembershipService.joinRoom(userId, roomId);

    verify(roomRepository).addUserToRoom(roomId, userId, Role.PARTICIPANT);
    verify(roomsRequestRepository).updateRequest(55, RequestStatus.ACCEPTED);
  }

  @Test
  @DisplayName("leaveRoom should reject room owner")
  void leaveRoom_rejectsOwner() {
    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.OWNER);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> roomMembershipService.leaveRoom(userId, roomId));

    assertEquals("Room owner cannot leave the room", exception.getMessage());
    verify(roomRepository, never()).removeUserFromRoom(roomId, userId);
  }

  @Test
  @DisplayName("removeParticipant should allow admin to remove ordinary participant")
  void removeParticipant_adminRemovesOrdinaryParticipant() {
    Integer adminId = 10;
    Integer targetUserId = 12;

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.ADMIN);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.PARTICIPANT);

    roomMembershipService.removeParticipant(adminId, roomId, targetUserId);

    verify(roomRepository).removeUserFromRoom(roomId, targetUserId);
  }

  @Test
  @DisplayName("removeParticipant should reject admin removing another admin")
  void removeParticipant_adminCannotRemoveAdmin() {
    Integer adminId = 10;
    Integer targetUserId = 12;

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.ADMIN);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.ADMIN);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> roomMembershipService.removeParticipant(adminId, roomId, targetUserId));

    assertEquals("Admins can remove only ordinary participants", exception.getMessage());
    verify(roomRepository, never()).removeUserFromRoom(roomId, targetUserId);
  }

  @Test
  @DisplayName("banUser should reject banning room owner")
  void banUser_rejectsOwnerTarget() {
    Integer adminId = 10;
    Integer ownerId = 11;
    User owner = createUser(ownerId, "owner@example.com", "owner");

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.ADMIN);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.banUser(adminId, roomId, ownerId));

    assertEquals("Room owner cannot be banned", exception.getMessage());
    verify(roomRepository, never()).addUserBan(roomId, ownerId);
  }

  @Test
  @DisplayName("banUser should remove participant, create ban and close pending request")
  void banUser_removesParticipantAndClosesPendingRequest() {
    Integer ownerId = 10;
    Integer targetUserId = 12;
    User target = createUser(targetUserId, "target@example.com", "target");
    Room managedRoom = createRoom(roomId, null);
    RoomsRequest request = new RoomsRequest(55, target, managedRoom, Instant.now(),
        RequestStatus.CONSIDERATION);

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(managedRoom);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(userRepository.getUserById(targetUserId)).thenReturn(target);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.PARTICIPANT);
    when(roomsRequestRepository.getRequestByUserAndRoom(targetUserId, roomId)).thenReturn(request);

    roomMembershipService.banUser(ownerId, roomId, targetUserId);

    verify(roomRepository).removeUserFromRoom(roomId, targetUserId);
    verify(roomRepository).addUserBan(roomId, targetUserId);
    verify(roomsRequestRepository).updateRequest(55, RequestStatus.REFUSED_WITH_BAN);
    verify(applicationEventPublisher).publishEvent(
        new JoinRequestDecisionEvent(targetUserId, managedRoom.getName(), RequestStatus.REFUSED_WITH_BAN));
  }

  @Test
  @DisplayName("banUser should not publish rejection event when no pending request exists")
  void banUser_withoutPendingRequestSkipsEventPublication() {
    Integer ownerId = 10;
    Integer targetUserId = 12;
    User target = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(userRepository.getUserById(targetUserId)).thenReturn(target);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(false);
    when(roomsRequestRepository.getRequestByUserAndRoom(targetUserId, roomId)).thenReturn(null);

    roomMembershipService.banUser(ownerId, roomId, targetUserId);

    verify(roomRepository).addUserBan(roomId, targetUserId);
    verify(roomsRequestRepository, never()).updateRequest(Mockito.anyInt(), Mockito.any());
    verifyNoInteractions(applicationEventPublisher);
  }

  @Test
  @DisplayName("unbanUser should remove existing ban")
  void unbanUser_removesExistingBan() {
    Integer adminId = 10;
    Integer targetUserId = 12;
    User target = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.ADMIN);
    when(userRepository.getUserById(targetUserId)).thenReturn(target);
    when(roomRepository.isUserBannedInRoom(roomId, targetUserId)).thenReturn(true);

    roomMembershipService.unbanUser(adminId, roomId, targetUserId);

    verify(roomRepository).removeUserBan(roomId, targetUserId);
  }

  @Test
  @DisplayName("transferOwnership should reject outsider target")
  void transferOwnership_rejectsOutsider() {
    Integer ownerId = 10;
    Integer outsiderId = 12;

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, outsiderId)).thenReturn(false);

    var exception = assertThrows(com.coactivity.service.exception.ConflictException.class,
        () -> roomMembershipService.transferOwnership(ownerId, roomId, outsiderId));

    assertEquals("INVALID_OWNERSHIP_TRANSFER", exception.getCode());
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(outsiderId, roomId, Role.OWNER);
  }

  @Test
  @DisplayName("transferOwnership should keep former owner as participant")
  void transferOwnership_keepsFormerOwnerAsParticipant() {
    Integer ownerId = 10;
    Integer newOwnerId = 12;

    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, newOwnerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, newOwnerId)).thenReturn(Role.ADMIN);

    var response = roomMembershipService.transferOwnership(ownerId, roomId, newOwnerId);

    assertEquals(roomId, response.getRoomId());
    assertEquals(ownerId, response.getPreviousOwnerId());
    assertEquals(newOwnerId, response.getNewOwnerId());
    assertEquals(Role.PARTICIPANT, response.getPreviousOwnerNewRole());
    assertEquals(Role.OWNER, response.getNewOwnerRole());
    verify(roomRepository).setRoleByUserIdAndRoomId(newOwnerId, roomId, Role.OWNER);
    verify(roomRepository).setRoleByUserIdAndRoomId(ownerId, roomId, Role.PARTICIPANT);
  }

  @Test
  @DisplayName("removeParticipant should return room not found before authorization checks")
  void removeParticipant_returnsRoomNotFoundBeforeAuthorization() {
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> roomMembershipService.removeParticipant(userId, roomId, 12));

    assertEquals("Room not found: 100", exception.getMessage());
    verify(roomRepository, never()).isUserInMembers(roomId, userId);
  }

  @Test
  @DisplayName("banUser should return room not found before authorization checks")
  void banUser_returnsRoomNotFoundBeforeAuthorization() {
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> roomMembershipService.banUser(userId, roomId, 12));

    assertEquals("Room not found: 100", exception.getMessage());
    verify(roomRepository, never()).isUserInMembers(roomId, userId);
  }

  @Test
  @DisplayName("getBannedUsers should return room not found before authorization checks")
  void getBannedUsers_returnsRoomNotFoundBeforeAuthorization() {
    when(roomRepository.getRoomById(roomId)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> roomMembershipService.getBannedUsers(userId, roomId));

    assertEquals("Room not found: 100", exception.getMessage());
    verify(roomRepository, never()).isUserInMembers(roomId, userId);
  }

  @Test
  @DisplayName("unbanUser should return room not found before authorization checks")
  void unbanUser_returnsRoomNotFoundBeforeAuthorization() {
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> roomMembershipService.unbanUser(userId, roomId, 12));

    assertEquals("Room not found: 100", exception.getMessage());
    verify(roomRepository, never()).isUserInMembers(roomId, userId);
  }

  @Test
  @DisplayName("transferOwnership should return room not found before authorization checks")
  void transferOwnership_returnsRoomNotFoundBeforeAuthorization() {
    when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> roomMembershipService.transferOwnership(userId, roomId, 12));

    assertEquals("Room not found: 100", exception.getMessage());
    verify(roomRepository, never()).isUserInMembers(roomId, userId);
  }

  @Test
  @DisplayName("isUserInRoom should reject null user id")
  void isUserInRoom_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.isUserInRoom(null, roomId));

    assertEquals("User id is required", exception.getMessage());
    verify(roomRepository, never()).getRoomByIdForUpdate(roomId);
    verify(roomRepository, never()).isUserInMembers(roomId, null);
  }

  @Test
  @DisplayName("assignAdminRole should reject non-member target")
  void assignAdminRole_rejectsNonMemberTarget() {
    Integer ownerId = 10;
    Integer targetUserId = 99;
    User owner = createUser(ownerId, "owner@example.com", "owner");
    User targetUser = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(userRepository.getUserById(targetUserId)).thenReturn(targetUser);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(false);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.assignAdminRole(ownerId, roomId, targetUserId));

    assertEquals("Target user is not a member of the room and cannot be assigned admin role.",
        exception.getMessage());
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(targetUserId, roomId, Role.ADMIN);
  }

  @Test
  @DisplayName("assignAdminRole should reject owner target to preserve room ownership invariant")
  void assignAdminRole_rejectsOwnerTarget() {
    Integer ownerId = 10;
    User owner = createUser(ownerId, "owner@example.com", "owner");

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.assignAdminRole(ownerId, roomId, ownerId));

    assertEquals("Room owner cannot be reassigned as admin", exception.getMessage());
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(ownerId, roomId, Role.ADMIN);
  }

  @Test
  @DisplayName("assignAdminRole should report actual previous role for existing admin")
  void assignAdminRole_preservesPreviousRoleForExistingAdmin() {
    Integer ownerId = 10;
    Integer targetUserId = 12;
    User owner = createUser(ownerId, "owner@example.com", "owner");
    User targetUser = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(userRepository.getUserById(targetUserId)).thenReturn(targetUser);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.ADMIN);

    var response = roomMembershipService.assignAdminRole(ownerId, roomId, targetUserId);

    assertEquals(Role.ADMIN, response.getPreviousRole());
    assertEquals(Role.ADMIN, response.getNewRole());
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(targetUserId, roomId, Role.ADMIN);
  }

  @Test
  @DisplayName("demoteAdminRole should reject target that is not admin")
  void demoteAdminRole_rejectsNonAdminTarget() {
    Integer ownerId = 10;
    Integer targetUserId = 12;
    User owner = createUser(ownerId, "owner@example.com", "owner");
    User targetUser = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(userRepository.getUserById(targetUserId)).thenReturn(targetUser);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.PARTICIPANT);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.demoteAdminRole(ownerId, roomId, targetUserId));

    assertEquals("Only admin users can be demoted", exception.getMessage());
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(targetUserId, roomId, Role.PARTICIPANT);
  }

  @Test
  @DisplayName("demoteAdminRole should reject non-member target instead of leaking repository failure")
  void demoteAdminRole_rejectsNonMemberTarget() {
    Integer ownerId = 10;
    Integer targetUserId = 12;
    User owner = createUser(ownerId, "owner@example.com", "owner");
    User targetUser = createUser(targetUserId, "target@example.com", "target");

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(userRepository.getUserById(ownerId)).thenReturn(owner);
    when(userRepository.getUserById(targetUserId)).thenReturn(targetUser);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(false);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.demoteAdminRole(ownerId, roomId, targetUserId));

    assertEquals("Target user is not a member of the room and cannot be demoted.",
        exception.getMessage());
    verify(roomRepository, never()).getUserRoleByRoomId(roomId, targetUserId);
    verify(roomRepository, never()).setRoleByUserIdAndRoomId(targetUserId, roomId, Role.PARTICIPANT);
  }

  @Test
  @DisplayName("getBanRooms should return only banned rooms")
  void getBanRooms_returnsOnlyBannedRooms() {
    Room bannedRoom = createRoom(101, null);
    bannedRoom.setName("Banned room");
    Room openRoom = createRoom(102, null);
    openRoom.setName("Open room");

    when(roomRepository.getAllRooms()).thenReturn(List.of(bannedRoom, openRoom));
    when(roomRepository.isUserBannedInRoom(101, userId)).thenReturn(true);
    when(roomRepository.isUserBannedInRoom(102, userId)).thenReturn(false);
    when(roomRepository.getUsersInRoom(101)).thenReturn(roomUsers);

    var responses = roomMembershipService.getBanRooms(userId);

    assertEquals(1, responses.size());
    assertEquals(101, responses.getFirst().getId());
    assertEquals("Banned room", responses.getFirst().getName());
  }

  private User createUser(Integer id, String login, String username) {
    return new User(
        id,
        login,
        username,
        Instant.parse("2000-01-01T00:00:00Z"),
        "Russia",
        "Moscow",
        username,
        1,
        null,
        List.of(),
        List.of(Notification.ACTIVITY_CLOSED));
  }

  private Room createRoom(Integer id, Map<User, Role> users) {
    return new Room(
        id,
        true,
        true,
        "https://chat.example.com",
        Category.SPORT,
        "Morning Run",
        "Test room",
        Instant.parse("2030-01-01T10:00:00Z"),
        Instant.parse("2030-01-01T12:00:00Z"),
        18,
        Instant.parse("2029-12-01T10:00:00Z"),
        10,
        users,
        List.of());
  }
}
