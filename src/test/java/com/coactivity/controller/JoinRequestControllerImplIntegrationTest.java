package com.coactivity.controller;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.*;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.TokenService;
import com.coactivity.service.dto.TokenPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@DisplayName("JoinRequestControllerImplIntegrationTest - Join Request Management Integration Tests")
class JoinRequestControllerImplIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("joinrequest_test_db")
    .withUsername("test_user")
    .withPassword("test_password");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserControllerImpl userController;

  @Autowired
  private DataRepository dataRepository;

  @Autowired
  private JoinRequestService joinRequestService;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private RoomRepositoryImpl roomRepository;

  private Integer regularUserId;
  private Integer adminUserId;
  private Integer roomOwnerId;
  private Integer roomId;
  private String regularUserToken;
  private String adminUserToken;
  private String roomOwnerToken;

  @BeforeEach
  void setUp() throws Exception {
    // Clean up database before each test
    cleanupDatabase();

    // Initialize database with test data
    initializeDatabase();

    // Create test users
    createTestUsers();

    // Create valid tokens for test users и регистрируем их
    regularUserToken = createAndRegisterToken(regularUserId);
    adminUserToken = createAndRegisterToken(adminUserId);
    roomOwnerToken = createAndRegisterToken(roomOwnerId);

    // Create test rooms using roomRepository
    createTestRoom();
  }

  private String createAndRegisterToken(Integer userId) {
    // Создаем токен
    String token = tokenService.createToken(userId);

    // Регистрируем токен в TokenService
    // Метод createToken уже регистрирует токен, но убедимся
    tokenService.registerToken(userId, token);

    return "Bearer " + token;
  }

  private void cleanupDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      // Disable foreign key constraints
      statement.execute("SET session_replication_role = 'replica'");

      // Delete all data from tables in correct order
      String[] tables = {
        "usersNotification", "BulletinBoard", "Answers", "Questions",
        "Bans", "rooms_requests", "Rooms_members", "Pictures",
        "Rooms", "Users",
      };

      for (String table : tables) {
        statement.execute("DELETE FROM " + table);
      }

      // Re-enable foreign key constraints
      statement.execute("SET session_replication_role = 'origin'");
    }
  }

  private void initializeDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      // Read and execute the init_tables.sql
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("init_tables.sql");
      if (inputStream != null) {
        String sql = new String(inputStream.readAllBytes());
        String[] statements = sql.split(";");

        for (String sqlStatement : statements) {
          if (!sqlStatement.trim().isEmpty()) {
            statement.execute(sqlStatement.trim());
          }
        }
      } else {
        throw new RuntimeException("init_tables.sql not found");
      }

      // Insert RequestStatuses if not present
      insertRequestStatuses(connection);
      // Insert Roles if not present
      insertRoles(connection);
      // Insert Categories if not present
      insertCategories(connection);
    }
  }

  private void insertRequestStatuses(Connection connection) throws SQLException {
    String checkSql = "SELECT COUNT(*) FROM RequestStatuses";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(checkSql)) {
      if (rs.next() && rs.getInt(1) == 0) {
        // Insert default statuses
        String insertSql = """
                    INSERT INTO RequestStatuses (status_info) VALUES 
                    ('Consideration'),
                    ('Accepted'),
                    ('Refused'),
                    ('RefusedWithBan')
                    """;
        stmt.executeUpdate(insertSql);
      }
    }
  }

  private void insertRoles(Connection connection) throws SQLException {
    String checkSql = "SELECT COUNT(*) FROM Roles";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(checkSql)) {
      if (rs.next() && rs.getInt(1) == 0) {
        // Insert default roles
        String insertSql = """
                    INSERT INTO Roles (role) VALUES 
                    ('OWNER'),
                    ('ADMIN'),
                    ('PARTICIPANT')
                    """;
        stmt.executeUpdate(insertSql);
      }
    }
  }

  private void insertCategories(Connection connection) throws SQLException {
    String checkSql = "SELECT COUNT(*) FROM Categories";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(checkSql)) {
      if (rs.next() && rs.getInt(1) == 0) {
        // Insert default categories
        String insertSql = """
                    INSERT INTO Categories (name) VALUES 
                    ('Sport'),
                    ('Music'),
                    ('Art')
                    """;
        stmt.executeUpdate(insertSql);
      }
    }
  }

  private void createTestUsers() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      // Create regular user
      regularUserId = insertUser(connection, "regular@test.com", "RegularUser", "password123");

      // Create admin user
      adminUserId = insertUser(connection, "admin@test.com", "AdminUser", "password123");

      // Create room owner
      roomOwnerId = insertUser(connection, "owner@test.com", "RoomOwner", "password123");
    }
  }

  private Integer insertUser(Connection connection, String email, String username, String password) throws SQLException {
    String sql = """
            INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING id
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setString(2, username);
      ps.setString(3, password);
      ps.setTimestamp(4, Timestamp.from(Instant.now().minusSeconds(86400 * 365 * 25)));
      ps.setString(5, "TestCountry");
      ps.setString(6, "TestCity");
      ps.setString(7, "Test description for " + username);
      ps.setInt(8, 1);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    throw new SQLException("Failed to insert user");
  }

  private void createTestRoom() {
    try {
      // Создаем RoomCreationRequest
      RoomCreationRequest request = new RoomCreationRequest();
      request.setName("Test Private Room");
      request.setDescription("A private room for testing join requests");
      request.setDateOfStartEvent(Instant.now().plusSeconds(86400));
      request.setDateOfEndEvent(Instant.now().plusSeconds(86400 * 7));
      request.setAgeRating(18);
      request.setFrequency(Instant.now().plusSeconds(86400));

      // Получаем ID категории Sport
      Integer sportCategoryId = getCategoryId("Sport");
      request.setCategoryId(sportCategoryId);

      request.setIsPublic(false); // Private room - требует запроса на вступление
      request.setChatLink("http://test-chat-link.com/private");
      request.setMaximumNumberOfPeople(50);

      // Создаем комнату через roomRepository
      Room createdRoom = roomRepository.createRoom(roomOwnerId, request);
      roomId = createdRoom.getId();

      // Добавляем администратора в комнату
      roomRepository.addUserToRoom(roomId, adminUserId, Role.ADMIN);
      System.out.println(roomRepository.getUsersInRoom(roomId));
      // Создаем тестовые запросы на вступление
      createTestJoinRequests();

    } catch (Exception e) {
      throw new RuntimeException("Failed to create test room: " + e.getMessage(), e);
    }
  }

  private Integer getCategoryId(String categoryName) throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(
           "SELECT id FROM Categories WHERE name = ?")) {
      ps.setString(1, categoryName);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    throw new SQLException("Category not found: " + categoryName);
  }

  private void createTestJoinRequests() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      // Создаем запросы на вступление для регулярного пользователя
      insertJoinRequest(connection, regularUserId, roomId, "Consideration");

      // Создаем еще одного пользователя и его запрос
      Integer anotherUserId = insertUser(connection, "another@test.com", "AnotherUser", "password123");
      insertJoinRequest(connection, anotherUserId, roomId, "Consideration");

      // Создаем запросы с разными статусами для тестирования
      Integer acceptedUserId = insertUser(connection, "accepted@test.com", "AcceptedUser", "password123");
      insertJoinRequest(connection, acceptedUserId, roomId, "Accepted");

      Integer refusedUserId = insertUser(connection, "refused@test.com", "RefusedUser", "password123");
      insertJoinRequest(connection, refusedUserId, roomId, "Refused");
    }
  }

  private void insertJoinRequest(Connection connection, Integer userId, Integer roomId, String status) throws SQLException {
    String sql = """
            INSERT INTO rooms_requests (user_id, room_id, status_id, created_at)
            VALUES (?, ?, ?, ?)
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setInt(2, roomId);

      // Получаем status_id
      Integer statusId = getStatusId(connection, status);
      ps.setInt(3, statusId);
      ps.setTimestamp(4, Timestamp.from(Instant.now()));

      ps.executeUpdate();
    }
  }

  private Integer getStatusId(Connection connection, String status) throws SQLException {
    String sql = "SELECT id FROM RequestStatuses WHERE status_info = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }

    throw new SQLException("Status not found: " + status);
  }

  @Nested
  @DisplayName("US-601: View all pending join requests (Admin)")
  class ViewAllPendingRequestsTest {

    @Test
    @DisplayName("Should return all pending join requests for admin")
    void getPendingRequests_AdminUser_ReturnsAllPendingRequests() {
      // Act
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getPendingRequests(adminUserToken);

      // Assert
      System.out.println(roomRepository.getUsersInRoom(roomId));

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

      // Должны возвращаться только ожидающие запросы (статус Consideration)
      System.out.println(requests);
      assertFalse(requests.isEmpty());
      assertTrue(requests.stream().allMatch(r ->
        r.getStatus() == RequestStatus.CONSIDERATION));

      // Проверяем детали запросов
      for (JoinRequestResponse request : requests) {
        assertNotNull(request.getRequestId());
        assertNotNull(request.getUserId());
        assertNotNull(request.getUsername());
        assertNotNull(request.getRoomId());
        assertNotNull(request.getRoomName());
        assertNotNull(request.getCreatedAt());
      }
    }
  }

  @Nested
  @DisplayName("US-602: View room-specific join requests (Admin)")
  class ViewRoomSpecificPendingRequestsTest {

    @Test
    @DisplayName("Should return pending requests for specific room")
    void getPendingRequestsForRoom_AdminUser_ReturnsRoomSpecificRequests() {
      // Act
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getPendingRequestsForRoom(adminUserToken, roomId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

      // Должны возвращаться только запросы для указанной комнаты
      assertFalse(requests.isEmpty());
      assertTrue(requests.stream().allMatch(r ->
        r.getRoomId().equals(roomId) &&
          r.getStatus() == RequestStatus.CONSIDERATION));

      System.out.println("Found " + requests.size() + " pending requests for room " + roomId);
      for (JoinRequestResponse request : requests) {
        System.out.println("Request ID: " + request.getRequestId() +
          ", User: " + request.getUsername() +
          ", Room: " + request.getRoomName());
      }
    }

    @Test
    @DisplayName("Should throw exception when user is not admin of the room")
    void getPendingRequestsForRoom_NonAdminUser_ThrowsException() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.getPendingRequestsForRoom(regularUserToken, roomId));
    }
  }

  @Nested
  @DisplayName("US-603: Process join requests (Admin)")
  class ProcessJoinRequestsTest {

    @Test
    @DisplayName("Should accept pending join request")
    void processJoinRequest_Accept_Success() throws SQLException {
      // Arrange - получаем ID ожидающего запроса
      Integer pendingRequestId = getPendingRequestId();
      System.out.println("Processing request ID: " + pendingRequestId);

      // Act
      ResponseEntity<Void> response =
        userController.processJoinRequest(adminUserToken, pendingRequestId, RequestStatus.ACCEPTED);

      // Assert
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      // Проверяем, что статус запроса изменился на ACCEPTED
      RequestStatus newStatus = getRequestStatus(pendingRequestId);
      assertEquals(RequestStatus.ACCEPTED, newStatus);

      // Проверяем, что пользователь добавлен в комнату через roomRepository
      assertTrue(roomRepository.isUserInMembers(roomId, regularUserId));
    }

    @Test
    @DisplayName("Should refuse pending join request")
    void processJoinRequest_Refuse_Success() throws SQLException {
      // Arrange - получаем ID ожидающего запроса
      Integer pendingRequestId = getPendingRequestId();

      // Act
      ResponseEntity<Void> response =
        userController.processJoinRequest(adminUserToken, pendingRequestId, RequestStatus.REFUSED);

      // Assert
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      // Проверяем, что статус запроса изменился на REFUSED
      RequestStatus newStatus = getRequestStatus(pendingRequestId);
      assertEquals(RequestStatus.REFUSED, newStatus);

      // Проверяем, что пользователь НЕ добавлен в комнату
      assertFalse(roomRepository.isUserInMembers(roomId, regularUserId));
    }

    @Test
    @DisplayName("Should refuse with ban pending join request")
    void processJoinRequest_RefuseWithBan_Success() throws SQLException {
      // Arrange - получаем ID ожидающего запроса
      Integer pendingRequestId = getPendingRequestId();

      // Act
      ResponseEntity<Void> response =
        userController.processJoinRequest(adminUserToken, pendingRequestId, RequestStatus.REFUSED_WITH_BAN);

      // Assert
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      // Проверяем, что статус запроса изменился на REFUSED_WITH_BAN
      RequestStatus newStatus = getRequestStatus(pendingRequestId);
      assertEquals(RequestStatus.REFUSED_WITH_BAN, newStatus);

      // Проверяем, что пользователь забанен в комнате
      Room room = roomRepository.getRoomById(roomId);
      assertTrue(room.getBans().stream()
        .anyMatch(user -> user.getId().equals(regularUserId)));
    }
  }

  @Nested
  @DisplayName("US-604: View sent join requests (User)")
  class ViewSentRequestsTest {

    @Test
    @DisplayName("Should return all sent join requests for user")
    void getSentRequests_RegularUser_ReturnsAllSentRequests() {
      // Act
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getSentRequests(regularUserToken);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

      // Должны возвращаться все запросы, отправленные этим пользователем
      assertFalse(requests.isEmpty());
      assertTrue(requests.stream().allMatch(r ->
        r.getUserId().equals(regularUserId)));

      System.out.println("User " + regularUserId + " has " + requests.size() + " sent requests");
    }

    @Test
    @DisplayName("Should return empty list when user has no sent requests")
    void getSentRequests_NoSentRequests_ReturnsEmptyList() throws SQLException {
      // Arrange - создаем пользователя без запросов
      Integer newUserId = createUser("norequests@test.com", "NoRequestsUser");
      String newUserToken = createAndRegisterToken(newUserId);

      // Act
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getSentRequests(newUserToken);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertTrue(response.getBody().isEmpty());
    }
  }

  @Nested
  @DisplayName("US-605: Cancel pending join requests (User)")
  class CancelPendingRequestsTest {

    @Test
    @DisplayName("Should cancel pending join request")
    void cancelRequest_PendingRequest_Success() throws SQLException {
      // Arrange - получаем ID ожидающего запроса
      Integer pendingRequestId = getPendingRequestId();
      System.out.println("Cancelling request ID: " + pendingRequestId);

      // Act
      ResponseEntity<Void> response =
        userController.cancelRequest(regularUserToken, pendingRequestId);

      // Assert
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      // Проверяем, что запрос удален
      assertFalse(requestExists(pendingRequestId));
    }

    @Test
    @DisplayName("Should throw exception when canceling non-existent request")
    void cancelRequest_NonExistentRequest_ThrowsException() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.ResourceNotFoundException.class,
        () -> userController.cancelRequest(regularUserToken, 999999));
    }
  }

  // Вспомогательные методы
  private Integer createUser(String email, String username) throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection()) {
      return insertUser(connection, email, username, "password123");
    }
  }

  private Integer getPendingRequestId() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(
           "SELECT id FROM rooms_requests WHERE user_id = ? AND status_id = ? LIMIT 1")) {
      ps.setInt(1, regularUserId);
      ps.setInt(2, getStatusId(connection, "Consideration"));

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    throw new SQLException("No pending request found");
  }

  private RequestStatus getRequestStatus(Integer requestId) throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(
           "SELECT rs.status_info FROM rooms_requests rr " +
             "JOIN RequestStatuses rs ON rr.status_id = rs.id " +
             "WHERE rr.id = ?")) {
      ps.setInt(1, requestId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return RequestStatus.fromDatabase(rs.getString(1));
        }
      }
    }
    throw new SQLException("Request not found: " + requestId);
  }

  private boolean requestExists(Integer requestId) throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(
           "SELECT COUNT(*) FROM rooms_requests WHERE id = ?")) {
      ps.setInt(1, requestId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    }
    return false;
  }
}