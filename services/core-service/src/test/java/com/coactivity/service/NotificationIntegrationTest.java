package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.DataRepository;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.sql.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("Notification Integration Tests")
class NotificationIntegrationTest {

  private static final String TOPIC = "notifications.email.v1";
  private final ObjectMapper objectMapper = new ObjectMapper();

  private RoomRepositoryImpl roomRepository;
  private RoomsRequestRepositoryImpl roomsRequestRepository;
  private UserRepositoryImpl userRepository;
  private DataRepository dataRepository;
  private KafkaTemplate<String, String> kafkaTemplate;
  private NotificationService notificationService;
  private JoinRequestService joinRequestService;
  private Connection transactionConnection;

  private User testUser;
  private User adminUser;
  private Room testRoom;
  private RoomsRequest testRequest;
  private Integer userId;
  private Integer adminId;
  private Integer roomId;
  private Integer requestId;

  @BeforeEach
  void setUp() {
    roomRepository = Mockito.mock(RoomRepositoryImpl.class);
    roomsRequestRepository = Mockito.mock(RoomsRequestRepositoryImpl.class);
    userRepository = Mockito.mock(UserRepositoryImpl.class);
    dataRepository = Mockito.mock(DataRepository.class);
    kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    transactionConnection = Mockito.mock(Connection.class);

    notificationService = new NotificationService(userRepository, objectMapper);
    notificationService.setKafkaTemplate(kafkaTemplate);
    notificationService.setNotificationsKafkaTopic(TOPIC);

    CompletableFuture<SendResult<String, String>> sendResult =
        CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(sendResult);
    when(dataRepository.inTransaction(any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      DataRepository.ConnectionCallback<Object> callback =
          (DataRepository.ConnectionCallback<Object>) invocation.getArgument(0);
      return callback.execute(transactionConnection);
    });

    joinRequestService = new JoinRequestService(dataRepository, roomRepository, roomsRequestRepository,
        userRepository, notificationService);

    userId = 1;
    adminId = 2;
    roomId = 100;
    requestId = 1000;

    testUser = new User(
        userId,
        "test@example.com",
        "testuser",
        "password",
        Instant.now(),
        "Country",
        "City",
        "Description",
        1,
        List.of(),
        List.of(Notification.MEMBERSHIP_ACCEPTED, Notification.MEMBERSHIP_REJECTED));

    adminUser = new User(
        adminId,
        "admin@example.com",
        "admin",
        "password",
        Instant.now(),
        "Country",
        "City",
        "Admin description",
        1,
        List.of(),
        List.of());

    Map<User, Role> roomUsers = new HashMap<>();
    roomUsers.put(adminUser, Role.OWNER);

    testRoom = new Room(
        roomId,
        false,
        false,
        "https://chat.link",
        Category.SPORT,
        "Test Room",
        "Room description",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        18,
        Instant.now(),
        10,
        roomUsers,
        List.of());

    testRequest = new RoomsRequest(
        requestId,
        testUser,
        testRoom,
        Instant.now(),
        RequestStatus.CONSIDERATION);

    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
  }

  @Test
  @DisplayName("Accepted request updates membership and publishes Kafka email command")
  void processJoinRequestAcceptedUpdatesStateAndPublishesKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembersInTransaction(transactionConnection, roomId, userId))
        .thenReturn(false);
    when(roomRepository.getRoomParticipantCountInTransaction(transactionConnection, roomId))
        .thenReturn(3);
    when(userRepository.getUserById(userId)).thenReturn(testUser);

    joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

    verify(roomRepository, times(1))
        .addUserToRoomInTransaction(transactionConnection, roomId, userId, Role.PARTICIPANT);
    verify(roomsRequestRepository, times(1))
        .updateRequestInTransaction(transactionConnection, requestId, RequestStatus.ACCEPTED);
    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Rejected request updates status and publishes Kafka email command")
  void processJoinRequestRefusedUpdatesStateAndPublishesKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(userRepository.getUserById(userId)).thenReturn(testUser);

    joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED);

    verify(roomsRequestRepository, times(1)).updateRequest(requestId, RequestStatus.REFUSED);
    verify(roomRepository, never()).addUserBan(roomId, userId);
    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Refuse with ban updates ban state and publishes Kafka email command")
  void processJoinRequestRefusedWithBanUpdatesStateAndPublishesKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(userRepository.getUserById(userId)).thenReturn(testUser);

    joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED_WITH_BAN);

    verify(roomRepository, times(1))
        .addUserBanInTransaction(transactionConnection, roomId, userId);
    verify(roomsRequestRepository, times(1))
        .updateRequestInTransaction(transactionConnection, requestId,
        RequestStatus.REFUSED_WITH_BAN);
    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Accepted request does not publish notification when room capacity is exceeded")
  void processJoinRequestAcceptedWhenRoomFullThrowsAndSkipsKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(roomRepository.getRoomParticipantCountInTransaction(transactionConnection, roomId))
        .thenReturn(testRoom.getMaximumNumberOfPeople());

    assertThrows(ValidationException.class,
        () -> joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    verify(roomsRequestRepository, never())
        .updateRequestInTransaction(transactionConnection, requestId, RequestStatus.ACCEPTED);
  }

  @Test
  @DisplayName("Accepted request does not publish notification when request status update fails")
  void processJoinRequestAcceptedWhenUpdateFailsSkipsKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembersInTransaction(transactionConnection, roomId, userId))
        .thenReturn(false);
    when(roomRepository.getRoomParticipantCountInTransaction(transactionConnection, roomId))
        .thenReturn(3);
    doThrow(new RuntimeException("status update failed"))
        .when(roomsRequestRepository)
        .updateRequestInTransaction(transactionConnection, requestId, RequestStatus.ACCEPTED);

    assertThrows(RuntimeException.class,
        () -> joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Refuse with ban does not publish notification when request status update fails")
  void processJoinRequestRefusedWithBanWhenUpdateFailsSkipsKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    doThrow(new RuntimeException("status update failed"))
        .when(roomsRequestRepository)
        .updateRequestInTransaction(transactionConnection, requestId,
            RequestStatus.REFUSED_WITH_BAN);

    assertThrows(RuntimeException.class, () -> joinRequestService.processJoinRequest(adminId,
        requestId, RequestStatus.REFUSED_WITH_BAN));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Moderator rights check should propagate repository failures instead of masking them")
  void getPendingRequestsForRoomPropagatesRepositoryFailure() {
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId))
        .thenThrow(new RuntimeException("database unavailable"));

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> joinRequestService.getPendingRequestsForRoom(adminId, roomId));

    org.junit.jupiter.api.Assertions.assertEquals("database unavailable", exception.getMessage());
  }
}
