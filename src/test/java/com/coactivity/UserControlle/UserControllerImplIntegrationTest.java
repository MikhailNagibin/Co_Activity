package com.coactivity.UserControlle;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import javax.swing.text.StyledEditorKit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = CoActivityApplication.class)
class UserControllerImplIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
    .withDatabaseName("postgres_db")
    .withUsername("postgres")
    .withPassword("qwerty");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserControllerImpl userController;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbcTemplate;
  private String validToken;
  private Integer testUserId;
  private Integer testRoomId;
  private Integer testAdminUserId;

  @BeforeEach
  void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    initializeTestDatabase();
    createTestData();

    validToken = tokenService.createToken(testUserId);
    tokenService.registerToken(testUserId, validToken);
  }

  private void initializeTestDatabase() {
    try {
      clearDatabase();

      ClassPathResource resource = new ClassPathResource("sql/init_tables.sql");
      String sqlScript = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

      String[] sqlCommands = sqlScript.split(";");
      for (String command : sqlCommands) {
        String trimmedCommand = command.trim();
        if (!trimmedCommand.isEmpty() && !trimmedCommand.startsWith("--")) {
          try {
            jdbcTemplate.execute(trimmedCommand + ";");
          } catch (Exception e) {
            if (!e.getMessage().contains("already exists")) {
              throw e;
            }
          }
        }
      }

      insertBaseData();

    } catch (IOException e) {
      throw new RuntimeException("Failed to read SQL file", e);
    }
  }

  private void clearDatabase() {
    String[] tablesToClear = {
      "usersNotification", "BulletinBoard", "Answers", "Questions",
      "Bans", "Rooms_requests", "Rooms_members", "Pictures",
      "Rooms", "Categories", "RequestStatuses", "Roles", "Notifications", "Users"
    };

    for (String table : tablesToClear) {
      try {
        jdbcTemplate.execute("DELETE FROM " + table);
      } catch (Exception e) {
      }
    }
  }

  private void insertBaseData() {
    jdbcTemplate.execute("""
            INSERT INTO Roles(role) VALUES
            ('Admin'),
            ('Participant'),
            ('Owner')
            ON CONFLICT (role) DO NOTHING;
            """);

    jdbcTemplate.execute("""
            INSERT INTO Categories(name) VALUES
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
            ON CONFLICT (name) DO NOTHING;
            """);

    jdbcTemplate.execute("""
            INSERT INTO RequestStatuses(status_info) VALUES
            ('Consideration'),
            ('Accepted'),
            ('Refused'),
            ('RefusedWithBan')
            ON CONFLICT (status_info) DO NOTHING;
            """);

    jdbcTemplate.execute("""
            INSERT INTO Notifications(notification) VALUES
            ('MembershipAccepted'),
            ('MembershipRejected'),
            ('ActivityClosed'),
            ('NewJoinRequest')
            ON CONFLICT (notification) DO NOTHING;
            """);
  }

  private void createTestData() {
    jdbcTemplate.update("DELETE FROM Users WHERE username IN ('testuser', 'adminuser')");

    String userSql = """
        INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    testUserId = jdbcTemplate.queryForObject(
      userSql,
      Integer.class,
      "testuser@example.com",
      "testuser",
      "hashedpassword123",
      java.sql.Timestamp.from(Instant.now().minus(25 * 365, ChronoUnit.DAYS)),
      "TestCountry",
      "TestCity",
      "Test description for main user",
      1
    );

    String adminUserSql = """
        INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    testAdminUserId = jdbcTemplate.queryForObject(
      adminUserSql,
      Integer.class,
      "adminuser@example.com",
      "adminuser",
      "hashedpassword456",
      java.sql.Timestamp.from(Instant.now().minus(30 * 365, ChronoUnit.DAYS)),
      "AdminCountry",
      "AdminCity",
      "Admin user description",
      2
    );

    jdbcTemplate.update("DELETE FROM Rooms WHERE name = 'Test Room'");

    Integer sportCategoryId = jdbcTemplate.queryForObject(
      "SELECT id FROM Categories WHERE name = 'Sport'",
      Integer.class
    );

    String roomSql = """
        INSERT INTO Rooms (is_active, is_public, name, description, maximum_number_of_people, category_id) 
        VALUES (true, true, 'Test Room', 'Test Room Description', 10, ?)
        RETURNING id
        """;

    testRoomId = jdbcTemplate.queryForObject(roomSql, Integer.class, sportCategoryId);

    jdbcTemplate.update("DELETE FROM Rooms_members WHERE room_id = ? OR user_id IN (?, ?)",
      testRoomId, testUserId, testAdminUserId);

    String memberSql = """
        INSERT INTO Rooms_members (room_id, user_id, role_id) 
        VALUES (?, ?, (SELECT id FROM Roles WHERE role = 'Participant'))
        """;
    jdbcTemplate.update(memberSql, testRoomId, testUserId);

    String ownerSql = """
        INSERT INTO Rooms_members (room_id, user_id, role_id) 
        VALUES (?, ?, (SELECT id FROM Roles WHERE role = 'Owner'))
        """;
    jdbcTemplate.update(ownerSql, testRoomId, testAdminUserId);
  }

  @Test
  void getUserProfile_WithValidToken_ReturnsUserProfileSuccessfully() {
    ApiResponse<UserProfileResponse> response = userController.getUserProfile(validToken);

    System.out.println(response.getMessage());
    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("Operation completed successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(testUserId, response.getData().getId());
    assertEquals("testuser", response.getData().getUsername());
    assertEquals("testuser@example.com", response.getData().getLogin());
    assertEquals("TestCity", response.getData().getCity());
    assertEquals("TestCountry", response.getData().getCountry());
  }

  @Test
  void getUserProfile_WithInvalidToken_ReturnsUnauthorizedError() {
    String invalidToken = "invalid_token_123";

    ApiResponse<UserProfileResponse> response = userController.getUserProfile(invalidToken);

    assertNotNull(response);
    assertFalse(response.getSuccess());
    assertEquals("401", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void updateUserProfile_WithValidData_ReturnsSuccess() {
    UserProfileUpdateRequest request = new UserProfileUpdateRequest();
    request.setUsername("updatedUsername");
    request.setCity("Updated City");
    request.setCountry("Updated Country");
    request.setDescription("Updated description with valid length");
    request.setDateOfBirth(Instant.now().minus(30 * 365, ChronoUnit.DAYS));

    ApiResponse<String> response = userController.updateUserProfile(validToken, request);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("Operation completed successfully", response.getMessage());
    assertEquals("200", response.getData());
  }

  @Test
  void updateUserProfile_WithTooShortUsername_ReturnsValidationError() {
    UserProfileUpdateRequest request = new UserProfileUpdateRequest();
    request.setUsername("ab");

    ApiResponse<String> response = userController.updateUserProfile(validToken, request);

    assertNotNull(response);
    assertFalse(response.getSuccess());
    assertEquals("400", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void updateUserProfile_WithTooLongUsername_ReturnsValidationError() {
    UserProfileUpdateRequest request = new UserProfileUpdateRequest();
    request.setUsername("a".repeat(51));

    ApiResponse<String> response = userController.updateUserProfile(validToken, request);

    assertNotNull(response);
    assertFalse(response.getSuccess());
    assertEquals("400", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void deleteAccount_WithValidToken_ReturnsSuccess() {
    ApiResponse<Integer> response = userController.deleteAccount(validToken);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("Operation completed successfully", response.getMessage());
    assertEquals(200, response.getData());
  }

  @Test
  void deleteAccount_WithInvalidToken_ReturnsUnauthorizedError() {
    String invalidToken = "invalid_token";

    ApiResponse<Integer> response = userController.deleteAccount(invalidToken);

    assertNotNull(response);
    assertFalse(response.getSuccess());
    assertEquals("401", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void configureNotificationSettings_WithValidTokenAndAllNotifications_ReturnsSuccess() {
    NotificationSettingsRequest request = new NotificationSettingsRequest();
    request.setActivityClosed(true);
    request.setNewJoinRequest(true);
    request.setMembershipAccepted(true);
    request.setMembershipRejected(true);

    ApiResponse<Void> response = userController.configureNotificationSettings(validToken, request);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("Operation completed successfully", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void isUserInRoom_WhenUserIsInRoom() {
    ApiResponse<Boolean> result = userController.isUserInRoom(validToken, testRoomId);

    assertNotNull(result);
    assertTrue(result.getSuccess());
  }

  @Test
  void isUserInRoom_WhenUserIsNotInRoom_ReturnsFalse() {
    String otherRoomSql = """
            INSERT INTO Rooms (is_active, is_public, name, description, maximum_number_of_people, category_id)
            VALUES (true, true, 'Other Room', 'Other Room Description', 5, 2)
            RETURNING id
            """;
    Integer otherRoomId = jdbcTemplate.queryForObject(otherRoomSql, Integer.class);

    ApiResponse<Boolean> result = userController.isUserInRoom(validToken, otherRoomId);

    assertNotNull(result);
    assertFalse(result.getSuccess());
  }

  @Test
  void assignAdminRole_WithValidTokenAndPermissions_ReturnsSuccess() {
    String ownerToken = tokenService.createToken(testAdminUserId);
    tokenService.registerToken(testAdminUserId, ownerToken);

    ApiResponse<Void> response = userController.assignAdminRole(ownerToken, testRoomId, testUserId);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("Operation completed successfully", response.getMessage());
    assertNull(response.getData());
  }

  @Test
  void demoteAdminRole_WithValidTokenAndPermissions_ReturnsSuccess() {
    String ownerToken = tokenService.createToken(testAdminUserId);
    tokenService.registerToken(testAdminUserId, ownerToken);

    jdbcTemplate.update(
      "UPDATE Rooms_members SET role_id = (SELECT id FROM Roles WHERE role = 'Admin') WHERE user_id = ? AND room_id = ?",
      testUserId, testRoomId
    );

    ApiResponse<Void> response = userController.demoteAdminRole(ownerToken, testRoomId, testUserId);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertNull(response.getData());
  }

  @Test
  void contextLoads_AllBeansInjectedSuccessfully() {
    assertNotNull(userController);
    assertNotNull(tokenService);
    assertNotNull(dataSource);
  }
}