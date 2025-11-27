package com.coactivity.UserControlle;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.*;
import com.coactivity.repository.impl.*;
import com.coactivity.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
  private UserRepositoryImpl userRepository;

  @Autowired
  private RoomRepositoryImpl roomRepository;

  @Autowired
  private RoomsRequestRepositoryImpl roomsRequestRepository;

  private String validToken;
  private Integer testUserId;
  private Integer testRoomId;
  private Integer testAdminUserId;
  private Integer otherRoomId;

  @BeforeEach
  void setUp() {
    initializeTestDatabase();
    createTestData();

    validToken = tokenService.createToken(testUserId);
    tokenService.registerToken(testUserId, validToken);
  }

  private void initializeTestDatabase() {
    // Очищаем данные через репозитории
    clearTestData();

    // Инициализируем базовые данные через репозитории
    initializeBaseData();
  }

  private void clearTestData() {
    // Получаем все комнаты и удаляем их через репозиторий
    List<Room> allRooms = roomRepository.getAllRooms();
    for (Room room : allRooms) {
      if (room.getName().equals("Test Room") || room.getName().equals("Other Room")) {
        roomRepository.deleteRoom(room.getId());
      }
    }

    // Удаляем тестовых пользователей через репозиторий
    try {
      User testUser = userRepository.getUser("testuser@example.com", "hashedpassword123");
      if (testUser != null) {
        userRepository.deleteUser(testUser.getId());
      }
    } catch (Exception e) {
      // Пользователь не существует
    }

    try {
      User adminUser = userRepository.getUser("adminuser@example.com", "hashedpassword456");
      if (adminUser != null) {
        userRepository.deleteUser(adminUser.getId());
      }
    } catch (Exception e) {
      // Пользователь не существует
    }
  }

  private void initializeBaseData() {
    // Категории, роли и статусы обычно инициализируются через миграции
    // В тестах мы предполагаем, что они уже существуют
    // Если нужно создавать через репозитории, добавьте соответствующие методы в репозитории
  }

  private void createTestData() {
    // Создаем тестового пользователя через UserRepository
    User testUser = createTestUser(
      "testuser@example.com",
      "testuser",
      "hashedpassword123",
      1,
      "TestCountry",
      "TestCity",
      "Test description for main user"
    );
    testUserId = testUser.getId();

    // Создаем администратора через UserRepository
    User adminUser = createTestUser(
      "adminuser@example.com",
      "adminuser",
      "hashedpassword456",
      2,
      "AdminCountry",
      "AdminCity",
      "Admin user description"
    );
    testAdminUserId = adminUser.getId();

    // Создаем основную тестовую комнату через RoomRepository
    Room testRoom = createTestRoom(
      "Test Room",
      "Test Room Description",
      Category.SPORT, // Предполагаем, что SPORT имеет id = 1
      testAdminUserId,
      10
    );
    testRoomId = testRoom.getId();

    // Добавляем тестового пользователя в комнату как участника через RoomRepository
    roomRepository.addUserToRoom(testRoomId, testUserId, Role.PARTICIPANT);

    // Создаем вторую комнату для теста "когда пользователь не в комнате"
    Room otherRoom = createTestRoom(
      "Other Room",
      "Other Room Description",
      Category.MUSIC, // Предполагаем, что MUSIC имеет id = 2
      testAdminUserId,
      5
    );
    otherRoomId = otherRoom.getId();

    System.out.println("Successfully created test data:");
    System.out.println(" - Test user ID: " + testUserId);
    System.out.println(" - Admin user ID: " + testAdminUserId);
    System.out.println(" - Test room ID: " + testRoomId);
    System.out.println(" - Other room ID: " + otherRoomId);
  }

  private User createTestUser(String login, String username, String password, Integer avatarId,
                              String country, String city, String description) {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setLogin(login);
    request.setUserName(username);
    request.setPassword(password);
    request.setDateOfBirth(Instant.now().minus(25 * 365, ChronoUnit.DAYS));
    request.setCountry(country);
    request.setCity(city);
    request.setDescription(description);
    request.setAvatarId(avatarId);

    return userRepository.createUser(request);
  }

  private Room createTestRoom(String name, String description, Category category, Integer ownerId, Integer maxPeople) {
    RoomCreationRequest request = new RoomCreationRequest();
    request.setIsPublic(true);
    request.setChatLink(name.toLowerCase().replace(" ", "-") + "-chat");
    request.setCategoryId(category.ordinal() + 1); // Преобразуем enum в ID
    request.setName(name);
    request.setDescription(description);
    request.setDateOfStartEvent(Instant.now().plus(1, ChronoUnit.DAYS));
    request.setDateOfEndEvent(Instant.now().plus(2, ChronoUnit.DAYS));
    request.setAgeRating(12);
    request.setMaximumNumberOfPeople(maxPeople);

    return roomRepository.createRoom(ownerId, request);
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

    // Сначала назначаем админскую роль через RoomRepository
    roomRepository.setRoleByUserIdAndRoomId(testUserId, testRoomId, Role.ADMIN);

    ApiResponse<Void> response = userController.demoteAdminRole(ownerToken, testRoomId, testUserId);

    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertNull(response.getData());
  }

  @Test
  void contextLoads_AllBeansInjectedSuccessfully() {
    assertNotNull(userController);
    assertNotNull(tokenService);
    assertNotNull(userRepository);
    assertNotNull(roomRepository);
    assertNotNull(roomsRequestRepository);
  }

  // Дополнительные тесты для работы с заявками на вступление в комнаты
  @Test
  void createAndProcessRoomRequest_ReturnsSuccess() {
    // Создаем заявку на вступление в другую комнату
    RoomsRequest request = roomsRequestRepository.createRequest(
      testUserId,
      otherRoomId,
      RequestStatus.CONSIDERATION
    );

    assertNotNull(request);
    assertEquals(testUserId, request.getUser().getId());
    assertEquals(otherRoomId, request.getRoom().getId());
    assertEquals(RequestStatus.CONSIDERATION, request.getStatus());

    // Обновляем статус заявки
    RoomsRequest updatedRequest = roomsRequestRepository.updateRequest(
      request.getId(),
      RequestStatus.ACCEPTED
    );

    assertNotNull(updatedRequest);
    assertEquals(RequestStatus.ACCEPTED, updatedRequest.getStatus());

    // Получаем заявки пользователя
    List<RoomsRequest> userRequests = roomsRequestRepository.getRequestsByUser(testUserId);
    assertFalse(userRequests.isEmpty());

    // Получаем заявки комнаты
    List<RoomsRequest> roomRequests = roomsRequestRepository.getRoomRequests(otherRoomId);
    assertFalse(roomRequests.isEmpty());

    // Удаляем заявку
    roomsRequestRepository.deleteRequest(request.getId());
  }
}