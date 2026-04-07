package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.coactivity.service.event.JoinRequestDecisionNotificationListener;
import com.coactivity.service.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("Notification integration tests")
class NotificationIntegrationTest {

  private static final String TOPIC = "notifications.email.v1";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private RoomRepository roomRepository;
  private RoomsRequestRepository roomsRequestRepository;
  private UserRepository userRepository;
  private KafkaTemplate<String, String> kafkaTemplate;
  private NotificationService notificationService;
  private JoinRequestService joinRequestService;

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
    roomRepository = Mockito.mock(RoomRepository.class);
    roomsRequestRepository = Mockito.mock(RoomsRequestRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    kafkaTemplate = Mockito.mock(KafkaTemplate.class);

    notificationService = new NotificationService(userRepository, objectMapper);
    notificationService.setKafkaTemplate(kafkaTemplate);
    notificationService.setNotificationsKafkaTopic(TOPIC);

    CompletableFuture<SendResult<String, String>> sendResult =
        CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(sendResult);

    JoinRequestDecisionNotificationListener listener =
        new JoinRequestDecisionNotificationListener(notificationService);
    ApplicationEventPublisher applicationEventPublisher = event -> {
      if (event instanceof JoinRequestDecisionEvent decisionEvent) {
        listener.onJoinRequestDecision(decisionEvent);
      }
    };

    joinRequestService = new JoinRequestService(
        roomRepository,
        roomsRequestRepository,
        userRepository,
        applicationEventPublisher);

    userId = 1;
    adminId = 2;
    roomId = 100;
    requestId = 1000;

    testUser = new User(
        userId,
        "test@example.com",
        "testuser",
        Instant.now(),
        "Country",
        "City",
        "Description",
        1,
        null,
        List.of(),
        List.of(Notification.MEMBERSHIP_ACCEPTED, Notification.MEMBERSHIP_REJECTED));

    adminUser = new User(
        adminId,
        "admin@example.com",
        "admin",
        Instant.now(),
        "Country",
        "City",
        "Admin description",
        1,
        null,
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
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(3);
    when(userRepository.getUserById(userId)).thenReturn(testUser);

    joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

    verify(roomRepository, times(1))
        .addUserToRoom(roomId, userId, Role.PARTICIPANT);
    verify(roomsRequestRepository, times(1))
        .updateRequest(requestId, RequestStatus.ACCEPTED);
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

    verify(roomRepository, times(1)).addUserBan(roomId, userId);
    verify(roomsRequestRepository, times(1))
        .updateRequest(requestId, RequestStatus.REFUSED_WITH_BAN);
    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Accepted request does not publish notification when room capacity is exceeded")
  void processJoinRequestAcceptedWhenRoomFullThrowsAndSkipsKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(roomRepository.getRoomParticipantCount(roomId))
        .thenReturn(testRoom.getMaximumNumberOfPeople());

    assertThrows(ValidationException.class,
        () -> joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    verify(roomsRequestRepository, never())
        .updateRequest(requestId, RequestStatus.ACCEPTED);
  }

  @Test
  @DisplayName("Accepted request does not publish notification when request status update fails")
  void processJoinRequestAcceptedWhenUpdateFailsSkipsKafka() {
    when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
    when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);
    when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(3);
    doThrow(new RuntimeException("status update failed"))
        .when(roomsRequestRepository)
        .updateRequest(requestId, RequestStatus.ACCEPTED);

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
        .updateRequest(requestId, RequestStatus.REFUSED_WITH_BAN);

    assertThrows(RuntimeException.class,
        () -> joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED_WITH_BAN));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }
}
