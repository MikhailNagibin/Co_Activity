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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Tag("docker")
@DisplayName("Room Administration Full Integration Tests")
class RoomAdministrationIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("room_admin_full_test_db")
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

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplate;

  private Integer ownerId;
  private Integer adminId;
  private Integer participant1Id;
  private Integer participant2Id;
  private Integer regularUserId;
  private String ownerToken;
  private String adminToken;
  private String participant1Token;
  private String participant2Token;
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

      statement.execute("SET session_replication_role = 'replica'");

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

      statement.execute("SET session_replication_role = 'origin'");
    }
  }

  private void initializeDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/init_tables.sql"));
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

      // Create participants
      participant1Id = createUser(connection, "participant1@test.com", "Participant1");
      participant2Id = createUser(connection, "participant2@test.com", "Participant2");

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

      // Add participants to room as PARTICIPANT
      addUserToRoom(connection, testRoomId, participant1Id, "Participant");
      addUserToRoom(connection, testRoomId, participant2Id, "Participant");
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
    participant1Token = "Bearer " + realTokenService.createToken(participant1Id);
    participant2Token = "Bearer " + realTokenService.createToken(participant2Id);
    regularUserToken = "Bearer " + realTokenService.createToken(regularUserId);

    // Register tokens
    realTokenService.registerToken(ownerId, ownerToken.substring(7));
    realTokenService.registerToken(adminId, adminToken.substring(7));
    realTokenService.registerToken(participant1Id, participant1Token.substring(7));
    realTokenService.registerToken(participant2Id, participant2Token.substring(7));
    realTokenService.registerToken(regularUserId, regularUserToken.substring(7));
  }

  @Test
  @DisplayName("Полный сценарий администрирования комнаты")
  void testFullRoomAdministrationScenario() {
    // Шаг 1: Владелец получает список всех пользователей в комнате
    ResponseEntity<List<RoomParticipantResponse>> participantsResponse =
      roomController.getRoomParticipants(ownerToken, testRoomId, null);

    assertEquals(HttpStatus.OK, participantsResponse.getStatusCode());
    assertNotNull(participantsResponse.getBody());

    List<RoomParticipantResponse> participants = participantsResponse.getBody();
    assertEquals(4, participants.size(), "В комнате должно быть 4 пользователя");

    // Проверяем роли пользователей
    boolean ownerFound = false;
    boolean adminFound = false;
    int participantCount = 0;

    for (RoomParticipantResponse participant : participants) {
      if (participant.getId().equals(ownerId)) {
        assertEquals(Role.OWNER, participant.getRole());
        ownerFound = true;
      } else if (participant.getId().equals(adminId)) {
        assertEquals(Role.ADMIN, participant.getRole());
        adminFound = true;
      } else if (participant.getRole() == Role.PARTICIPANT) {
        participantCount++;
      }
    }

    assertTrue(ownerFound, "Должен найти владельца");
    assertTrue(adminFound, "Должен найти администратора");
    assertEquals(2, participantCount, "Должно быть 2 участника");

    // Шаг 2: Владелец повышает участника до администратора
    ResponseEntity<RoleAssignmentResponse> promoteResponse =
      userController.assignAdminRole(ownerToken, testRoomId, participant1Id);

    assertEquals(HttpStatus.OK, promoteResponse.getStatusCode());
    assertNotNull(promoteResponse.getBody());

    RoleAssignmentResponse promotion = promoteResponse.getBody();
    assertEquals(participant1Id, promotion.getUserId());
    assertEquals(testRoomId, promotion.getRoomId());
    assertEquals(Role.ADMIN, promotion.getNewRole());
    assertEquals(Role.PARTICIPANT, promotion.getPreviousRole());

    // Проверяем, что роль изменилась в базе данных
    Role newRole = roomRepository.getUserRoleByRoomId(testRoomId, participant1Id);
    assertEquals(Role.ADMIN, newRole, "Пользователь должен стать администратором");

    // Шаг 3: Проверяем обновленный список участников с фильтрацией по роли ADMIN
    ResponseEntity<List<RoomParticipantResponse>> adminsResponse =
      roomController.getRoomParticipants(ownerToken, testRoomId, Role.ADMIN);

    assertEquals(HttpStatus.OK, adminsResponse.getStatusCode());
    List<RoomParticipantResponse> admins = adminsResponse.getBody();
    assertEquals(2, admins.size(), "Теперь должно быть 2 администратора");

    // Проверяем, что оба являются администраторами
    for (RoomParticipantResponse admin : admins) {
      assertEquals(Role.ADMIN, admin.getRole());
    }

    // Шаг 4: Владелец понижает администратора обратно до участника
    ResponseEntity<RoleAssignmentResponse> demoteResponse =
      userController.demoteAdminRole(ownerToken, testRoomId, participant1Id);

    assertEquals(HttpStatus.OK, demoteResponse.getStatusCode());
    assertNotNull(demoteResponse.getBody());

    RoleAssignmentResponse demotion = demoteResponse.getBody();
    assertEquals(participant1Id, demotion.getUserId());
    assertEquals(Role.PARTICIPANT, demotion.getNewRole());
    assertEquals(Role.ADMIN, demotion.getPreviousRole());

    // Проверяем, что роль изменилась обратно
    Role demotedRole = roomRepository.getUserRoleByRoomId(testRoomId, participant1Id);
    assertEquals(Role.PARTICIPANT, demotedRole, "Пользователь должен стать участником");

    // Шаг 5: Проверяем список администраторов после понижения
    ResponseEntity<List<RoomParticipantResponse>> adminsAfterDemotionResponse =
      roomController.getRoomParticipants(ownerToken, testRoomId, Role.ADMIN);

    assertEquals(HttpStatus.OK, adminsAfterDemotionResponse.getStatusCode());
    List<RoomParticipantResponse> adminsAfterDemotion = adminsAfterDemotionResponse.getBody();
    assertEquals(1, adminsAfterDemotion.size(), "Теперь должен быть 1 администратор");
    assertEquals(adminId, adminsAfterDemotion.get(0).getId());

    // Шаг 6: Администратор обновляет доску объявлений
    String initialBulletinContent = "Добро пожаловать в нашу комнату! Правила: 1. Уважайте друг друга. 2. Не спамьте.";
    ResponseEntity<BulletinBoardResponse> updateBulletinResponse =
      roomController.updateBulletinBoard(adminToken, testRoomId, initialBulletinContent);

    assertEquals(HttpStatus.OK, updateBulletinResponse.getStatusCode());
    assertNotNull(updateBulletinResponse.getBody());

    BulletinBoardResponse bulletin = updateBulletinResponse.getBody();
    assertEquals(initialBulletinContent, bulletin.getContent());
    assertEquals(adminId, bulletin.getAuthor().getId());

    // Шаг 7: Проверяем, что доска объявлений обновилась через получение деталей комнаты
    ResponseEntity<RoomDetailedResponse> roomDetailsResponse =
      roomController.getRoomById(testRoomId, adminToken);

    assertEquals(HttpStatus.OK, roomDetailsResponse.getStatusCode());
    assertNotNull(roomDetailsResponse.getBody());

    RoomDetailedResponse roomDetails = roomDetailsResponse.getBody();
    assertNotNull(roomDetails.getBulletinBoard());
    assertEquals(initialBulletinContent, roomDetails.getBulletinBoard().getContent());

    // Шаг 8: Владелец обновляет доску объявлений
    String updatedBulletinContent = "ВАЖНО: Собрание в пятницу в 18:00. Принести свои идеи для новых активностей.";
    ResponseEntity<BulletinBoardResponse> ownerUpdateBulletinResponse =
      roomController.updateBulletinBoard(ownerToken, testRoomId, updatedBulletinContent);

    assertEquals(HttpStatus.OK, ownerUpdateBulletinResponse.getStatusCode());
    assertEquals(updatedBulletinContent, ownerUpdateBulletinResponse.getBody().getContent());
    assertEquals(ownerId, ownerUpdateBulletinResponse.getBody().getAuthor().getId());

    // Шаг 9: Финальная проверка состояния комнаты
    ResponseEntity<List<RoomParticipantResponse>> finalParticipantsResponse =
      roomController.getRoomParticipants(ownerToken, testRoomId, null);

    assertEquals(HttpStatus.OK, finalParticipantsResponse.getStatusCode());
    assertEquals(4, finalParticipantsResponse.getBody().size(), "Все еще 4 пользователя в комнате");

    // Проверяем окончательные роли
    int finalAdminCount = 0;
    int finalParticipantCount = 0;

    for (RoomParticipantResponse participant : finalParticipantsResponse.getBody()) {
      if (participant.getRole() == Role.ADMIN) {
        finalAdminCount++;
      } else if (participant.getRole() == Role.PARTICIPANT) {
        finalParticipantCount++;
      }
    }

    assertEquals(1, finalAdminCount, "Должен быть 1 администратор");
    assertEquals(2, finalParticipantCount, "Должно быть 2 участника");

    // Проверяем финальную доску объявлений
    ResponseEntity<RoomDetailedResponse> finalRoomDetailsResponse =
      roomController.getRoomById(testRoomId, ownerToken);

    assertEquals(HttpStatus.OK, finalRoomDetailsResponse.getStatusCode());
    assertEquals(updatedBulletinContent, finalRoomDetailsResponse.getBody().getBulletinBoard().getContent());
  }

  @Nested
  @DisplayName("US-501: As a room administrator, I want to view room participants")
  class ViewRoomParticipantsTests {

    @Test
    @DisplayName("Owner should be able to view all participants")
    void ownerCanViewAllParticipants() {
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(ownerToken, testRoomId, null);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      List<RoomParticipantResponse> participants = response.getBody();
      assertEquals(4, participants.size());
    }

    @Test
    @DisplayName("Admin should be able to view all participants")
    void adminCanViewAllParticipants() {
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(adminToken, testRoomId, null);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(4, response.getBody().size());
    }

    @Test
    @DisplayName("Admin should be able to filter participants by role")
    void adminCanFilterParticipantsByRole() {
      ResponseEntity<List<RoomParticipantResponse>> response =
        roomController.getRoomParticipants(adminToken, testRoomId, Role.PARTICIPANT);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<RoomParticipantResponse> participants = response.getBody();
      assertEquals(2, participants.size());
      assertTrue(participants.stream().allMatch(p -> p.getRole() == Role.PARTICIPANT));
    }
  }

  @Nested
  @DisplayName("US-502: As a room administrator, I want to verify user membership")
  class VerifyUserMembershipTests {

    @Test
    @DisplayName("Admin should be able to verify participant membership")
    void adminCanVerifyParticipantMembership() {
      ResponseEntity<MembershipVerificationResponse> response =
        roomController.isUserInRoom(adminToken, testRoomId, participant1Id);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      MembershipVerificationResponse verification = response.getBody();
      assertTrue(verification.getIsMember());
      assertEquals(Role.PARTICIPANT, verification.getRole());
      assertEquals(participant1Id, verification.getUserInfo().getId());
    }

    @Test
    @DisplayName("Admin should see false for non-member")
    void adminSeesFalseForNonMember() {
      ResponseEntity<MembershipVerificationResponse> response =
        roomController.isUserInRoom(adminToken, testRoomId, regularUserId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      MembershipVerificationResponse verification = response.getBody();
      assertFalse(verification.getIsMember());
      assertNull(verification.getRole());
    }
  }

  @Nested
  @DisplayName("US-503: As a room owner, I want to promote users to administrators")
  class PromoteToAdminTests {

    @Test
    @DisplayName("Owner should be able to promote participant to admin")
    void ownerCanPromoteParticipantToAdmin() {
      ResponseEntity<RoleAssignmentResponse> response =
        userController.assignAdminRole(ownerToken, testRoomId, participant1Id);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      RoleAssignmentResponse roleResponse = response.getBody();
      assertEquals(participant1Id, roleResponse.getUserId());
      assertEquals(testRoomId, roleResponse.getRoomId());
      assertEquals(Role.ADMIN, roleResponse.getNewRole());
      assertEquals(Role.PARTICIPANT, roleResponse.getPreviousRole());

      Role actualRole = roomRepository.getUserRoleByRoomId(testRoomId, participant1Id);
      assertEquals(Role.ADMIN, actualRole);
    }
  }

  @Nested
  @DisplayName("US-504: As a room owner, I want to demote administrators")
  class DemoteAdminTests {

    @BeforeEach
    void setUpDemoteTests() throws SQLException {
      roomRepository.setRoleByUserIdAndRoomId(participant1Id, testRoomId, Role.ADMIN);
    }

    @Test
    @DisplayName("Owner should be able to demote admin to participant")
    void ownerCanDemoteAdminToParticipant() {
      ResponseEntity<RoleAssignmentResponse> response =
        userController.demoteAdminRole(ownerToken, testRoomId, participant1Id);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      RoleAssignmentResponse roleResponse = response.getBody();
      assertEquals(participant1Id, roleResponse.getUserId());
      assertEquals(Role.PARTICIPANT, roleResponse.getNewRole());
      assertEquals(Role.ADMIN, roleResponse.getPreviousRole());

      Role actualRole = roomRepository.getUserRoleByRoomId(testRoomId, participant1Id);
      assertEquals(Role.PARTICIPANT, actualRole);
    }
  }

  @Nested
  @DisplayName("US-505: As a room administrator, I want to update bulletin boards")
  class UpdateBulletinBoardTests {

    @Test
    @DisplayName("Admin should be able to update bulletin board")
    void adminCanUpdateBulletinBoard() {
      String newContent = "Important announcement: Meeting scheduled for Friday";

      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(adminToken, testRoomId, newContent);

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
      String newContent = "Owner announcement: New room rules";

      ResponseEntity<BulletinBoardResponse> response =
        roomController.updateBulletinBoard(ownerToken, testRoomId, newContent);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(newContent, response.getBody().getContent());
      assertEquals(ownerId, response.getBody().getAuthor().getId());
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
