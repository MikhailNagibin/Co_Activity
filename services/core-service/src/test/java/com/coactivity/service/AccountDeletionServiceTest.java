package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.AccountDeletionMode;
import com.coactivity.controller.dto.request.AccountDeletionRequest;
import com.coactivity.controller.dto.request.AccountDeletionRoomActionRequest;
import com.coactivity.controller.dto.response.AccountDeletionPreviewResponse;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Account deletion service tests")
class AccountDeletionServiceTest {

  private UserRepository userRepository;
  private RoomRepository roomRepository;
  private NotificationService notificationService;
  private AccountDeletionService accountDeletionService;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    roomRepository = Mockito.mock(RoomRepository.class);
    notificationService = Mockito.mock(NotificationService.class);
    accountDeletionService = new AccountDeletionService(userRepository, roomRepository,
        notificationService);
  }

  @Test
  void getDeletionPreviewReturnsImmediateDeleteWhenUserOwnsNoRooms() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of());

    AccountDeletionPreviewResponse response = accountDeletionService.getDeletionPreview(1);

    assertTrue(response.isCanDeleteImmediately());
    assertTrue(response.getOwnedRooms().isEmpty());
  }

  @Test
  void getDeletionPreviewTreatsMissingOwnedRoomsListAsEmpty() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(null);

    AccountDeletionPreviewResponse response = accountDeletionService.getDeletionPreview(1);

    assertTrue(response.isCanDeleteImmediately());
    assertTrue(response.getOwnedRooms().isEmpty());
  }

  @Test
  void getDeletionPreviewReturnsSortedTransferCandidates() {
    Room ownedRoom = room(10, "Preview room");
    User owner = user(1, "owner");
    User admin = user(2, "admin");
    User participant = user(3, "participant");

    when(userRepository.getUserById(1)).thenReturn(owner);
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(ownedRoom));
    when(roomRepository.getUsersInRoom(10)).thenReturn(Map.of(
        owner, Role.OWNER,
        participant, Role.PARTICIPANT,
        admin, Role.ADMIN));

    AccountDeletionPreviewResponse response = accountDeletionService.getDeletionPreview(1);

    assertFalse(response.isCanDeleteImmediately());
    assertEquals(1, response.getOwnedRooms().size());
    assertEquals(3, response.getOwnedRooms().getFirst().getParticipantCount());
    assertEquals(List.of("admin", "participant"),
        response.getOwnedRooms().getFirst().getTransferCandidates().stream()
            .map(candidate -> candidate.getUserName())
            .toList());
  }

  @Test
  void getDeletionPreviewReturnsNoTransferCandidatesWhenOwnerIsAlone() {
    Room ownedRoom = room(10, "Solo room");
    User owner = user(1, "owner");

    when(userRepository.getUserById(1)).thenReturn(owner);
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(ownedRoom));
    when(roomRepository.getUsersInRoom(10)).thenReturn(Map.of(owner, Role.OWNER));

    AccountDeletionPreviewResponse response = accountDeletionService.getDeletionPreview(1);

    assertFalse(response.isCanDeleteImmediately());
    assertTrue(response.getOwnedRooms().getFirst().getTransferCandidates().isEmpty());
  }

  @Test
  void getDeletionPreviewHandlesMissingMembershipMap() {
    Room ownedRoom = room(10, "Preview room");
    User owner = user(1, "owner");

    when(userRepository.getUserById(1)).thenReturn(owner);
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(ownedRoom));
    when(roomRepository.getUsersInRoom(10)).thenReturn(null);

    AccountDeletionPreviewResponse response = accountDeletionService.getDeletionPreview(1);

    assertFalse(response.isCanDeleteImmediately());
    assertEquals(1, response.getOwnedRooms().size());
    assertEquals(0, response.getOwnedRooms().getFirst().getParticipantCount());
    assertTrue(response.getOwnedRooms().getFirst().getTransferCandidates().isEmpty());
  }

  @Test
  void deleteAccountIfNoOwnedRoomsDeletesUser() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of());

    accountDeletionService.deleteAccountIfNoOwnedRooms(1);

    verify(userRepository).deleteUser(1);
  }

  @Test
  void deleteAccountIfNoOwnedRoomsTreatsMissingOwnedRoomsListAsEmpty() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(null);

    accountDeletionService.deleteAccountIfNoOwnedRooms(1);

    verify(userRepository).deleteUser(1);
  }

  @Test
  void deleteAccountIfNoOwnedRoomsThrowsConflictWhenOwnedRoomsExist() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> accountDeletionService.deleteAccountIfNoOwnedRooms(1));

    assertEquals("OWNED_ROOMS_RESOLUTION_REQUIRED", exception.getCode());
    verify(userRepository, never()).deleteUser(1);
  }

  @Test
  void deleteAccountRejectsMissingRoomActions() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "First"), room(11, "Second")));

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null)));

    assertThrows(ValidationException.class, () -> accountDeletionService.deleteAccount(1, request));
  }

  @Test
  void deleteAccountRejectsDuplicateRoomActions() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null),
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null)));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("Deletion actions contain duplicate room IDs", exception.getMessage());
  }

  @Test
  void deleteAccountRejectsDeleteActionWithTransferTarget() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, 2)));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("Delete room action must not include transfer target", exception.getMessage());
    verify(roomRepository, never()).deleteRoom(10);
  }

  @Test
  void deleteAccountRejectsTransferToDeletingUser() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.TRANSFER_OWNERSHIP, 1)));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("INVALID_OWNERSHIP_TRANSFER", exception.getCode());
    verify(userRepository, never()).deleteUser(1);
  }

  @Test
  void deleteAccountRejectsActionFlowWhenUserOwnsNoRooms() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of());

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null)));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("This account can be deleted without room actions", exception.getMessage());
    verify(userRepository, never()).deleteUser(1);
  }

  @Test
  void deleteAccountTreatsMissingOwnedRoomsListAsNoOwnedRooms() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(null);

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null)));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("This account can be deleted without room actions", exception.getMessage());
    verify(userRepository, never()).deleteUser(1);
  }

  @Test
  void deleteAccountTransfersOwnershipDeletesRoomsAndDeletesUser() {
    User owner = user(1, "owner");
    User newOwner = user(2, "newOwner");
    User participant = user(3, "participant");
    Room deletedRoom = room(10, "Deleted room");
    Room transferredRoom = room(11, "Transferred room");

    when(userRepository.getUserById(1)).thenReturn(owner);
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(deletedRoom, transferredRoom));
    when(roomRepository.getUsersInRoom(10)).thenReturn(Map.of(
        owner, Role.OWNER,
        participant, Role.PARTICIPANT));
    when(roomRepository.isUserInMembers(11, 2)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(11, 2)).thenReturn(Role.ADMIN);

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null),
        new AccountDeletionRoomActionRequest(11, AccountDeletionMode.TRANSFER_OWNERSHIP, 2)));

    accountDeletionService.deleteAccount(1, request);

    verify(roomRepository).deleteRoom(10);
    verify(notificationService).sendActivityClosed(3, "Deleted room");
    verify(roomRepository).setRoleByUserIdAndRoomId(2, 11, Role.OWNER);
    verify(roomRepository).setRoleByUserIdAndRoomId(1, 11, Role.PARTICIPANT);
    verify(userRepository).deleteUser(1);
  }

  @Test
  void deleteAccountDeleteRoomActionHandlesMissingMembershipMap() {
    User owner = user(1, "owner");
    Room deletedRoom = room(10, "Deleted room");

    when(userRepository.getUserById(1)).thenReturn(owner);
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(deletedRoom));
    when(roomRepository.getUsersInRoom(10)).thenReturn(null);

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.DELETE_ROOM, null)));

    accountDeletionService.deleteAccount(1, request);

    verify(roomRepository).deleteRoom(10);
    verify(notificationService, never()).sendActivityClosed(Mockito.anyInt(), Mockito.anyString());
    verify(userRepository).deleteUser(1);
  }

  @Test
  void deleteAccountRejectsTransferToNonParticipant() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));
    when(roomRepository.isUserInMembers(10, 2)).thenReturn(false);

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.TRANSFER_OWNERSHIP, 2)));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("INVALID_OWNERSHIP_TRANSFER", exception.getCode());
    verify(userRepository, never()).deleteUser(1);
  }

  @Test
  void deleteAccountRejectsTransferToUnsupportedRole() {
    when(userRepository.getUserById(1)).thenReturn(user(1, "owner"));
    when(roomRepository.getRoomsOwnedByUser(1)).thenReturn(List.of(room(10, "Owned room")));
    when(roomRepository.isUserInMembers(10, 2)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(10, 2)).thenReturn(Role.OWNER);

    AccountDeletionRequest request = new AccountDeletionRequest(List.of(
        new AccountDeletionRoomActionRequest(10, AccountDeletionMode.TRANSFER_OWNERSHIP, 2)));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> accountDeletionService.deleteAccount(1, request));

    assertEquals("INVALID_OWNERSHIP_TRANSFER", exception.getCode());
    verify(userRepository, never()).deleteUser(1);
  }

  private User user(Integer id, String userName) {
    return new User(id, userName + "@example.com", userName, Instant.now(), null, null, null, null,
        List.of(), List.of());
  }

  private Room room(Integer id, String name) {
    return new Room(id, true, true, null, Category.SPORT, name, "description", Instant.now(),
        Instant.now().plusSeconds(3600), 18, null, 10, Map.of(), List.of());
  }
}
