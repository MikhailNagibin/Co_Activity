package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.event.JoinRequestDecisionEvent;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("JoinRequestService tests")
class JoinRequestServiceTest {

  private RoomRepository roomRepository;
  private RoomsRequestRepository roomsRequestRepository;
  private UserRepository userRepository;
  private ApplicationEventPublisher applicationEventPublisher;
  private JoinRequestService joinRequestService;

  @BeforeEach
  void setUp() {
    roomRepository = Mockito.mock(RoomRepository.class);
    roomsRequestRepository = Mockito.mock(RoomsRequestRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);

    joinRequestService = new JoinRequestService(roomRepository, roomsRequestRepository,
        userRepository, applicationEventPublisher);
  }

  @Test
  void processJoinRequestAcceptsRequestAddsParticipantAndPublishesEvent() {
    Integer moderatorId = 10;
    Integer requesterId = 20;
    Integer roomId = 30;
    Integer requestId = 40;
    Room room = room(roomId, false, "Private Room", 5);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.CONSIDERATION);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, moderatorId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, moderatorId)).thenReturn(Role.ADMIN);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(1);
    when(roomRepository.isUserInMembers(roomId, requesterId)).thenReturn(false);

    joinRequestService.processJoinRequest(moderatorId, requestId, RequestStatus.ACCEPTED);

    verify(roomRepository).addUserToRoom(roomId, requesterId, Role.PARTICIPANT);
    verify(roomsRequestRepository).updateRequest(requestId, RequestStatus.ACCEPTED);
    verify(applicationEventPublisher).publishEvent(
        new JoinRequestDecisionEvent(requesterId, "Private Room", RequestStatus.ACCEPTED));
  }

  @Test
  void processJoinRequestRejectsAcceptWhenRoomCapacityIsExceeded() {
    Integer moderatorId = 10;
    Integer requesterId = 20;
    Integer roomId = 30;
    Integer requestId = 40;
    Room room = room(roomId, false, "Full Room", 2);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.CONSIDERATION);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, moderatorId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, moderatorId)).thenReturn(Role.OWNER);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(2);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> joinRequestService.processJoinRequest(moderatorId, requestId, RequestStatus.ACCEPTED));

    assertEquals("Room capacity exceeded", exception.getMessage());
    verify(roomRepository, never()).addUserToRoom(roomId, requesterId, Role.PARTICIPANT);
    verify(roomsRequestRepository, never()).updateRequest(requestId, RequestStatus.ACCEPTED);
    verify(applicationEventPublisher, never()).publishEvent(Mockito.any());
  }

  @Test
  void processJoinRequestRefuseWithBanUpdatesBanStateAndPublishesEvent() {
    Integer moderatorId = 10;
    Integer requesterId = 20;
    Integer roomId = 30;
    Integer requestId = 40;
    Room room = room(roomId, false, "Moderated Room", 5);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.CONSIDERATION);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, moderatorId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, moderatorId)).thenReturn(Role.ADMIN);

    joinRequestService.processJoinRequest(moderatorId, requestId, RequestStatus.REFUSED_WITH_BAN);

    verify(roomRepository).addUserBan(roomId, requesterId);
    verify(roomsRequestRepository).updateRequest(requestId, RequestStatus.REFUSED_WITH_BAN);
    verify(applicationEventPublisher).publishEvent(
        new JoinRequestDecisionEvent(requesterId, "Moderated Room", RequestStatus.REFUSED_WITH_BAN));
  }

  @Test
  void processJoinRequestRefuseUpdatesStatusAndPublishesEventWithoutBan() {
    Integer moderatorId = 10;
    Integer requesterId = 20;
    Integer roomId = 30;
    Integer requestId = 40;
    Room room = room(roomId, false, "Private Room", 5);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.CONSIDERATION);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);
    when(roomRepository.isUserInMembers(roomId, moderatorId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, moderatorId)).thenReturn(Role.OWNER);
    when(roomRepository.getRoomById(roomId)).thenReturn(room);

    joinRequestService.processJoinRequest(moderatorId, requestId, RequestStatus.REFUSED);

    verify(roomsRequestRepository).updateRequest(requestId, RequestStatus.REFUSED);
    verify(roomRepository, never()).addUserBan(roomId, requesterId);
    verify(applicationEventPublisher).publishEvent(
        new JoinRequestDecisionEvent(requesterId, "Private Room", RequestStatus.REFUSED));
  }

  @Test
  void cancelRequestRejectsAnotherUsersRequest() {
    Integer requesterId = 20;
    Integer anotherUserId = 21;
    Integer requestId = 40;
    Room room = room(30, false, "Private Room", 5);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.CONSIDERATION);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> joinRequestService.cancelRequest(anotherUserId, requestId));

    assertEquals("Cannot cancel request created by another user", exception.getMessage());
    verify(roomsRequestRepository, never()).deleteRequest(requestId);
  }

  @Test
  void processJoinRequestRejectsAlreadyProcessedRequest() {
    Integer moderatorId = 10;
    Integer requesterId = 20;
    Integer roomId = 30;
    Integer requestId = 40;
    Room room = room(roomId, false, "Private Room", 5);
    RoomsRequest request = request(requestId, requesterId, room, RequestStatus.ACCEPTED);

    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> joinRequestService.processJoinRequest(moderatorId, requestId, RequestStatus.REFUSED));

    assertEquals("Join request already processed", exception.getMessage());
    verify(roomsRequestRepository, never()).updateRequest(Mockito.anyInt(), Mockito.any());
    verify(applicationEventPublisher, never()).publishEvent(Mockito.any());
  }

  @Test
  void getPendingRequestsForRoomRejectsNonModerator() {
    Integer userId = 10;
    Integer roomId = 30;
    Room room = room(roomId, false, "Private Room", 5);

    when(roomRepository.getRoomById(roomId)).thenReturn(room);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, userId)).thenReturn(Role.PARTICIPANT);

    AuthorizationException exception = assertThrows(AuthorizationException.class,
        () -> joinRequestService.getPendingRequestsForRoom(userId, roomId));

    assertEquals("User lacks moderation rights", exception.getMessage());
  }

  @Test
  void getSentRequestsRejectsUnknownUser() {
    when(userRepository.getUserById(99)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> joinRequestService.getSentRequests(99));

    assertEquals("User not found", exception.getMessage());
  }

  @Test
  void getPendingRequestsReturnsOnlyConsiderationRequestsForPrivateModeratedRooms() {
    Integer moderatorId = 10;
    Room privateModeratedRoom = room(30, false, "Private Moderated", 5);
    Room publicRoom = room(31, true, "Public Room", 5);
    Room privateUnmanagedRoom = room(32, false, "Private Unmanaged", 5);
    User moderator = user(moderatorId, "moderator@example.com", "moderator",
        List.of(privateModeratedRoom, publicRoom, privateUnmanagedRoom));

    RoomsRequest pendingRequest = request(40, 20, privateModeratedRoom, RequestStatus.CONSIDERATION);
    RoomsRequest acceptedRequest = request(41, 21, privateModeratedRoom, RequestStatus.ACCEPTED);

    when(userRepository.getUserById(moderatorId)).thenReturn(moderator);
    when(roomRepository.isUserInMembers(30, moderatorId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(30, moderatorId)).thenReturn(Role.ADMIN);
    when(roomRepository.isUserInMembers(32, moderatorId)).thenReturn(false);
    when(roomsRequestRepository.getRoomRequests(30)).thenReturn(List.of(pendingRequest, acceptedRequest));

    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(moderatorId);

    assertEquals(1, responses.size());
    assertEquals(pendingRequest.getId(), responses.getFirst().getRequestId());
    assertEquals("Private Moderated", responses.getFirst().getRoomName());
  }

  private User user(Integer id, String email, String username, List<Room> rooms) {
    return new User(
        id,
        email,
        username,
        Instant.parse("2000-01-01T00:00:00Z"),
        "Russia",
        "Moscow",
        username,
        1,
        rooms,
        List.of(Notification.ACTIVITY_CLOSED));
  }

  private Room room(Integer id, boolean isPublic, String name, int maxParticipants) {
    return new Room(
        id,
        true,
        isPublic,
        "https://chat.example.com",
        Category.SPORT,
        name,
        "Room for testing",
        Instant.parse("2030-01-01T10:00:00Z"),
        Instant.parse("2030-01-01T12:00:00Z"),
        18,
        Instant.parse("2029-12-01T10:00:00Z"),
        maxParticipants,
        null,
        List.of());
  }

  private RoomsRequest request(Integer id, Integer requesterId, Room room, RequestStatus status) {
    return new RoomsRequest(
        id,
        user(requesterId, "user" + requesterId + "@example.com", "user" + requesterId, List.of()),
        room,
        Instant.parse("2029-12-01T09:00:00Z"),
        status);
  }
}
