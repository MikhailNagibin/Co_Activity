package com.coactivity.controller;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.response.*;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.Role;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
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
@DisplayName("Room Administration Integration Tests")
class RoomAdministrationIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("room_admin_test_db")
    .withUsername("test")
    .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private RoomControllerImpl roomController;

  @Autowired
  private UserControllerImpl userController;

  @Autowired
  private DataRepository dataRepository;

  @Autowired
  private RoomRepositoryImpl roomRepository;

  @Autowired
  private UserRepositoryImpl userRepository;

  @Autowired
  private TokenService realTokenService;

  private Integer ownerId;
  private Integer adminId;
  private Integer participantId;
  private Integer regularUserId;
  private String ownerToken;
  private String adminToken;
  private String participantToken;
  private String regularUserToken;
  private Integer testRoomId;

  @BeforeEach
  void setUp() throws Exception {
    // Clean up database
    cleanupDatabase();

    // Initialize database structure
    initializeDatabase();

    // Insert reference data
    insertReferenceData();

    // Create test users
    createTestUsers();

    // Create test room
    createTestRoom();

    // Setup tokens using real TokenService
    setupTokens();
  }

  private void cleanupDatabase() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      // Temporarily disable foreign key constraints
      statement.execute("SET session_replication_role = 'replica'");

      // Delete data from all tables in correct order
      String[] tables = {
        "usersNotification", "BulletinBoard", "Answers", "Questions",
        "Bans", "rooms_requests", "Rooms_members", "Pictures",
        "Rooms", "Users", "Categories", "Roles", "RequestStatuses", "Notifications"
      };

      for (String table : tables) {
        try {
          statement.execute("DELETE FROM " + table);
        } catch (SQLException e) {
          // Table might not exist yet, ignore
        }
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
            try {
              statement.execute(sqlStatement.trim());
            } catch (SQLException e) {
              // Ignore errors for tables that already exist
            }
          }
        }
      }
    }
  }

  private void insertReferenceData() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      // Insert roles
      statement.execute("""
                INSERT INTO Roles (role) VALUES 
                ('Owner'),
                ('Admin'),
                ('Participant')
                ON CONFLICT DO NOTHING
                """);

      // Insert categories
      statement.execute("""
                INSERT INTO Categories (name) VALUES 
                ('Sport'),
                ('Music'),
                ('Art'),
                ('Entertainments'),
                ('Business'),
                ('Education'),
                ('ActiveRecreation'),
                ('PassiveRecreation'),
                ('MassEvent'),
                ('Other'),
                ('NotSpecified')
                ON CONFLICT DO NOTHING
                """);

      // Insert request statuses
      statement.execute("""
                INSERT INTO RequestStatuses (status_info) VALUES 
                ('Consideration'),
                ('Accepted'),
                ('Refused'),
                ('RefusedWithBan')
                ON CONFLICT DO NOTHING
                """);

      // Insert notifications
      statement.execute("""
                INSERT INTO Notifications (notification) VALUES 
                ('MembershipAccepted'),
                ('MembershipRejected'),
                ('ActivityClosed'),
                ('NewJoinRequest')
                ON CONFLICT DO NOTHING
                """);
    }
  }

  private void createTestUsers() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      // Create room owner
      ownerId = createUser(connection, "owner@test.com", "RoomOwner");

      // Create admin user
      adminId = createUser(connection, "admin@test.com", "RoomAdmin");

      // Create regular participant
      participantId = createUser(connection, "participant@test.com", "RoomParticipant");

      // Create regular user (not in room)
      regularUserId = createUser(connection, "regular@test.com", "RegularUser");
    }
  }

  private Integer createUser(Connection connection, String email, String username) throws SQLException {
    String sql = """
            INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING id
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setString(2, username);
      ps.setString(3, hashPassword("password123"));
      ps.setTimestamp(4, Timestamp.from(Instant.now().minusSeconds(86400 * 365 * 25)));
      ps.setString(5, "TestCountry");
      ps.setString(6, "TestCity");
      ps.setString(7, "Test description for " + username);
      ps.setInt(8, 1);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("id");
        }
      }
    }
    throw new SQLException("Failed to create user: " + username);
  }

  private String hashPassword(String password) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(password.getBytes());
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to hash password", e);
    }
  }

  private void createTestRoom() throws SQLException {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      // Get category ID
      Integer sportCategoryId = getCategoryId(connection, "Sport");

      // Create room
      String roomSql = """
                INSERT INTO Rooms (is_active, is_public, chat_link, category_id, name, description, 
                                   start_date, end_date, age_rating, frequency, maximum_number_of_people)
                VALUES (true, false, 'https://test-chat.com', ?, 'Test Admin Room', 
                        'Room for testing administration features', 
                        CURRENT_TIMESTAMP + INTERVAL '1 day', 
                        CURRENT_TIMESTAMP + INTERVAL '7 days', 
                        18, CURRENT_TIMESTAMP + INTERVAL '1 day', 10)
                RETURNING id
                """;

      try (PreparedStatement ps = connection.prepareStatement(roomSql)) {
        ps.setInt(1, sportCategoryId);

        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            testRoomId = rs.getInt("id");
          }
        }
      }

      // Add owner to room as OWNER
      addUserToRoom(connection, testRoomId, ownerId, "Owner");

      // Add admin to room as ADMIN
      addUserToRoom(connection, testRoomId, adminId, "Admin");

      // Add participant to room as PARTICIPANT
      addUserToRoom(connection, testRoomId, participantId, "Participant");
    }
  }

  private Integer getCategoryId(Connection connection, String categoryName) throws SQLException {
    String sql = "SELECT id FROM Categories WHERE name = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, categoryName);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("id");
        }
      }
    }
    throw new SQLException("Category not found: " + categoryName);
  }

  private void addUserToRoom(Connection connection, Integer roomId, Integer userId, String role) throws SQLException {
    String sql = """
            INSERT INTO Rooms_members (room_id, user_id, role_id)
            VALUES (?, ?, (SELECT id FROM Roles WHERE role = ?))
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, userId);
      ps.setString(3, role);
      ps.executeUpdate();
    }
  }

  private void setupTokens() {
    // Create tokens using real TokenService
    ownerToken = "Bearer " + realTokenService.createToken(ownerId);
    adminToken = "Bearer " + realTokenService.createToken(adminId);
    participantToken = "Bearer " + realTokenService.createToken(participantId);
    regularUserToken = "Bearer " + realTokenService.createToken(regularUserId);

    // Register tokens
    realTokenService.registerToken(ownerId, ownerToken.substring(7));
    realTokenService.registerToken(adminId, adminToken.substring(7));
    realTokenService.registerToken(participantId, participantToken.substring(7));
    realTokenService.registerToken(regularUserId, regularUserToken.substring(7));
  }

  @Nested
  @DisplayName("US-501: As a room administrator, I want to view   room participants")
  class ViewRoomParticipantsTests {

    @Test
    @DisplayName("Owner should be able to view all participants")
    void ownerCanViewAllParticipants() {
      // Act
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(ownerToken, testRoomId, null);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      System.out.println(response.getBody().getClass());
      List<RoomParticipantResponse> participants = response.getBody();
      assertEquals(3, participants.size());

      // Verify all users are present with correct roles
      boolean foundOwner = false;
      boolean foundAdmin = false;
      boolean foundParticipant = false;

      for (RoomParticipantResponse participant : participants) {
        if (participant.getId().equals(ownerId)) {
          assertEquals(Role.OWNER, participant.getRole());
          foundOwner = true;
        } else if (participant.getId().equals(adminId)) {
          assertEquals(Role.ADMIN, participant.getRole());
          foundAdmin = true;
        } else if (participant.getId().equals(participantId)) {
          assertEquals(Role.PARTICIPANT, participant.getRole());
          foundParticipant = true;
        }
      }

      assertTrue(foundOwner, "Should find owner");
      assertTrue(foundAdmin, "Should find admin");
      assertTrue(foundParticipant, "Should find participant");
    }

    @Test
    @DisplayName("Admin should be able to view all participants")
    void adminCanViewAllParticipants() {
      // Act
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(adminToken, testRoomId, null);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(3, response.getBody().size());
    }

    @Test
    @DisplayName("Admin should be able to filter participants by role")
    void adminCanFilterParticipantsByRole() {
      // Act - filter by PARTICIPANT role
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(adminToken, testRoomId, Role.PARTICIPANT);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<RoomParticipantResponse> participants = response.getBody();
      assertEquals(1, participants.size());
      assertEquals(participantId, participants.get(0).getId());
      assertEquals(Role.PARTICIPANT, participants.get(0).getRole());
    }

    @Test
    @DisplayName("Participant should not be able to view participants")
    void participantCannotViewParticipants() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> roomController.getRoomParticipants(participantToken, testRoomId, null));
    }

    @Test
    @DisplayName("Regular user should not be able to view participants")
    void regularUserCannotViewParticipants() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> roomController.getRoomParticipants(regularUserToken, testRoomId, null));
    }

    @Test
    @DisplayName("Filter by ADMIN role should return only admins")
    void filterByAdminRoleReturnsOnlyAdmins() throws SQLException {
      // Arrange - add another admin
      Integer anotherAdminId = createUser("another_admin@test.com", "AnotherAdmin");
      addUserToRoom(testRoomId, anotherAdminId, "Admin");
      String anotherAdminToken = "Bearer " + realTokenService.createToken(anotherAdminId);
      realTokenService.registerToken(anotherAdminId, anotherAdminToken.substring(7));

      // Act
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(ownerToken, testRoomId, Role.ADMIN);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      List<RoomParticipantResponse> admins = response.getBody();
      assertEquals(2, admins.size()); // Original admin + new admin
      assertTrue(admins.stream().allMatch(p -> p.getRole() == Role.ADMIN));
    }
  }

  @Nested
  @DisplayName("US-502: As a room administrator, I want to verify user membership")
  class VerifyUserMembershipTests {

    @Test
    @DisplayName("Admin should be able to verify participant membership")
    void adminCanVerifyParticipantMembership() {
      // Act
      ResponseEntity<MembershipVerificationResponse> response =
        roomController.isUserInRoom(adminToken, testRoomId, participantId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      MembershipVerificationResponse verification = response.getBody();
      assertTrue(verification.getIsMember());
      assertEquals(Role.PARTICIPANT, verification.getRole());
      assertEquals(participantId, verification.getUserInfo().getId());
    }

    @Test
    @DisplayName("Owner should be able to verify admin membership")
    void ownerCanVerifyAdminMembership() {
      // Act
      ResponseEntity<MembershipVerificationResponse> response =
        roomController.isUserInRoom(ownerToken, testRoomId, adminId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      MembershipVerificationResponse verification = response.getBody();
      assertTrue(verification.getIsMember());
      assertEquals(Role.ADMIN, verification.getRole());
    }

    @Test
    @DisplayName("Admin should see false for non-member")
    void adminSeesFalseForNonMember() {
      // Act
      ResponseEntity<MembershipVerificationResponse> response =
        roomController.isUserInRoom(adminToken, testRoomId, regularUserId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      MembershipVerificationResponse verification = response.getBody();
      assertFalse(verification.getIsMember());
      assertNull(verification.getRole());
      assertEquals(regularUserId, verification.getUserInfo().getId());
    }

    @Test
    @DisplayName("Participant should not be able to verify membership")
    void participantCannotVerifyMembership() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> roomController.isUserInRoom(participantToken, testRoomId, adminId));
    }

    @Test
    @DisplayName("Regular user should not be able to verify membership")
    void regularUserCannotVerifyMembership() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> roomController.isUserInRoom(regularUserToken, testRoomId, participantId));
    }
  }

  @Nested
  @DisplayName("US-503: As a room owner, I want to promote users to administrators")
  class PromoteToAdminTests {

    @Test
    @DisplayName("Owner should be able to promote participant to admin")
    void ownerCanPromoteParticipantToAdmin() {
      // Act
      System.out.println(roomRepository.getUserRoleByRoomId(testRoomId, participantId));
      ResponseEntity<RoleAssignmentResponse> response =
        userController.assignAdminRole(ownerToken, testRoomId, participantId);
      System.out.println(roomRepository.getUserRoleByRoomId(testRoomId, participantId));

      // Assert


      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      RoleAssignmentResponse roleResponse = response.getBody();
      assertEquals(participantId, roleResponse.getUserId());
      assertEquals(testRoomId, roleResponse.getRoomId());
      assertEquals(Role.ADMIN, roleResponse.getNewRole());
      assertEquals(Role.PARTICIPANT, roleResponse.getPreviousRole());

      // Verify in database
      Role actualRole = roomRepository.getUserRoleByRoomId(testRoomId, participantId);
      assertEquals(Role.ADMIN, actualRole);
    }

    @Test
    @DisplayName("Admin should not be able to promote users")
    void adminCannotPromoteUsers() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.assignAdminRole(adminToken, testRoomId, participantId));
    }

    @Test
    @DisplayName("Should throw exception when promoting non-member")
    void cannotPromoteNonMember() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.ValidationException.class,
        () -> userController.assignAdminRole(ownerToken, testRoomId, regularUserId));
    }

    @Test
    @DisplayName("Participant should not be able to promote users")
    void participantCannotPromoteUsers() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.assignAdminRole(participantToken, testRoomId, adminId));
    }

    @Test
    @DisplayName("Regular user should not be able to promote users")
    void regularUserCannotPromoteUsers() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.assignAdminRole(regularUserToken, testRoomId, participantId));
    }
  }

  @Nested
  @DisplayName("US-504: As a room owner, I want to demote administrators")
  class DemoteAdminTests {

    @BeforeEach
    void setUpDemoteTests() throws SQLException {
      // Ensure participant is an admin for demotion tests
      roomRepository.setRoleByUserIdAndRoomId(participantId, testRoomId, Role.ADMIN);
    }

    @Test
    @DisplayName("Owner should be able to demote admin to participant")
    void ownerCanDemoteAdminToParticipant() {
      // Act
      ResponseEntity<RoleAssignmentResponse> response =
        userController.demoteAdminRole(ownerToken, testRoomId, participantId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());

      RoleAssignmentResponse roleResponse = response.getBody();
      assertEquals(participantId, roleResponse.getUserId());
      assertEquals(Role.PARTICIPANT, roleResponse.getNewRole());
      assertEquals(Role.ADMIN, roleResponse.getPreviousRole());

      // Verify in database
      Role actualRole = roomRepository.getUserRoleByRoomId(testRoomId, participantId);
      assertEquals(Role.PARTICIPANT, actualRole);
    }

    @Test
    @DisplayName("Admin should not be able to demote other admins")
    void adminCannotDemoteOtherAdmins() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.demoteAdminRole(adminToken, testRoomId, participantId));
    }

    @Test
    @DisplayName("Demoting non-admin should work but keep same role")
    void demotingNonAdminKeepsSameRole() throws SQLException {
      // Arrange - ensure participant is just a participant
      roomRepository.setRoleByUserIdAndRoomId(participantId, testRoomId, Role.PARTICIPANT);

      // Act
      ResponseEntity<RoleAssignmentResponse> response =
        userController.demoteAdminRole(ownerToken, testRoomId, participantId);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(Role.PARTICIPANT, response.getBody().getNewRole());
      assertEquals(Role.PARTICIPANT, response.getBody().getPreviousRole());
    }

    @Test
    @DisplayName("Participant should not be able to demote admins")
    void participantCannotDemoteAdmins() {
      // Act & Assert
      assertThrows(com.coactivity.service.exception.AuthorizationException.class,
        () -> userController.demoteAdminRole(participantToken, testRoomId, adminId));
    }
  }

  @Nested
  @DisplayName("US-505: As a room administrator, I want to update bulletin boards")
  class UpdateBulletinBoardTests {

    @Test
    @DisplayName("Admin should be able to update bulletin board")
    void adminCanUpdateBulletinBoard() {
      // Arrange
      String newContent = "Important announcement: Meeting scheduled for Friday";

      // Act
      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(adminToken, testRoomId, newContent);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      BulletinBoardResponse bulletin = response.getBody();
      assertEquals(newContent, bulletin.getContent());
      assertEquals(adminId, bulletin.getAuthor().getId());
      assertNotNull(bulletin.getUpdatedAt());
    }

    @Test
    @DisplayName("Owner should be able to update bulletin board")
    void ownerCanUpdateBulletinBoard() {
      // Arrange
      String newContent = "Owner announcement: New room rules";

      // Act
      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(ownerToken, testRoomId, newContent);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(newContent, response.getBody().getContent());
      assertEquals(ownerId, response.getBody().getAuthor().getId());
    }

    @Test
    @DisplayName("Should create bulletin board if not exists")
    void createsBulletinBoardIfNotExists() {
      // Arrange - delete any existing bulletin board
      try (Connection connection = dataRepository.getDataSource().getConnection();
           PreparedStatement ps = connection.prepareStatement(
             "DELETE FROM BulletinBoard WHERE room_id = ?")) {
        ps.setInt(1, testRoomId);
        ps.executeUpdate();
      } catch (SQLException e) {
      }

      String newContent = "Creating new bulletin board";

      // Act
      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(adminToken, testRoomId, newContent);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(newContent, response.getBody().getContent());
    }

    @Test
    @DisplayName("Should update existing bulletin board content")
    void updatesExistingBulletinBoard() {
      // Arrange - first create a bulletin board
      String initialContent = "Initial content";
      roomController.updateBulletinBoard(adminToken, testRoomId, initialContent);

      String updatedContent = "Updated content";

      // Act
      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(adminToken, testRoomId, updatedContent);

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(updatedContent, response.getBody().getContent());

      // Verify by getting room details
      ResponseEntity<RoomDetailedResponse> roomResponse =
        roomController.getRoomById(testRoomId, adminToken);
      assertEquals(updatedContent, roomResponse.getBody().getBulletinBoard().getContent());
    }
  }



  // Helper methods
  private Integer createUser(String email, String username) throws SQLException {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      return createUser(connection, email, username);
    }
  }

  private void addUserToRoom(Integer roomId, Integer userId, String role) throws SQLException {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      addUserToRoom(connection, roomId, userId, role);
    }
  }
}