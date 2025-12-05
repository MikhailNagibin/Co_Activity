//package com.coactivity;
//
//import com.coactivity.controller.dto.request.*;
//import com.coactivity.controller.dto.response.ApiResponse;
//import com.coactivity.controller.dto.response.UserProfileResponse;
//import com.coactivity.controller.impl.UserControllerImpl;
//import com.coactivity.domain.*;
//import com.coactivity.repository.impl.*;
//import com.coactivity.service.TokenService;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@Testcontainers
//@SpringBootTest(classes = CoActivityPlatformApplication.class)
//public class TestUpdateProfile {
//
//  @Container
//  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
//    .withDatabaseName("postgres_db")
//    .withUsername("postgres")
//    .withPassword("qwerty");
//
//  @DynamicPropertySource
//  static void configureProperties(DynamicPropertyRegistry registry) {
//    registry.add("spring.datasource.url", () -> "jdbc:" + postgres.getJdbcUrl());
//    registry.add("spring.datasource.username", postgres::getUsername);
//    registry.add("spring.datasource.password", postgres::getPassword);
//  }
//
//  @Autowired
//  private UserControllerImpl userController;
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
//  private String validToken;
//  private User testUser;
//
//  @BeforeEach
//  void setUp() {
//    clearTestData();
//
//    testUser = createTestUser(
//      "testuser@example.com",
//      "testuser",
//      "hashedpassword123",
//      1,
//      "TestCountry",
//      "TestCity",
//      "Test description for main user"
//    );
//  }
//
//  @AfterEach
//  void tearDown() {
//    clearTestData();
//  }
//
//  private void clearTestData() {
//    List<Integer> allUsers = userRepository.getAllUsers();
//    System.out.println(allUsers);
//    for (Integer room : allUsers) {
//      userRepository.deleteUser(room);
//    }
//  }
//
//  @Test
//  void updateProfile() {
////    List<Integer> allRooms = userRepository.getAllUsers();
////    System.out.println(allRooms);
////    System.out.println(testUser.getId());
////    User user = userRepository.getUserById(allRooms.getFirst());
////    System.out.println(user.getLogin() + " " + user.getPassword());
//
//
//    // Создаем токен для тестового пользователя
//    validToken = tokenService.createToken(testUser.getId());
//
//    // Входим в личный кабинет
//    var profileResponse = userController.getUserProfile(validToken);
//
//    assertNotNull(profileResponse, "Profile response should not be null");
//    assertNotNull(profileResponse.getBody(), "Profile data should not be null - check if token is valid and user exists");
//
//    UserProfileResponse profile = profileResponse.getBody();
//
//    // Изменяем профиль
//    UserProfileUpdateRequest request = new UserProfileUpdateRequest();
//    request.setUsername("updatedUsername");
//    request.setCity("Updated City");
//    request.setCountry("Updated Country");
//    request.setDescription("Updated description with valid length");
//    request.setDateOfBirth(Instant.now().minus(30 * 365, ChronoUnit.DAYS));
//
//    // Отправляем запрос на изменение
//    ApiResponse<String> response = userController.updateUserProfile(validToken, request);
//
//    // Проверяем успешность обновления
//    assertNotNull(response, "Update response should not be null");
//    assertTrue(response.isSuccess(), "Update should be successful");
//
//    // Получаем обновленный профиль
//    var updatedProfileResponse = userController.getUserProfile(validToken);
//
//    // Проверяем корректность ответа
//    assertNotNull(updatedProfileResponse, "Updated profile response should not be null");
//    assertNotNull(updatedProfileResponse.getBody(), "Updated profile data should not be null");
//
//    UserProfileResponse updatedProfile = updatedProfileResponse.getBody();
//    assertEquals("updatedUsername", updatedProfile.getUsername());
//    assertEquals("Updated City", updatedProfile.getCity());
//    assertEquals("Updated Country", updatedProfile.getCountry());
//    assertEquals("Updated description with valid length", updatedProfile.getDescription());
//    assertEquals(1, updatedProfile.getAvatarId());
//    assertEquals("testuser@example.com", updatedProfile.getLogin());
//  }
//
//  private User createTestUser(String login, String username, String password, Integer avatarId,
//                              String country, String city, String description) {
//    UserRegistrationRequest request = new UserRegistrationRequest();
//    request.setLogin(login);
//    request.setUserName(username);
//    request.setPassword(password);
//    request.setDateOfBirth(Instant.now().minus(25 * 365, ChronoUnit.DAYS));
//    request.setCountry(country);
//    request.setCity(city);
//    request.setDescription(description);
//    request.setAvatarId(avatarId);
//
//    return userRepository.createUser(request);
//  }
//}