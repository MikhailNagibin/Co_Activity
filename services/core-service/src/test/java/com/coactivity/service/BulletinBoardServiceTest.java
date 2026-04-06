package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("BulletinBoardService authorization tests")
class BulletinBoardServiceTest {

  private BulletinBoardRepositoryImpl bulletinBoardRepository;
  private RoomRepositoryImpl roomRepository;
  private UserRepositoryImpl userRepository;
  private BulletinBoardService bulletinBoardService;

  @BeforeEach
  void setUp() {
    bulletinBoardRepository = Mockito.mock(BulletinBoardRepositoryImpl.class);
    roomRepository = Mockito.mock(RoomRepositoryImpl.class);
    userRepository = Mockito.mock(UserRepositoryImpl.class);
    bulletinBoardService = new BulletinBoardService(bulletinBoardRepository, roomRepository,
        userRepository);
  }

  @Test
  @DisplayName("Participant cannot update bulletin board")
  void participantCannotUpdateBulletinBoard() {
    Integer roomId = 10;
    Integer userId = 11;
    Room room = room(roomId);
    User user = user(userId);

    when(userRepository.getUserById(userId)).thenReturn(user);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.PARTICIPANT);

    assertThrows(AuthorizationException.class,
        () -> bulletinBoardService.updateBulletinBoard(roomId, "new content", userId));

    verifyNoInteractions(bulletinBoardRepository);
  }

  @Test
  @DisplayName("Participant cannot delete bulletin board")
  void participantCannotDeleteBulletinBoard() {
    Integer roomId = 20;
    Integer userId = 21;
    Room room = room(roomId);
    User user = user(userId);

    when(userRepository.getUserById(userId)).thenReturn(user);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.PARTICIPANT);

    assertThrows(AuthorizationException.class,
        () -> bulletinBoardService.deleteBulletinBoard(roomId, userId));

    verify(bulletinBoardRepository, never()).deleteBulletinBoard(roomId);
  }

  @Test
  @DisplayName("Admin can create bulletin board when absent")
  void adminCanCreateBulletinBoardWhenAbsent() {
    Integer roomId = 30;
    Integer userId = 31;
    String content = "Important update";
    Room room = room(roomId);
    User user = user(userId);
    BulletinBoard board = new BulletinBoard(1, room, content, user, Instant.now());

    when(userRepository.getUserById(userId)).thenReturn(user);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.ADMIN);
    when(bulletinBoardRepository.getBulletinBoard(roomId)).thenReturn(null);
    when(bulletinBoardRepository.createBulletinBoard(roomId, content, userId)).thenReturn(board);

    BulletinBoardResponse response = bulletinBoardService.updateBulletinBoard(roomId, content,
        userId);

    assertEquals(content, response.getContent());
    assertEquals(userId, response.getAuthor().getId());
  }

  @Test
  @DisplayName("Admin updates existing bulletin board instead of creating a new one")
  void adminUpdatesExistingBulletinBoard() {
    Integer roomId = 40;
    Integer userId = 41;
    String content = "Updated bulletin";
    Room room = room(roomId);
    User user = user(userId);
    BulletinBoard existingBoard = new BulletinBoard(2, room, "Old content", user, Instant.now());
    BulletinBoard updatedBoard = new BulletinBoard(2, room, content, user, Instant.now());

    when(userRepository.getUserById(userId)).thenReturn(user);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.ADMIN);
    when(bulletinBoardRepository.getBulletinBoard(roomId)).thenReturn(existingBoard);
    when(bulletinBoardRepository.updateBulletinBoard(roomId, content, userId)).thenReturn(updatedBoard);

    BulletinBoardResponse response = bulletinBoardService.updateBulletinBoard(roomId, content,
        userId);

    assertEquals(content, response.getContent());
    verify(bulletinBoardRepository).updateBulletinBoard(roomId, content, userId);
    verify(bulletinBoardRepository, never()).createBulletinBoard(roomId, content, userId);
  }

  @Test
  @DisplayName("Owner can delete existing bulletin board")
  void ownerCanDeleteExistingBulletinBoard() {
    Integer roomId = 50;
    Integer userId = 51;
    Room room = room(roomId);
    User user = user(userId);

    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.OWNER);
    when(bulletinBoardRepository.isBulletinBoardExists(roomId)).thenReturn(true);

    bulletinBoardService.deleteBulletinBoard(roomId, userId);

    verify(bulletinBoardRepository).deleteBulletinBoard(roomId);
  }

  @Test
  @DisplayName("Empty bulletin content is rejected before repository interaction")
  void emptyBulletinContentIsRejected() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> bulletinBoardService.updateBulletinBoard(60, "   ", 61));

    assertEquals("Bulletin content cannot be empty", exception.getMessage());
    verifyNoInteractions(bulletinBoardRepository);
  }

  @Test
  @DisplayName("Deleting absent bulletin board returns not found")
  void deletingAbsentBulletinBoardReturnsNotFound() {
    Integer roomId = 70;
    Integer userId = 71;
    Room room = room(roomId);

    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.ADMIN);
    when(bulletinBoardRepository.isBulletinBoardExists(roomId)).thenReturn(false);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> bulletinBoardService.deleteBulletinBoard(roomId, userId));

    assertEquals("Bulletin board not found for room 70", exception.getMessage());
    verify(bulletinBoardRepository, never()).deleteBulletinBoard(roomId);
  }

  private Room room(Integer roomId) {
    return new Room(roomId, true, true, null, Category.SPORT, "Room", "Desc", null,
        null, 0, null, 10, null, null);
  }

  private User user(Integer userId) {
    return new User(userId, "user@example.com", "user", null,
        "RU", "Moscow", "desc", null, List.of(), List.of());
  }
}
