//package com.coactivity.controller;
//
//import com.coactivity.controller.dto.request.LoginRequest;
//import com.coactivity.controller.dto.request.NotificationSettingsRequest;
//import com.coactivity.controller.dto.request.RoomCreationRequest;
//import com.coactivity.controller.dto.request.UserRegistrationRequest;
//import com.coactivity.controller.dto.response.*;
//import com.coactivity.controller.impl.RoomControllerImpl;
//import com.coactivity.controller.impl.UserControllerImpl;
//import com.coactivity.domain.*;
//import com.coactivity.repository.impl.*;
//import com.coactivity.service.*;
//import com.coactivity.service.exception.AuthorizationException;
//import com.coactivity.service.exception.ResourceNotFoundException;
//import com.coactivity.service.exception.ValidationException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.ResponseEntity;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RoomControllerImplTest {
//
//  @Mock
//  private RoomService roomService;
//
//  @Mock
//  private TokenService tokenService;
//
//  @Mock
//  private UserWithRoomService userWithRoomService;
//
//  @Mock
//  private BulletinBoardService bulletinBoardService;
//
//  @Mock
//  private UserProfileService userProfileService;
//
//  @Mock
//  private UserRepositoryImpl userRepository;
//
//  @Mock
//  private RoomRepositoryImpl roomRepository;
//
//  @Mock
//  private RoomsRequestRepositoryImpl roomsRequestRepository;
//
//  @Mock
//  private NotificationService notificationService;
//
//  private RoomControllerImpl roomController;
//  private UserControllerImpl userController;
//
//  // Тестовые данные
//  private String ownerToken = "owner_token_123";
//  private String adminToken = "admin_token_123";
//  private String participantToken = "participant_token_123";
//  private String targetUserToken = "target_token_123";
//
//  private Integer ownerId = 1;
//  private Integer adminId = 2;
//  private Integer participantId = 3;
//  private Integer targetUserId = 4;
//
//  private Integer roomId = 100;
//  private Integer requestId = 200;
//
//  private User owner;
//  private User admin;
//  private User participant;
//  private User targetUser;
//
//  private Room testRoom;
//
//  @BeforeEach
//  void setUp() {
//    // Инициализация контроллеров с моками
//    roomController = new RoomControllerImpl(
//      roomService,
//      tokenService,
//      userWithRoomService,
//      bulletinBoardService
//    );
//
//    userController = new UserControllerImpl(
//      userProfileService,
//      tokenService,
//      userWithRoomService,
//      mock(JoinRequestService.class)
//    );
//
//    // Создание тестовых пользователей
//    owner = createTestUser(ownerId, "owner_user", "Owner User");
//    admin = createTestUser(adminId, "admin_user", "Admin User");
//    participant = createTestUser(participantId, "participant_user", "Participant User");
//    targetUser = createTestUser(targetUserId, "target_user", "Target User");
//
//    // Создание тестовой комнаты
//    testRoom = createTestRoom(roomId, "Test Room", Category.SPORT);
//
//    // Настройка связей пользователей с комнатой
//    Map<User, Role> roomUsers = new HashMap<>();
//    roomUsers.put(owner, Role.OWNER);
//    roomUsers.put(admin, Role.ADMIN);
//    roomUsers.put(participant, Role.PARTICIPANT);
//    testRoom.setUsers(roomUsers);
//
//    // Список банов (пустой)
//    testRoom.setBans(new ArrayList<>());
//  }
//
//  private User createTestUser(Integer id, String login, String username) {
//    return new User(
//      id,
//      login,
//      username,
//      "hashed_password",
//      Instant.now().minus(365 * 20, ChronoUnit.DAYS),
//      "Test Country",
//      "Test City",
//      "Test Description",
//      1,
//      new ArrayList<>(),
//      new ArrayList<>()
//    );
//  }
//
//  private Room createTestRoom(Integer id, String name, Category category) {
//    return new Room(
//      id,
//      true,
//      false,
//      "https://chat.test.com/room",
//      category,
//      name,
//      "Test Room Description",
//      Instant.now().plus(1, ChronoUnit.DAYS),
//      Instant.now().plus(2, ChronoUnit.DAYS),
//      16,
//      null,
//      10,
//      new HashMap<>(),
//      new ArrayList<>()
//    );
//  }
//
//  private void setupTokenValidation(String token, Integer userId) {
//    when(tokenService.isTokenActive(token)).thenReturn(true);
//    when(tokenService.decodeToken(token)).thenReturn(new TokenPayload(userId, Instant.now().plus(30, ChronoUnit.MINUTES)));
//  }
//
//  private void setupUserRepositoryMocks() {
//    when(userRepository.getUserById(ownerId)).thenReturn(owner);
//    when(userRepository.getUserById(adminId)).thenReturn(admin);
//    when(userRepository.getUserById(participantId)).thenReturn(participant);
//    when(userRepository.getUserById(targetUserId)).thenReturn(targetUser);
//  }
//
//  private void setupRoomRepositoryMocks() {
//    when(roomRepository.getRoomById(roomId)).thenReturn(testRoom);
//    when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
//    when(roomRepository.isUserInMembers(roomId, adminId)).thenReturn(true);
//    when(roomRepository.isUserInMembers(roomId, participantId)).thenReturn(true);
//    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(false);
//
//    when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
//    when(roomRepository.getUserRoleByRoomId(roomId, adminId)).thenReturn(Role.ADMIN);
//    when(roomRepository.getUserRoleByRoomId(roomId, participantId)).thenReturn(Role.PARTICIPANT);
//  }
//
//  @Test
//  void testUS501_ViewRoomParticipants_AsRoomAdmin_Success() {
//    // US-501: Просмотр участников комнаты как администратор
//
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    List<RoomParticipantResponse> expectedParticipants = Arrays.asList(
//      new RoomParticipantResponse(
//        ownerId, "Owner User", owner.getDataOfBirth(),
//        "Test City", "Test Country", 1, "Test Description", Role.OWNER
//      ),
//      new RoomParticipantResponse(
//        adminId, "Admin User", admin.getDataOfBirth(),
//        "Test City", "Test Country", 1, "Test Description", Role.ADMIN
//      ),
//      new RoomParticipantResponse(
//        participantId, "Participant User", participant.getDataOfBirth(),
//        "Test City", "Test Country", 1, "Test Description", Role.PARTICIPANT
//      )
//    );
//
//    when(userWithRoomService.getRoomParticipants(adminId, roomId, Role.PARTICIPANT))
//      .thenReturn(expectedParticipants);
//
//    // Act
//    ResponseEntity<List<RoomParticipantResponse>> response =
//      roomController.getRoomParticipants("Bearer " + adminToken, roomId, Role.PARTICIPANT);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertNotNull(response.getBody());
//    assertEquals(3, response.getBody().size());
//
//    // Проверяем, что фильтрация по роли работает
//    verify(userWithRoomService).getRoomParticipants(adminId, roomId, Role.PARTICIPANT);
//
//    // Проверяем, что у всех участников правильные данные
//    RoomParticipantResponse participantResponse = response.getBody().get(2);
//    assertEquals(participantId, participantResponse.getId());
//    assertEquals("Participant User", participantResponse.getUserName());
//    assertEquals(Role.PARTICIPANT, participantResponse.getRole());
//  }
//
//  @Test
//  void testUS501_ViewRoomParticipants_AsRoomAdmin_NoParticipants() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    when(userWithRoomService.getRoomParticipants(adminId, roomId, null))
//      .thenReturn(Collections.emptyList());
//
//    // Act
//    ResponseEntity<List<RoomParticipantResponse>> response =
//      roomController.getRoomParticipants("Bearer " + adminToken, roomId, null);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertTrue(response.getBody().isEmpty());
//  }
//
//  @Test
//  void testUS501_ViewRoomParticipants_Unauthorized() {
//    // Arrange
//    when(tokenService.isTokenActive(anyString())).thenReturn(false);
//
//    // Act & Assert
//    assertThrows(com.coactivity.service.exception.TokenValidationException.class, () -> {
//      roomController.getRoomParticipants("InvalidToken", roomId, null);
//    });
//  }
//
//  @Test
//  void testUS502_VerifyUserMembership_AsRoomAdmin_Success() {
//    // US-502: Проверка членства пользователя в комнате
//
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    UserSummaryResponse userSummary = new UserSummaryResponse();
//    userSummary.setId(targetUserId);
//    userSummary.setUserName("Target User");
//
//    MembershipVerificationResponse expectedResponse = new MembershipVerificationResponse(
//      false,  // не является участником
//      null,   // нет роли
//      userSummary,
//      "Test Room"
//    );
//
//    when(userWithRoomService.verifyUserMembership(adminId, roomId, targetUserId))
//      .thenReturn(expectedResponse);
//
//    // Act
//    ResponseEntity<MembershipVerificationResponse> response =
//      roomController.isUserInRoom("Bearer " + adminToken, roomId, targetUserId);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertNotNull(response.getBody());
//    assertFalse(response.getBody().isMember());
//    assertEquals("Target User", response.getBody().getUser().getUserName());
//    assertEquals("Test Room", response.getBody().getRoomName());
//  }
//
//  @Test
//  void testUS502_VerifyUserMembership_UserIsMember() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    UserSummaryResponse userSummary = new UserSummaryResponse();
//    userSummary.setId(participantId);
//    userSummary.setUserName("Participant User");
//
//    MembershipVerificationResponse expectedResponse = new MembershipVerificationResponse(
//      true,  // является участником
//      Role.PARTICIPANT,
//      userSummary,
//      "Test Room"
//    );
//
//    when(userWithRoomService.verifyUserMembership(adminId, roomId, participantId))
//      .thenReturn(expectedResponse);
//
//    // Act
//    ResponseEntity<MembershipVerificationResponse> response =
//      roomController.isUserInRoom("Bearer " + adminToken, roomId, participantId);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertTrue(response.getBody().isMember());
//    assertEquals(Role.PARTICIPANT, response.getBody().getRole());
//    assertEquals("Participant User", response.getBody().getUser().getUserName());
//  }
//
//  @Test
//  void testUS502_VerifyUserMembership_NotRoomAdmin() {
//    // Arrange
//    setupTokenValidation("Bearer " + participantToken, participantId);
//
//    when(userWithRoomService.verifyUserMembership(participantId, roomId, targetUserId))
//      .thenThrow(new AuthorizationException("Insufficient privileges for room " + roomId));
//
//    // Act & Assert
//    assertThrows(AuthorizationException.class, () -> {
//      userWithRoomService.verifyUserMembership(participantId, roomId, targetUserId);
//    });
//  }
//
//  @Test
//  void testUS503_PromoteUserToAdmin_AsRoomOwner_Success() {
//    // US-503: Назначение пользователя администратором как владелец комнаты
//
//    // Arrange
//    setupTokenValidation("Bearer " + ownerToken, ownerId);
//    setupUserRepositoryMocks();
//    setupRoomRepositoryMocks();
//
//    // Сначала добавляем targetUser как участника
//    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(true);
//    when(roomRepository.getUserRoleByRoomId(roomId, targetUserId)).thenReturn(Role.PARTICIPANT);
//
//    RoleAssignmentResponse expectedResponse = new RoleAssignmentResponse(
//      targetUserId,
//      roomId,
//      Role.ADMIN,
//      Role.PARTICIPANT,
//      ownerId
//    );
//
//    when(userWithRoomService.assignAdminRole(ownerId, roomId, targetUserId))
//      .thenReturn(expectedResponse);
//
//    // Act
//    ResponseEntity<RoleAssignmentResponse> response =
//      userController.assignAdminRole("Bearer " + ownerToken, roomId, targetUserId);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertNotNull(response.getBody());
//    assertEquals(targetUserId, response.getBody().getUserId());
//    assertEquals(roomId, response.getBody().getRoomId());
//    assertEquals(Role.ADMIN, response.getBody().getNewRole());
//    assertEquals(Role.PARTICIPANT, response.getBody().getPreviousRole());
//    assertEquals(ownerId, response.getBody().getAssignedBy());
//
//    verify(userWithRoomService).assignAdminRole(ownerId, roomId, targetUserId);
//  }
//
//  @Test
//  void testUS503_PromoteUserToAdmin_NotOwner() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    when(userWithRoomService.assignAdminRole(adminId, roomId, targetUserId))
//      .thenThrow(new AuthorizationException("Only room owner can perform this action"));
//
//    // Act & Assert
//    assertThrows(AuthorizationException.class, () -> {
//      userWithRoomService.assignAdminRole(adminId, roomId, targetUserId);
//    });
//  }
//
//  @Test
//  void testUS503_PromoteUserToAdmin_UserNotInRoom() {
//    // Arrange
//    setupTokenValidation("Bearer " + ownerToken, ownerId);
//    setupUserRepositoryMocks();
//
//    when(roomRepository.isUserInMembers(roomId, targetUserId)).thenReturn(false);
//
//    when(userWithRoomService.assignAdminRole(ownerId, roomId, targetUserId))
//      .thenThrow(new ValidationException("Target user is not a member of the room"));
//
//    // Act & Assert
//    assertThrows(ValidationException.class, () -> {
//      userWithRoomService.assignAdminRole(ownerId, roomId, targetUserId);
//    });
//  }
//
//  @Test
//  void testUS504_DemoteAdmin_AsRoomOwner_Success() {
//    // US-504: Понижение администратора до участника как владелец комнаты
//
//    // Arrange
//    setupTokenValidation("Bearer " + ownerToken, ownerId);
//    setupUserRepositoryMocks();
//
//    RoleAssignmentResponse expectedResponse = new RoleAssignmentResponse(
//      adminId,
//      roomId,
//      Role.PARTICIPANT,
//      Role.ADMIN,
//      ownerId
//    );
//
//    when(userWithRoomService.demoteAdminRole(ownerId, roomId, adminId))
//      .thenReturn(expectedResponse);
//
//    // Act
//    ResponseEntity<RoleAssignmentResponse> response =
//      userController.demoteAdminRole("Bearer " + ownerToken, roomId, adminId);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertNotNull(response.getBody());
//    assertEquals(adminId, response.getBody().getUserId());
//    assertEquals(Role.PARTICIPANT, response.getBody().getNewRole());
//    assertEquals(Role.ADMIN, response.getBody().getPreviousRole());
//
//    verify(userWithRoomService).demoteAdminRole(ownerId, roomId, adminId);
//  }
//
//  @Test
//  void testUS504_DemoteAdmin_NotOwner() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    when(userWithRoomService.demoteAdminRole(adminId, roomId, participantId))
//      .thenThrow(new AuthorizationException("Only room owner can perform this action"));
//
//    // Act & Assert
//    assertThrows(AuthorizationException.class, () -> {
//      userWithRoomService.demoteAdminRole(adminId, roomId, participantId);
//    });
//  }
//
//  @Test
//  void testUS504_DemoteAdmin_UserNotAdmin() {
//    // Arrange
//    setupTokenValidation("Bearer " + ownerToken, ownerId);
//    setupUserRepositoryMocks();
//
//    when(userWithRoomService.demoteAdminRole(ownerId, roomId, participantId))
//      .thenThrow(new ValidationException("User is not an admin in this room"));
//
//    // Act & Assert
//    assertThrows(ValidationException.class, () -> {
//      userWithRoomService.demoteAdminRole(ownerId, roomId, participantId);
//    });
//  }
//
//  @Test
//  void testUS505_UpdateBulletinBoard_AsRoomAdmin_Success() {
//    // US-505: Обновление доски объявлений как администратор комнаты
//
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    String newContent = "Important announcement: Meeting scheduled for tomorrow at 3 PM";
//
//    BulletinBoardResponse expectedResponse = new BulletinBoardResponse(
//      1,
//      newContent,
//      createUserSummary(admin),
//      Instant.now()
//    );
//
//    when(bulletinBoardService.updateBulletinBoard(roomId, newContent, adminId))
//      .thenReturn(expectedResponse);
//
//    // Act
//    ResponseEntity<BulletinBoardResponse> response =
//      roomController.updateBulletinBoard("Bearer " + adminToken, roomId, newContent);
//
//    // Assert
//    assertEquals(200, response.getStatusCodeValue());
//    assertNotNull(response.getBody());
//    assertEquals(newContent, response.getBody().getContent());
//    assertEquals(adminId, response.getBody().getAuthor().getId());
//    assertEquals("Admin User", response.getBody().getAuthor().getUserName());
//
//    verify(bulletinBoardService).updateBulletinBoard(roomId, newContent, adminId);
//  }
//
//  @Test
//  void testUS505_UpdateBulletinBoard_EmptyContent() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    // Act & Assert
//    assertThrows(ValidationException.class, () -> {
//      roomController.updateBulletinBoard("Bearer " + adminToken, roomId, "");
//    });
//  }
//
//  @Test
//  void testUS505_UpdateBulletinBoard_NotAuthorized() {
//    // Arrange
//    setupTokenValidation("Bearer " + participantToken, participantId);
//
//    when(bulletinBoardService.updateBulletinBoard(roomId, "Test content", participantId))
//      .thenThrow(new AuthorizationException("User is not authorized to update bulletin board"));
//
//    // Act & Assert
//    assertThrows(AuthorizationException.class, () -> {
//      bulletinBoardService.updateBulletinBoard(roomId, "Test content", participantId);
//    });
//  }
//
//  @Test
//  void testUS505_DeleteBulletinBoard_AsRoomAdmin_Success() {
//    // US-505: Удаление доски объявлений как администратор комнаты
//
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    doNothing().when(bulletinBoardService).deleteBulletinBoard(roomId);
//
//    // Act
//    ResponseEntity<Void> response =
//      roomController.deleteBulletinBoard("Bearer " + adminToken, roomId);
//
//    // Assert
//    assertEquals(204, response.getStatusCodeValue()); // No Content
//    assertNull(response.getBody());
//
//    verify(bulletinBoardService).deleteBulletinBoard(roomId);
//  }
//
//  @Test
//  void testUS505_DeleteBulletinBoard_BulletinNotFound() {
//    // Arrange
//    setupTokenValidation("Bearer " + adminToken, adminId);
//
//    doThrow(new ResourceNotFoundException("Bulletin board not found for room: " + roomId))
//      .when(bulletinBoardService).deleteBulletinBoard(roomId);
//
//    // Act & Assert
//    assertThrows(ResourceNotFoundException.class, () -> {
//      bulletinBoardService.deleteBulletinBoard(roomId);
//    });
//  }
//
//  @Test
//  void testFullUserStoryFlow_FromRegistrationToAdminManagement() {
//    // Полный тестовый сценарий: от регистрации до управления админами
//
//    // 1. Регистрация нового пользователя
//    UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
//    registrationRequest.setLogin("new_user_" + System.currentTimeMillis());
//    registrationRequest.setUserName("New Test User");
//    registrationRequest.setPassword("Password123!");
//    registrationRequest.setDateOfBirth(Instant.now().minus(365 * 25, ChronoUnit.DAYS));
//    registrationRequest.setCity("New City");
//    registrationRequest.setCountry("New Country");
//    registrationRequest.setDescription("New user for testing");
//    registrationRequest.setAvatarId(2);
//
//    when(userProfileService.registerUser(any(UserRegistrationRequest.class)))
//      .thenReturn(new RegistrationResponse(999, "New Test User"));
//
//    RegistrationResponse registrationResponse = userProfileService.registerUser(registrationRequest);
//    assertEquals(999, registrationResponse.getUserId());
//    assertEquals("New Test User", registrationResponse.getUserName());
//
//    // 2. Создание комнаты новым пользователем
//    RoomCreationRequest roomRequest = new RoomCreationRequest();
//    roomRequest.setName("Management Test Room");
//    roomRequest.setDescription("Room for testing admin management");
//    roomRequest.setCategoryId(Category.BUSINESS.ordinal());
//    roomRequest.setIsPublic(false);
//    roomRequest.setChatLink("https://chat.test.com/management");
//    roomRequest.setAgeRating(18);
//    roomRequest.setMaximumNumberOfPeople(15);
//
//    RoomCreationResponse roomCreationResponse = new RoomCreationResponse(
//      888,
//      "Management Test Room",
//      Category.BUSINESS,
//      Instant.now()
//    );
//
//    when(roomService.createRoom(999, roomRequest))
//      .thenReturn(roomCreationResponse);
//
//    RoomCreationResponse createdRoom = roomService.createRoom(999, roomRequest);
//    assertEquals(888, createdRoom.getRoomId());
//    assertEquals("Management Test Room", createdRoom.getRoomName());
//    assertEquals(Category.BUSINESS, createdRoom.getCategory());
//
//    // 3. Владелец добавляет пользователя как администратора
//    RoleAssignmentResponse promotionResponse = new RoleAssignmentResponse(
//      targetUserId,
//      888,
//      Role.ADMIN,
//      Role.PARTICIPANT,
//      999
//    );
//
//    when(userWithRoomService.assignAdminRole(999, 888, targetUserId))
//      .thenReturn(promotionResponse);
//
//    RoleAssignmentResponse promotionResult = userWithRoomService.assignAdminRole(999, 888, targetUserId);
//    assertEquals(Role.ADMIN, promotionResult.getNewRole());
//    assertEquals(999, promotionResult.getAssignedBy());
//
//    // 4. Новый администратор проверяет участников комнаты
//    List<RoomParticipantResponse> participants = Arrays.asList(
//      new RoomParticipantResponse(999, "New Test User", null, "New City", "New Country", 2, "New user for testing", Role.OWNER),
//      new RoomParticipantResponse(targetUserId, "Target User", null, "Test City", "Test Country", 1, "Test Description", Role.ADMIN)
//    );
//
//    when(userWithRoomService.getRoomParticipants(targetUserId, 888, null))
//      .thenReturn(participants);
//
//    List<RoomParticipantResponse> roomParticipants = userWithRoomService.getRoomParticipants(targetUserId, 888, null);
//    assertEquals(2, roomParticipants.size());
//    assertEquals(Role.OWNER, roomParticipants.get(0).getRole());
//    assertEquals(Role.ADMIN, roomParticipants.get(1).getRole());
//
//    // 5. Администратор обновляет доску объявлений
//    BulletinBoardResponse bulletinResponse = new BulletinBoardResponse(
//      1,
//      "Welcome to Management Test Room!",
//      createUserSummary(targetUser),
//      Instant.now()
//    );
//
//    when(bulletinBoardService.updateBulletinBoard(888, "Welcome to Management Test Room!", targetUserId))
//      .thenReturn(bulletinResponse);
//
//    BulletinBoardResponse updatedBulletin = bulletinBoardService.updateBulletinBoard(
//      888, "Welcome to Management Test Room!", targetUserId
//    );
//    assertEquals("Welcome to Management Test Room!", updatedBulletin.getContent());
//    assertEquals(targetUserId, updatedBulletin.getAuthor().getId());
//
//    // 6. Владелец проверяет успешность операций
//    verify(userProfileService, atLeastOnce()).registerUser(any(UserRegistrationRequest.class));
//    verify(roomService, atLeastOnce()).createRoom(anyInt(), any(RoomCreationRequest.class));
//    verify(userWithRoomService, atLeastOnce()).assignAdminRole(anyInt(), anyInt(), anyInt());
//    verify(userWithRoomService, atLeastOnce()).getRoomParticipants(anyInt(), anyInt(), any());
//    verify(bulletinBoardService, atLeastOnce()).updateBulletinBoard(anyInt(), anyString(), anyInt());
//  }
//
//  private UserSummaryResponse createUserSummary(User user) {
//    UserSummaryResponse summary = new UserSummaryResponse();
//    summary.setId(user.getId());
//    summary.setUserName(user.getUserName());
//    summary.setDateOfBirth(user.getDataOfBirth());
//    summary.setCity(user.getCity());
//    summary.setCountry(user.getCountry());
//    summary.setDescription(user.getDescription());
//    summary.setAvatarId(user.getAvatarId());
//    return summary;
//  }
//
//  @Test
//  void testUserLeavesRoom_AdminTakesOverManagement() {
//    // Дополнительный тест: когда владелец уходит, администратор берет на себя управление
//
//    // Arrange
//    setupTokenValidation("Bearer " + ownerToken, ownerId);
//    setupUserRepositoryMocks();
//
//    // Владелец назначает администратора
//    RoleAssignmentResponse promotionResponse = new RoleAssignmentResponse(
//      adminId,
//      roomId,
//      Role.ADMIN,
//      Role.PARTICIPANT,
//      ownerId
//    );
//
//    when(userWithRoomService.assignAdminRole(ownerId, roomId, adminId))
//      .thenReturn(promotionResponse);
//
//    // Act - владелец назначает администратора
//    RoleAssignmentResponse promotionResult = userWithRoomService.assignAdminRole(ownerId, roomId, adminId);
//
//    // Assert
//    assertEquals(Role.ADMIN, promotionResult.getNewRole());
//
//    // Теперь администратор может управлять доской объявлений
//    BulletinBoardResponse bulletinResponse = new BulletinBoardResponse(
//      1,
//      "Admin announcement: Room will be temporarily closed",
//      createUserSummary(admin),
//      Instant.now()
//    );
//
//    when(bulletinBoardService.updateBulletinBoard(roomId, "Admin announcement: Room will be temporarily closed", adminId))
//      .thenReturn(bulletinResponse);
//
//    BulletinBoardResponse updatedBulletin = bulletinBoardService.updateBulletinBoard(
//      roomId, "Admin announcement: Room will be temporarily closed", adminId
//    );
//
//    assertEquals(adminId, updatedBulletin.getAuthor().getId());
//    assertTrue(updatedBulletin.getContent().contains("Admin announcement"));
//
//    verify(userWithRoomService).assignAdminRole(ownerId, roomId, adminId);
//    verify(bulletinBoardService).updateBulletinBoard(roomId, anyString(), eq(adminId));
//  }
//}