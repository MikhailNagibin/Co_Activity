package com.coactivity.controller;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.*;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Tag("docker")
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
  private TokenService tokenService;

  @Autowired
  private RoomRepositoryImpl roomRepository;

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplate;

  private Integer regularUserId;
  private Integer adminUserId;
  private Integer roomOwnerId;
  private Integer roomId;
  private String regularUserToken;
  private String adminUserToken;

  @BeforeEach
  void setUp() throws Exception {
    cleanupDatabase();

    initializeDatabase();

    createTestUsers();

    regularUserToken = createAndRegisterToken(regularUserId);
    adminUserToken = createAndRegisterToken(adminUserId);
    createTestRoom();
  }

  private String createAndRegisterToken(Integer userId) {
    String token = tokenService.createToken(userId);

    tokenService.registerToken(userId, token);

    return "Bearer " + token;
  }

  private void cleanupDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      statement.execute("SET session_replication_role = 'replica'");

      String[] tables = {
        "usersNotification", "BulletinBoard", "Answers", "Questions",
        "Bans", "rooms_requests", "Rooms_members", "Pictures",
        "Rooms", "Users",
      };

      for (String table : tables) {
        statement.execute("DELETE FROM " + table);
      }

      statement.execute("SET session_replication_role = 'origin'");
    }
  }

  private void initializeDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/init_tables.sql"));
      insertRequestStatuses(connection);
      insertRoles(connection);
      insertCategories(connection);
    }
  }

  private void insertRequestStatuses(Connection connection) throws SQLException {
    String checkSql = "SELECT COUNT(*) FROM RequestStatuses";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(checkSql)) {
      if (rs.next() && rs.getInt(1) == 0) {

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
      regularUserId = insertUser(connection, "regular@test.com", "RegularUser", "password123");

      adminUserId = insertUser(connection, "admin@test.com", "AdminUser", "password123");

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

      request.setCategory("Sport");

      request.setIsPublic(false); // Private room - требует запроса на вступление
      request.setChatLink("http://test-chat-link.com/private");
      request.setMaximumNumberOfPeople(50);

      Room createdRoom = roomRepository.createRoom(roomOwnerId, request);
      roomId = createdRoom.getId();

      roomRepository.addUserToRoom(roomId, adminUserId, Role.ADMIN);
      System.out.println(roomRepository.getUsersInRoom(roomId));
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
      insertJoinRequest(connection, regularUserId, roomId, "Consideration");

      Integer anotherUserId = insertUser(connection, "another@test.com", "AnotherUser", "password123");
      insertJoinRequest(connection, anotherUserId, roomId, "Consideration");

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
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getPendingRequests(adminUserToken);

      System.out.println(roomRepository.getUsersInRoom(roomId));

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

      System.out.println(requests);
      assertFalse(requests.isEmpty());
      assertTrue(requests.stream().allMatch(r ->
        r.getStatus() == RequestStatus.CONSIDERATION));

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
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getPendingRequestsForRoom(adminUserToken, roomId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

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
      Integer pendingRequestId = getPendingRequestId();
      System.out.println("Processing request ID: " + pendingRequestId);

      ResponseEntity<Void> response =
        userController.processJoinRequest(adminUserToken, pendingRequestId, RequestStatus.ACCEPTED);

      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      RequestStatus newStatus = getRequestStatus(pendingRequestId);
      assertEquals(RequestStatus.ACCEPTED, newStatus);

      assertTrue(roomRepository.isUserInMembers(roomId, regularUserId));
    }

    @Test
    @DisplayName("Should refuse pending join request")
    void processJoinRequest_Refuse_Success() throws SQLException {
      Integer pendingRequestId = getPendingRequestId();

      ResponseEntity<Void> response =
        userController.processJoinRequest(adminUserToken, pendingRequestId, RequestStatus.REFUSED);

      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      RequestStatus newStatus = getRequestStatus(pendingRequestId);
      assertEquals(RequestStatus.REFUSED, newStatus);

      assertFalse(roomRepository.isUserInMembers(roomId, regularUserId));
    }
  }

  @Nested
  @DisplayName("US-604: View sent join requests (User)")
  class ViewSentRequestsTest {

    @Test
    @DisplayName("Should return all sent join requests for user")
    void getSentRequests_RegularUser_ReturnsAllSentRequests() {
      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getSentRequests(regularUserToken);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<JoinRequestResponse> requests = response.getBody();

      assertFalse(requests.isEmpty());
      assertTrue(requests.stream().allMatch(r ->
        r.getUserId().equals(regularUserId)));

      System.out.println("User " + regularUserId + " has " + requests.size() + " sent requests");
    }

    @Test
    @DisplayName("Should return empty list when user has no sent requests")
    void getSentRequests_NoSentRequests_ReturnsEmptyList() throws SQLException {
      Integer newUserId = createUser("norequests@test.com", "NoRequestsUser");
      String newUserToken = createAndRegisterToken(newUserId);

      ResponseEntity<List<JoinRequestResponse>> response =
        userController.getSentRequests(newUserToken);

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
      Integer pendingRequestId = getPendingRequestId();
      System.out.println("Cancelling request ID: " + pendingRequestId);

      ResponseEntity<Void> response =
        userController.cancelRequest(regularUserToken, pendingRequestId);

      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

      assertFalse(requestExists(pendingRequestId));
    }

    @Test
    @DisplayName("Should throw exception when canceling non-existent request")
    void cancelRequest_NonExistentRequest_ThrowsException() {
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
