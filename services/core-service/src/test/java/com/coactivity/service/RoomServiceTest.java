package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.PictureRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@DisplayName("RoomService Tests")
class RoomServiceTest {

  private RoomRepository roomRepository;
  private PictureRepository pictureRepository;
  private BulletinBoardRepository bulletinBoardRepository;
  private NotificationService notificationService;
  private RoomService roomService;

  private Integer ownerId;
  private Integer roomId;
  private Room room;
  private Map<User, Role> roomUsers;

  @BeforeEach
  void setUp() {
    roomRepository = Mockito.mock(RoomRepository.class);
    pictureRepository = Mockito.mock(PictureRepository.class);
    bulletinBoardRepository = Mockito.mock(BulletinBoardRepository.class);
    notificationService = Mockito.mock(NotificationService.class);
    roomService = new RoomService(roomRepository, pictureRepository, bulletinBoardRepository,
        notificationService);

    ownerId = 10;
    roomId = 100;

    User owner = createUser(ownerId, "owner@example.com", "owner");
    User participant = createUser(20, "participant@example.com", "participant");

    roomUsers = new LinkedHashMap<>();
    roomUsers.put(owner, Role.OWNER);
    roomUsers.put(participant, Role.PARTICIPANT);

    room = createRoom(roomId, false, roomUsers);

    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
  }

  @Test
  @DisplayName("deleteRoom should notify participants only after successful room deletion")
  void deleteRoom_notifiesParticipantsAfterSuccessfulDelete() {
    roomService.deleteRoom(ownerId, roomId);

    InOrder inOrder = inOrder(roomRepository, notificationService);
    inOrder.verify(roomRepository).deleteRoom(roomId);
    inOrder.verify(notificationService).sendActivityClosed(ownerId, "Morning Run");
    inOrder.verify(notificationService).sendActivityClosed(20, "Morning Run");
  }

  @Test
  @DisplayName("deleteRoom should not send activity closed notifications when deletion fails")
  void deleteRoom_doesNotNotifyParticipantsWhenDeletionFails() {
    doThrow(new RuntimeException("delete failed"))
        .when(roomRepository)
        .deleteRoom(roomId);

    assertThrows(RuntimeException.class, () -> roomService.deleteRoom(ownerId, roomId));

    verify(notificationService, never()).sendActivityClosed(ownerId, "Morning Run");
    verify(notificationService, never()).sendActivityClosed(20, "Morning Run");
  }

  @Test
  @DisplayName("getRooms should expose only public rooms in the public catalog")
  void getRooms_returnsOnlyPublicRooms() {
    Room publicRoom = createRoom(101, true, null);
    Room privateRoom = createRoom(102, false, null);

    when(roomRepository.getAllRooms()).thenReturn(List.of(privateRoom, publicRoom));
    when(roomRepository.getUsersInRoom(101)).thenReturn(Map.of());

    List<RoomSummaryResponse> responses = roomService.getRooms(null, null, RoomSort.NEWEST);

    assertEquals(1, responses.size());
    assertEquals(101, responses.getFirst().getId());
  }

  @Test
  @DisplayName("getRooms should reject unsupported location filters instead of silently ignoring them")
  void getRooms_rejectsUnsupportedLocationFilters() {
    RoomFilter filter = new RoomFilter();
    filter.setCity("Moscow");

    assertThrows(ValidationException.class, () -> roomService.getRooms(null, filter, null));
  }

  @Test
  @DisplayName("getRoomById should load creator and participant count from repository when room entity is not hydrated")
  void getRoomById_loadsUsersFromRepositoryWhenRoomEntityHasNoUsers() {
    Room roomWithoutUsers = createRoom(roomId, true, null);

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.getUsersInRoom(roomId)).thenReturn(roomUsers);
    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);

    RoomDetailedResponse response = roomService.getRoomById(roomId, ownerId);

    assertNotNull(response);
    assertEquals(2, response.getParticipantCount());
    assertNotNull(response.getCreator());
    assertEquals("owner", response.getCreator().getUserName());
  }

  @Test
  @DisplayName("deleteRoom should load participants from repository when room entity is not hydrated")
  void deleteRoom_loadsParticipantsFromRepositoryWhenRoomEntityHasNoUsers() {
    Room roomWithoutUsers = createRoom(roomId, false, null);

    when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
    when(roomRepository.getUsersInRoom(roomId)).thenReturn(roomUsers);

    roomService.deleteRoom(ownerId, roomId);

    verify(notificationService).sendActivityClosed(ownerId, "Morning Run");
    verify(notificationService).sendActivityClosed(20, "Morning Run");
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

  private Room createRoom(Integer id, boolean isPublic, Map<User, Role> users) {
    return new Room(
        id,
        true,
        isPublic,
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
