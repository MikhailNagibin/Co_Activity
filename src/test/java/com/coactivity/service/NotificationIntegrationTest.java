package com.coactivity.service;

import static org.mockito.Mockito.*;

import com.coactivity.domain.*;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests verifying that notification service is properly integrated with business logic
 * services. Tests run synchronously without Spring context to avoid async complications.
 */
@DisplayName("Notification Integration Tests")
class NotificationIntegrationTest {

  private RoomRepositoryImpl roomRepository;
  private RoomsRequestRepositoryImpl roomsRequestRepository;
  private UserRepositoryImpl userRepository;
  private MailService mailService;
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
    // Create mocks manually
    roomRepository = Mockito.mock(RoomRepositoryImpl.class);
    roomsRequestRepository = Mockito.mock(RoomsRequestRepositoryImpl.class);
    userRepository = Mockito.mock(UserRepositoryImpl.class);
    mailService = Mockito.mock(MailService.class);

    // Create real NotificationService, then spy it to verify calls
    NotificationService realNotificationService = new NotificationService(mailService,
        userRepository);
    notificationService = Mockito.spy(realNotificationService);

    // Create JoinRequestService with spied notificationService
    joinRequestService = new JoinRequestService(roomRepository, roomsRequestRepository,
        userRepository, notificationService);

    // Initialize test data
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
        List.of(Notification.MEMBERSHIP_ACCEPTED, Notification.MEMBERSHIP_REJECTED)
    );

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
        List.of()
    );

    Map<User, Role> roomUsers = new HashMap<>();
    roomUsers.put(adminUser, Role.OWNER);

    testRoom = new Room(
        roomId,
        false, // private room
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
        List.of()
    );

    testRequest = new RoomsRequest(
        requestId,
        testUser,
        testRoom,
        Instant.now(),
        RequestStatus.CONSIDERATION
    );
  }

  // ==================== JoinRequestService Integration ====================

  @Nested
  @DisplayName("JoinRequestService -> NotificationService")
  class JoinRequestIntegrationTests {

    @Test
    @DisplayName("Should trigger MEMBERSHIP_ACCEPTED notification when request is accepted")
    void acceptRequest_triggersAcceptedNotification() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
      when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

      // Assert - verify NotificationService method was called
      verify(notificationService, times(1))
          .sendMembershipAccepted(eq(userId), eq("Test Room"));
    }

    @Test
    @DisplayName("Should trigger MEMBERSHIP_REJECTED notification when request is refused")
    void refuseRequest_triggersRejectedNotification() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED);

      // Assert
      verify(notificationService, times(1))
          .sendMembershipRejected(eq(userId), eq("Test Room"));
    }

    @Test
    @DisplayName("Should trigger MEMBERSHIP_REJECTED notification when user is banned")
    void refuseWithBan_triggersRejectedNotification() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED_WITH_BAN);

      // Assert
      verify(notificationService, times(1))
          .sendMembershipRejected(eq(userId), eq("Test Room"));
    }

    @Test
    @DisplayName("Should send email when user has notification preference enabled")
    void acceptRequest_sendsEmailIfPreferenceEnabled() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
      when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

      // Assert - verify actual email was sent
      verify(mailService, times(1))
          .sendSimpleMessage(eq(testUser.getLogin()), anyString(), anyString());
    }

    @Test
    @DisplayName("Should NOT send email when user has notification preference disabled")
    void acceptRequest_skipsEmailIfPreferenceDisabled() {
      // Arrange
      User userWithoutPreferences = new User(
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
          List.of() // No notification preferences
      );

      RoomsRequest requestWithoutPrefs = new RoomsRequest(
          requestId,
          userWithoutPreferences,
          testRoom,
          Instant.now(),
          RequestStatus.CONSIDERATION
      );

      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(requestWithoutPrefs);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(userWithoutPreferences);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
      when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

      // Assert - notification service called, but no email sent
      verify(notificationService, times(1)).sendMembershipAccepted(anyInt(), anyString());
      verify(mailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }
  }

  // ==================== Notification Parameters Tests ====================

  @Nested
  @DisplayName("Notification Parameters Verification")
  class NotificationParametersTests {

    @Test
    @DisplayName("Should pass correct user ID to notification service")
    void passesCorrectUserId() {
      // Arrange
      Integer specificUserId = 42;
      User specificUser = new User(
          specificUserId,
          "specific@example.com",
          "specificuser",
          "password",
          Instant.now(),
          "Country",
          "City",
          "Description",
          1,
          List.of(),
          List.of(Notification.MEMBERSHIP_ACCEPTED)
      );

      RoomsRequest request = new RoomsRequest(
          requestId,
          specificUser,
          testRoom,
          Instant.now(),
          RequestStatus.CONSIDERATION
      );

      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(request);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(specificUserId)).thenReturn(specificUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
      when(roomRepository.isUserInMembers(roomId, specificUserId)).thenReturn(false);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

      // Assert
      verify(notificationService).sendMembershipAccepted(eq(specificUserId), anyString());
    }

    @Test
    @DisplayName("Should pass correct room name to notification service")
    void passesCorrectRoomName() {
      // Arrange
      String customRoomName = "My Special Room";
      testRoom.setName(customRoomName);

      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED);

      // Assert
      verify(notificationService).sendMembershipRejected(anyInt(), eq(customRoomName));
    }
  }

  // ==================== Single Notification Tests ====================

  @Nested
  @DisplayName("Single Notification per Action")
  class SingleNotificationTests {

    @Test
    @DisplayName("Should call notification service exactly once per accepted request")
    void singleNotificationPerAcceptance() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);
      when(roomRepository.isUserInMembers(roomId, userId)).thenReturn(false);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.ACCEPTED);

      // Assert
      verify(notificationService, times(1)).sendMembershipAccepted(anyInt(), anyString());
      verify(notificationService, never()).sendMembershipRejected(anyInt(), anyString());
      verify(notificationService, never()).sendActivityClosed(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should call notification service exactly once per refused request")
    void singleNotificationPerRefusal() {
      // Arrange
      when(roomsRequestRepository.getRequestById(requestId)).thenReturn(testRequest);
      when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
      when(userRepository.getUserById(userId)).thenReturn(testUser);
      when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.OWNER);

      // Act
      joinRequestService.processJoinRequest(adminId, requestId, RequestStatus.REFUSED);

      // Assert
      verify(notificationService, times(1)).sendMembershipRejected(anyInt(), anyString());
      verify(notificationService, never()).sendMembershipAccepted(anyInt(), anyString());
      verify(notificationService, never()).sendActivityClosed(anyInt(), anyString());
    }
  }
}
