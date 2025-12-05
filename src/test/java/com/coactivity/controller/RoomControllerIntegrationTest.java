//package com.coactivity.controller;
//
//import com.coactivity.DataRepository;
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
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//class RoomControllerIntegrationTest {
//
//  @Autowired
//  private RoomControllerImpl roomController;
//
//  @Autowired
//  private UserControllerImpl userController;
//
//  @Autowired
//  private UserProfileService userProfileService;
//
//  @Autowired
//  private RoomService roomService;
//
//  @Autowired
//  private UserWithRoomService userWithRoomService;
//
//  @Autowired
//  private TokenService tokenService;
//
//  @Autowired
//  private UserRepositoryImpl userRepository;
//
//  @Autowired
//  private RoomRepositoryImpl roomRepository;
//
//  @Autowired
//  private RoomsRequestRepositoryImpl roomsRequestRepository;
//
//  @Autowired
//  private DataRepository dataRepository;
//
//  // Тестовые данные
//  private String ownerToken;
//  private String adminToken;
//  private String participantToken;
//
//  private Integer ownerId;
//  private Integer adminId;
//  private Integer participantId;
//
//  private Integer roomId;
//  private String uniqueRoomName;
//  private String uniqueOwnerLogin;
//  private String uniqueAdminLogin;
//  private String uniqueParticipantLogin;
//
//  @BeforeEach
//  void setUp() {
//    System.out.println("=== SETUP START ===");
//
//    // Проверяем, что DataRepository работает
//    checkDataRepositoryDirectly();
//
//    // Генерируем уникальные имена
//    String timestamp = String.valueOf(System.currentTimeMillis());
//    String uuid = UUID.randomUUID().toString().substring(0, 8);
//    String testId = timestamp + "_" + uuid;
//
//    uniqueOwnerLogin = "owner_" + testId;
//    uniqueAdminLogin = "admin_" + testId;
//    uniqueParticipantLogin = "participant_" + testId;
//    uniqueRoomName = "Test Room " + testId;
//
//    System.out.println("Creating test users...");
//
//    // Создаем тестовых пользователей
//    ownerId = createTestUser(uniqueOwnerLogin, "Owner User");
//    adminId = createTestUser(uniqueAdminLogin, "Admin User");
//    participantId = createTestUser(uniqueParticipantLogin, "Participant User");
//
//    // Получаем токены
//    ownerToken = loginUser(uniqueOwnerLogin, "Test@123");
//    adminToken = loginUser(uniqueAdminLogin, "Test@123");
//    participantToken = loginUser(uniqueParticipantLogin, "Test@123");
//
//    System.out.println("Creating test room...");
//
//    // Создаем комнату
//    roomId = createTestRoom(ownerToken, uniqueRoomName, Category.SPORT, 10);
//    System.out.println("Room created: " + roomId);
//
//    // Владелец автоматически добавляется в комнату при создании
//    System.out.println("Owner is automatically added to room as owner");
//
//    // Проверяем, что владелец действительно в комнате
//    verifyUserInRoomDirectly(roomId, ownerId, "OWNER");
//
//    // Добавляем админа и участника через старый подход (запрос на вступление)
//    // чтобы избежать проблем с новым методом addUserToRoom
//    System.out.println("Adding admin and participant via join requests...");
//    addUserViaJoinRequest(adminId, roomId, ownerToken, Role.ADMIN);
//    addUserViaJoinRequest(participantId, roomId, ownerToken, Role.PARTICIPANT);
//
//    System.out.println("=== SETUP COMPLETE ===");
//  }
//
//  @AfterEach
//  void cleanUp() {
//    System.out.println("=== CLEANUP START ===");
//
//    // Очищаем данные
//    cleanupTestData();
//
//    System.out.println("=== CLEANUP COMPLETE ===");
//  }
//
//  /**
//   * Проверяем DataRepository напрямую
//   */
//  private void checkDataRepositoryDirectly() {
//    try (Connection connection = dataRepository.getDataSource().getConnection()) {
//      DatabaseMetaData metaData = connection.getMetaData();
//
//      System.out.println("=== DIRECT DATAREPOSITORY CONNECTION ===");
//      System.out.println("URL: " + metaData.getURL());
//      System.out.println("User: " + metaData.getUserName());
//      System.out.println("Database: " + metaData.getDatabaseProductName());
//      System.out.println("========================================");
//
//    } catch (SQLException e) {
//      System.err.println("ERROR: Direct DataRepository connection failed");
//      System.err.println("Message: " + e.getMessage());
//      throw new RuntimeException("DataRepository is not working", e);
//    }
//  }
//
//  /**
//   * Очищаем тестовые данные
//   */
//  private void cleanupTestData() {
//    System.out.println("Cleaning up test data...");
//
//    try {
//      // Удаляем комнату
//      if (roomId != null) {
//        roomRepository.deleteRoom(roomId);
//        System.out.println("✓ Room deleted: " + roomId);
//      }
//
//      // Удаляем пользователей
//      deleteUserIfExists(ownerId);
//      deleteUserIfExists(adminId);
//      deleteUserIfExists(participantId);
//
//    } catch (Exception e) {
//      System.err.println("Error during cleanup: " + e.getMessage());
//      e.printStackTrace();
//    }
//  }
//
//  private void deleteUserIfExists(Integer userId) {
//    if (userId == null) return;
//
//    try {
//      // Проверяем, существует ли пользователь
//      User user = userRepository.getUserById(userId);
//      if (user != null) {
//        // Удаляем пользователя через репозиторий или прямой запрос
//        deleteUserViaDirectQuery(userId);
//      }
//    } catch (Exception e) {
//      System.err.println("Error deleting user " + userId + ": " + e.getMessage());
//    }
//  }
//
//  private void deleteUserViaDirectQuery(Integer userId) {
//    try (Connection connection = dataRepository.getDataSource().getConnection()) {
//      connection.setAutoCommit(false);
//
//      try {
//        // Удаляем зависимости в правильном порядке
//        String[] queries = {
//          "DELETE FROM Bans WHERE user_id = ?",
//          "DELETE FROM Rooms_members WHERE user_id = ?",
//          "DELETE FROM rooms_requests WHERE user_id = ?",
//          "DELETE FROM Users WHERE id = ?"
//        };
//
//        for (String query : queries) {
//          try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setInt(1, userId);
//            stmt.executeUpdate();
//          }
//        }
//
//        connection.commit();
//        System.out.println("✓ User deleted: " + userId);
//
//      } catch (SQLException e) {
//        connection.rollback();
//        // Игнорируем ошибки, если данные уже удалены
//        if (!e.getMessage().contains("does not exist")) {
//          throw e;
//        }
//      }
//
//    } catch (SQLException e) {
//      System.err.println("Error in deleteUserViaDirectQuery for user " + userId + ": " + e.getMessage());
//    }
//  }
//
//  /**
//   * Создаем пользователя
//   */
//  private Integer createTestUser(String login, String userName) {
//    try {
//      UserRegistrationRequest request = new UserRegistrationRequest();
//      request.setLogin(login);
//      request.setUserName(userName);
//      request.setPassword("Test@123");
//      request.setDateOfBirth(Instant.now().minus(365 * 20, ChronoUnit.DAYS));
//      request.setCity("Test City");
//      request.setCountry("Test Country");
//      request.setDescription("Test Description for " + userName);
//      request.setAvatarId(1);
//
//      RegistrationResponse response = userProfileService.registerUser(request);
//
//      // Получаем ID созданного пользователя
//      User user = userRepository.getUser(login, "Test@123");
//      if (user == null) {
//        throw new RuntimeException("User was not created in database");
//      }
//
//      System.out.println("✓ User created: " + userName + " (ID: " + user.getId() + ")");
//      return user.getId();
//
//    } catch (Exception e) {
//      System.err.println("Failed to create test user: " + e.getMessage());
//      e.printStackTrace();
//      throw new RuntimeException("Failed to create test user", e);
//    }
//  }
//
//  private String loginUser(String login, String password) {
//    try {
//      User user = userRepository.getUser(login, password);
//      if (user == null) {
//        throw new RuntimeException("User not found for login: " + login);
//      }
//
//      String token = tokenService.createToken(user.getId());
//      tokenService.registerToken(user.getId(), token);
//
//      System.out.println("✓ User logged in: " + login);
//      return token;
//    } catch (Exception e) {
//      System.err.println("Failed to login user: " + e.getMessage());
//      throw new RuntimeException("Failed to login user", e);
//    }
//  }
//
//  private Integer createTestRoom(String token, String name, Category category, int maxPeople) {
//    try {
//      RoomCreationRequest request = new RoomCreationRequest();
//      request.setName(name);
//      request.setDescription("Test Room Description");
//      request.setCategoryId(category.ordinal() + 1);
//      request.setIsPublic(false);
//      request.setChatLink("https://chat.test.com/room");
//      request.setAgeRating(16);
//      request.setMaximumNumberOfPeople(maxPeople);
//      request.setDateOfStartEvent(Instant.now().plus(1, ChronoUnit.DAYS));
//      request.setDateOfEndEvent(Instant.now().plus(2, ChronoUnit.DAYS));
//
//      Integer userId = tokenService.decodeToken(token).userId();
//
//      RoomCreationResponse response = roomService.createRoom(userId, request);
//
//      System.out.println("✓ Room created: " + name + " (ID: " + response.getRoomId() + ")");
//      return response.getRoomId();
//
//    } catch (Exception e) {
//      System.err.println("Failed to create test room: " + e.getMessage());
//      e.printStackTrace();
//      throw new RuntimeException("Failed to create test room", e);
//    }
//  }
//
//  /**
//   * Проверяем напрямую через DataRepository, что пользователь добавлен в комнату
//   */
//  private void verifyUserInRoomDirectly(Integer roomId, Integer userId, String expectedRole) {
//    try (Connection connection = dataRepository.getDataSource().getConnection();
//         PreparedStatement stmt = connection.prepareStatement(
//           """
//           SELECT r.role FROM Rooms_members rm
//           JOIN Roles r ON rm.role_id = r.id
//           WHERE rm.room_id = ? AND rm.user_id = ?
//           """)) {
//
//      stmt.setInt(1, roomId);
//      stmt.setInt(2, userId);
//
//      try (ResultSet rs = stmt.executeQuery()) {
//        if (rs.next()) {
//          String actualRole = rs.getString("role");
//          System.out.println("✓ Verified: User " + userId + " in room " + roomId + " with role " + actualRole);
//          assertEquals(expectedRole.toUpperCase(), actualRole.toUpperCase(),
//            "User role should be " + expectedRole);
//        } else {
//          System.err.println("✗ ERROR: User " + userId + " NOT found in room " + roomId);
//          throw new RuntimeException("User was not added to room: " + userId);
//        }
//      }
//    } catch (SQLException e) {
//      System.err.println("Error verifying user in room: " + e.getMessage());
//      throw new RuntimeException("Failed to verify user in room", e);
//    }
//  }
//
//  /**
//   * Добавляем пользователя через запрос на вступление (старый подход)
//   */
//  private void addUserViaJoinRequest(Integer userId, Integer roomId, String approverToken, Role role) {
//    try {
//      System.out.println("Adding user " + userId + " to room " + roomId + " via join request...");
//
//      // 1. Пользователь отправляет запрос на вступление
//      userWithRoomService.joinRoom(userId, roomId);
//
//      // 2. Находим ID запроса
//      Integer requestId = findPendingRequestId(userId, roomId);
//      if (requestId == null) {
//        throw new RuntimeException("Could not find join request for user " + userId);
//      }
//
//      // 3. Подтверждаем запрос
//      Integer approverId = tokenService.decodeToken(approverToken).userId();
//      userWithRoomService.approveRequest(approverId, requestId);
//
//      // 4. Если нужно назначить роль ADMIN (не PARTICIPANT)
//      if (role == Role.ADMIN) {
//        userWithRoomService.assignAdminRole(approverId, roomId, userId);
//      }
//
//      // 5. Проверяем, что пользователь добавлен
//      verifyUserInRoomDirectly(roomId, userId, role.name());
//
//      System.out.println("✓ User " + userId + " added to room with role " + role);
//
//    } catch (Exception e) {
//      System.err.println("Failed to add user via join request: " + e.getMessage());
//      e.printStackTrace();
//      throw new RuntimeException("Failed to add user to room", e);
//    }
//  }
//
//  private Integer findPendingRequestId(Integer userId, Integer roomId) {
//    try (Connection connection = dataRepository.getDataSource().getConnection();
//         PreparedStatement stmt = connection.prepareStatement(
//           """
//           SELECT rr.id FROM rooms_requests rr
//           JOIN RequestStatuses rs ON rr.status_id = rs.id
//           WHERE rr.user_id = ? AND rr.room_id = ? AND rs.status_info = 'Consideration'
//           """)) {
//
//      stmt.setInt(1, userId);
//      stmt.setInt(2, roomId);
//
//      try (ResultSet rs = stmt.executeQuery()) {
//        if (rs.next()) {
//          return rs.getInt("id");
//        }
//      }
//    } catch (SQLException e) {
//      System.err.println("Error finding pending request: " + e.getMessage());
//    }
//    return null;
//  }
//
//  // ==================== ТЕСТЫ ====================
//
//  @Test
//  @Order(1)
//  @DisplayName("US-501: View room participants as admin - Success")
//  void testUS501_ViewRoomParticipants_AsRoomAdmin_Success() {
//    System.out.println("=== Running testUS501 ===");
//
//    // Проверяем, что данные есть в базе
//    verifyTestDataInDatabase();
//
//    // Act - админ запрашивает список участников
//    ResponseEntity<List<RoomParticipantResponse>> response =
//      roomController.getRoomParticipants("Bearer " + adminToken, roomId, null);
//
//    // Assert
//    assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
//    assertNotNull(response.getBody(), "Response body should not be null");
//
//    List<RoomParticipantResponse> participants = response.getBody();
//    System.out.println("Found " + participants.size() + " participants in room");
//
//    // Проверяем, что все три пользователя в списке
//    boolean hasOwner = participants.stream().anyMatch(p ->
//      p.getId().equals(ownerId) && p.getRole() == Role.OWNER);
//    boolean hasAdmin = participants.stream().anyMatch(p ->
//      p.getId().equals(adminId) && p.getRole() == Role.ADMIN);
//    boolean hasParticipant = participants.stream().anyMatch(p ->
//      p.getId().equals(participantId) && p.getRole() == Role.PARTICIPANT);
//
//    assertTrue(hasOwner, "Should contain owner");
//    assertTrue(hasAdmin, "Should contain admin");
//    assertTrue(hasParticipant, "Should contain participant");
//
//    System.out.println("✓ testUS501 passed");
//  }
//
//  @Test
//  @Order(2)
//  @DisplayName("US-501: View room participants filtered by role")
//  void testUS501_ViewRoomParticipants_FilteredByRole() {
//    System.out.println("=== Running testUS501 (filtered) ===");
//
//    // Act - запрашиваем только участников (PARTICIPANT)
//    ResponseEntity<List<RoomParticipantResponse>> response =
//      roomController.getRoomParticipants("Bearer " + adminToken, roomId, Role.PARTICIPANT);
//
//    // Assert
//    assertEquals(HttpStatus.OK, response.getStatusCode());
//    assertNotNull(response.getBody());
//
//    List<RoomParticipantResponse> participants = response.getBody();
//    System.out.println("Found " + participants.size() + " participants with role PARTICIPANT");
//
//    // Должен быть только один участник
//    assertEquals(1, participants.size(), "Should have exactly one participant");
//    assertEquals(participantId, participants.get(0).getId(), "Should be the participant user");
//    assertEquals(Role.PARTICIPANT, participants.get(0).getRole(), "Should have role PARTICIPANT");
//
//    System.out.println("✓ testUS501 (filtered) passed");
//  }
//
//  @Test
//  @Order(3)
//  @DisplayName("US-502: Verify user membership - User is member")
//  void testUS502_VerifyUserMembership_UserIsMember() {
//    System.out.println("=== Running testUS502 (user is member) ===");
//
//    // Act - проверяем, что участник в комнате
//    ResponseEntity<MembershipVerificationResponse> response =
//      roomController.isUserInRoom("Bearer " + adminToken, roomId, participantId);
//
//    // Assert
//    assertEquals(HttpStatus.OK, response.getStatusCode());
//    assertNotNull(response.getBody());
//
//    MembershipVerificationResponse verification = response.getBody();
//    assertTrue(verification.getIsMember(), "User should be a member");
//    assertEquals(Role.PARTICIPANT, verification.getRole(), "User should have role PARTICIPANT");
//    assertNotNull(verification.getUserInfo(), "User info should not be null");
//    assertEquals("Participant User", verification.getUserInfo().getUserName(),
//      "User name should match");
//
//    System.out.println("✓ testUS502 (user is member) passed");
//  }
//
//  @Test
//  @Order(4)
//  @DisplayName("US-502: Verify user membership - User not member")
//  void testUS502_VerifyUserMembership_UserNotMember() {
//    System.out.println("=== Running testUS502 (user not member) ===");
//
//    // Создаем нового пользователя, который не в комнате
//    String newUserLogin = "nonmember_" + System.currentTimeMillis();
//    Integer newUserId = createTestUser(newUserLogin, "Non-Member User");
//
//    try {
//      // Act - проверяем, что новый пользователь НЕ в комнате
//      ResponseEntity<MembershipVerificationResponse> response =
//        roomController.isUserInRoom("Bearer " + adminToken, roomId, newUserId);
//
//      // Assert
//      assertEquals(HttpStatus.OK, response.getStatusCode());
//      assertNotNull(response.getBody());
//
//      MembershipVerificationResponse verification = response.getBody();
//      assertFalse(verification.getIsMember(), "User should NOT be a member");
//      assertNull(verification.getRole(), "Role should be null for non-member");
//      assertNotNull(verification.getUserInfo(), "User info should not be null");
//      assertEquals("Non-Member User", verification.getUserInfo().getUserName());
//
//      System.out.println("✓ testUS502 (user not member) passed");
//
//    } finally {
//      // Очищаем созданного пользователя
//      deleteUserIfExists(newUserId);
//    }
//  }
//
//  @Test
//  @Order(5)
//  @DisplayName("US-503: Promote user to admin as room owner")
//  void testUS503_PromoteUserToAdmin_AsRoomOwner_Success() {
//    System.out.println("=== Running testUS503 ===");
//
//    // Создаем нового пользователя для теста
//    String newUserLogin = "promotetest_" + System.currentTimeMillis();
//    Integer newUserId = createTestUser(newUserLogin, "User to Promote");
//
//    try {
//      // Добавляем пользователя в комнату как участника
//      addUserViaJoinRequest(newUserId, roomId, ownerToken, Role.PARTICIPANT);
//
//      // Проверяем, что пользователь добавлен с ролью PARTICIPANT
//      verifyUserInRoomDirectly(roomId, newUserId, "PARTICIPANT");
//
//      // Act - владелец повышает пользователя до админа
//      ResponseEntity<RoleAssignmentResponse> response =
//        userController.assignAdminRole("Bearer " + ownerToken, roomId, newUserId);
//
//      // Assert
//      assertEquals(HttpStatus.OK, response.getStatusCode());
//      assertNotNull(response.getBody());
//
//      RoleAssignmentResponse assignment = response.getBody();
//      assertEquals(newUserId, assignment.getUserId(), "User ID should match");
//      assertEquals(roomId, assignment.getRoomId(), "Room ID should match");
//      assertEquals(Role.ADMIN, assignment.getNewRole(), "New role should be ADMIN");
//
//      // Проверяем, что роль действительно изменилась
//      verifyUserInRoomDirectly(roomId, newUserId, "ADMIN");
//
//      System.out.println("✓ testUS503 passed");
//
//    } finally {
//      // Очищаем созданного пользователя
//      deleteUserIfExists(newUserId);
//    }
//  }
//
//  @Test
//  @Order(6)
//  @DisplayName("US-504: Demote admin as room owner")
//  void testUS504_DemoteAdmin_AsRoomOwner_Success() {
//    System.out.println("=== Running testUS504 ===");
//
//    // Arrange - админ уже есть в комнате с ролью ADMIN
//    verifyUserInRoomDirectly(roomId, adminId, "ADMIN");
//
//    // Act - владелец понижает админа до участника
//    ResponseEntity<RoleAssignmentResponse> response =
//      userController.demoteAdminRole("Bearer " + ownerToken, roomId, adminId);
//
//    // Assert
//    assertEquals(HttpStatus.OK, response.getStatusCode());
//    assertNotNull(response.getBody());
//
//    RoleAssignmentResponse assignment = response.getBody();
//    assertEquals(adminId, assignment.getUserId(), "User ID should match");
//    assertEquals(roomId, assignment.getRoomId(), "Room ID should match");
//    assertEquals(Role.PARTICIPANT, assignment.getNewRole(), "New role should be PARTICIPANT");
//
//    // Проверяем, что роль действительно изменилась
//    verifyUserInRoomDirectly(roomId, adminId, "PARTICIPANT");
//
//    System.out.println("✓ testUS504 passed");
//  }
//
//  @Test
//  @Order(7)
//  @DisplayName("Test basic RoomRepository operations")
//  void testRoomRepositoryBasicOperations() {
//    System.out.println("=== Testing RoomRepository basic operations ===");
//
//    // 1. Проверяем получение комнаты по ID
//    Room room = roomRepository.getRoomById(roomId);
//    assertNotNull(room, "Room should not be null");
//    assertEquals(roomId, room.getId(), "Room ID should match");
//    assertEquals(uniqueRoomName, room.getName(), "Room name should match");
//
//    // 2. Проверяем, что пользователи действительно в комнате через RoomRepository
//    assertTrue(roomRepository.isUserInMembers(roomId, ownerId), "Owner should be in room");
//    assertTrue(roomRepository.isUserInMembers(roomId, adminId), "Admin should be in room");
//    assertTrue(roomRepository.isUserInMembers(roomId, participantId), "Participant should be in room");
//
//    // 3. Проверяем роли через RoomRepository
//    Role ownerRole = roomRepository.getUserRoleByRoomId(roomId, ownerId);
//    Role adminRole = roomRepository.getUserRoleByRoomId(roomId, adminId);
//    Role participantRole = roomRepository.getUserRoleByRoomId(roomId, participantId);
//
//    assertEquals(Role.OWNER, ownerRole, "Owner should have OWNER role");
//    assertEquals(Role.ADMIN, adminRole, "Admin should have ADMIN role");
//    assertEquals(Role.PARTICIPANT, participantRole, "Participant should have PARTICIPANT role");
//
//    // 4. Проверяем, что владелец действительно владелец комнаты
//    assertTrue(roomRepository.isUserOwnerOfRoom(ownerId, roomId), "User should be owner of room");
//    assertFalse(roomRepository.isUserOwnerOfRoom(adminId, roomId), "Admin should not be owner of room");
//    assertFalse(roomRepository.isUserOwnerOfRoom(participantId, roomId), "Participant should not be owner of room");
//
//    System.out.println("✓ RoomRepository basic operations test passed");
//  }
//
//  @Test
//  @Order(8)
//  @DisplayName("Test RoomRepository.removeUserFromRoom")
//  void testRemoveUserFromRoom() {
//    System.out.println("=== Testing removeUserFromRoom ===");
//
//    // Создаем временного пользователя
//    String tempUserLogin = "temp_" + System.currentTimeMillis();
//    Integer tempUserId = createTestUser(tempUserLogin, "Temporary User");
//
//    try {
//      // Добавляем пользователя в комнату
//      addUserViaJoinRequest(tempUserId, roomId, ownerToken, Role.PARTICIPANT);
//      verifyUserInRoomDirectly(roomId, tempUserId, "PARTICIPANT");
//
//      // Act - удаляем пользователя из комнаты
//      roomRepository.removeUserFromRoom(roomId, tempUserId);
//
//      // Verify - проверяем, что пользователь удален
//      assertFalse(roomRepository.isUserInMembers(roomId, tempUserId),
//        "User should not be in room after removal");
//
//      // Проверяем через прямой запрос
//      try (Connection connection = dataRepository.getDataSource().getConnection();
//           PreparedStatement stmt = connection.prepareStatement(
//             "SELECT 1 FROM Rooms_members WHERE room_id = ? AND user_id = ?")) {
//
//        stmt.setInt(1, roomId);
//        stmt.setInt(2, tempUserId);
//
//        try (ResultSet rs = stmt.executeQuery()) {
//          assertFalse(rs.next(), "User should not exist in Rooms_members");
//        }
//      }
//
//      System.out.println("✓ removeUserFromRoom test passed");
//
//    } finally {
//      deleteUserIfExists(tempUserId);
//    }
//  }
//
//  /**
//   * Проверяем данные напрямую через базу данных
//   */
//  private void verifyTestDataInDatabase() {
//    System.out.println("=== VERIFYING TEST DATA IN DATABASE ===");
//
//    try (Connection connection = dataRepository.getDataSource().getConnection()) {
//      // Проверяем комнату
//      try (PreparedStatement stmt = connection.prepareStatement(
//        "SELECT id, name FROM Rooms WHERE id = ?")) {
//
//        stmt.setInt(1, roomId);
//        try (ResultSet rs = stmt.executeQuery()) {
//          assertTrue(rs.next(), "Room should exist in database");
//          System.out.println("✓ Room in DB: " + rs.getInt("id") + " - " + rs.getString("name"));
//        }
//      }
//
//      // Проверяем членов комнаты
//      System.out.println("Room members:");
//      try (PreparedStatement stmt = connection.prepareStatement(
//        """
//        SELECT u.id, u.username, r.role
//        FROM Rooms_members rm
//        JOIN Users u ON rm.user_id = u.id
//        JOIN Roles r ON rm.role_id = r.id
//        WHERE rm.room_id = ?
//        ORDER BY r.role
//        """)) {
//
//        stmt.setInt(1, roomId);
//        try (ResultSet rs = stmt.executeQuery()) {
//          int count = 0;
//          while (rs.next()) {
//            count++;
//            System.out.println("  " + rs.getInt("id") + " - " +
//              rs.getString("username") + " (" + rs.getString("role") + ")");
//          }
//          assertTrue(count >= 3, "Should have at least 3 members in room");
//          System.out.println("✓ Total members: " + count);
//        }
//      }
//
//    } catch (SQLException e) {
//      System.err.println("Error verifying test data: " + e.getMessage());
//      throw new RuntimeException("Failed to verify test data", e);
//    }
//  }
//
//  @Test
//  @Order(99)
//  @DisplayName("Final cleanup verification")
//  void testCleanupVerification() {
//    System.out.println("=== VERIFYING CLEANUP ===");
//
//    // Этот тест должен запускаться последним
//    // Проверяем, что основные данные все еще существуют
//    assertNotNull(roomId, "Room ID should not be null");
//    assertNotNull(ownerId, "Owner ID should not be null");
//
//    // Проверяем, что комната существует
//    Room room = roomRepository.getRoomById(roomId);
//    assertNotNull(room, "Room should still exist");
//
//    // Проверяем, что владелец все еще в комнате
//    assertTrue(roomRepository.isUserInMembers(roomId, ownerId),
//      "Owner should still be in room");
//
//    System.out.println("✓ Cleanup verification passed - test data is intact");
//  }
//}