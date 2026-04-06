package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.PictureRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
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
