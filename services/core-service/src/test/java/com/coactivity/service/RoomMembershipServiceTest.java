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
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.PictureRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("RoomMembershipService Tests")
class RoomMembershipServiceTest {

  private UserRepository userRepository;
  private RoomRepository roomRepository;
  private RoomsRequestRepository roomsRequestRepository;
  private PictureRepository pictureRepository;
  private BulletinBoardRepository bulletinBoardRepository;
  private NotificationService notificationService;
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
    pictureRepository = Mockito.mock(PictureRepository.class);
    bulletinBoardRepository = Mockito.mock(BulletinBoardRepository.class);
    notificationService = Mockito.mock(NotificationService.class);

    roomMembershipService = new RoomMembershipService(
        userRepository,
        roomRepository,
        roomsRequestRepository,
        pictureRepository,
        bulletinBoardRepository,
        notificationService);

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
    when(roomRepository.getRoomById(roomId)).thenReturn(publicRoom);
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
    verify(roomRepository, never()).getRoomById(roomId);
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
    when(roomRepository.getRoomById(roomId)).thenReturn(privateRoom);
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
    when(roomRepository.getRoomById(roomId)).thenReturn(privateRoom);
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
    when(roomRepository.getRoomById(roomId)).thenReturn(privateRoom);
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
    when(roomRepository.getRoomById(roomId)).thenReturn(privateRoom);
    when(roomRepository.isUserBannedInRoom(roomId, userId)).thenReturn(true);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> roomMembershipService.joinRoom(userId, roomId));

    assertEquals("User is banned from this room", exception.getMessage());
    verify(roomsRequestRepository, never()).createRequest(userId, roomId, RequestStatus.CONSIDERATION);
    verify(roomRepository, never()).addUserToRoom(roomId, userId, Role.PARTICIPANT);
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
  @DisplayName("isUserInRoom should reject null user id")
  void isUserInRoom_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomMembershipService.isUserInRoom(null, roomId));

    assertEquals("User id is required", exception.getMessage());
    verify(roomRepository, never()).getRoomById(roomId);
    verify(roomRepository, never()).isUserInMembers(roomId, null);
  }

  @Test
  @DisplayName("assignAdminRole should reject non-member target")
  void assignAdminRole_rejectsNonMemberTarget() {
    Integer ownerId = 10;
    Integer targetUserId = 99;
    User owner = createUser(ownerId, "owner@example.com", "owner");
    User targetUser = createUser(targetUserId, "target@example.com", "target");

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
