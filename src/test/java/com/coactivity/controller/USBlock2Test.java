package com.coactivity.controller;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = CoActivityApplication.class)
public class UserProfileManagementTest {

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
    private DataSource dataSource;

    private String validToken;
    private Integer testUserId;
    private Integer anotherUserId;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/init_tables.sql"));
        }
        userRepository.deleteAll();
        User testUser = createTestUser(
                "testuser@example.com",
                "testuser",
                "hashedpassword123"
        );
        testUserId = testUser.getId();

        User anotherUser = createTestUser(
                "anotheruser@example.com",
                "anotheruser",
                "anotherpassword"
        );
        anotherUserId = anotherUser.getId();

        validToken = tokenService.createToken(testUserId);
        tokenService.registerToken(testUserId, validToken);
    }

    private User createTestUser(String login, String username, String password) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setLogin(login);
        request.setUserName(username);
        request.setPassword(password);
        request.setDateOfBirth(Instant.now().minus(25 * 365, ChronoUnit.DAYS));
        return userRepository.createUser(request);
    }

    @Test
    void US201_getUserProfile() {
        ResponseEntity<UserProfileResponse> response = userController.getUserProfile(validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserId, response.getBody().getId());
        assertEquals("testuser", response.getBody().getUsername());
    }

    @Test
    void US202_getPublicUserProfileById() {
        ResponseEntity<UserSummaryResponse> response = userController.getPublicUserProfileById(validToken, anotherUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(anotherUserId, response.getBody().getId());
        assertEquals("anotheruser", response.getBody().getUsername());
    }

    @Test
    void US203_updateUserProfile() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("updateduser");
        request.setCity("Updated City");

        ResponseEntity<UserProfileResponse> response = userController.updateUserProfile(validToken, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        User updatedUser = userRepository.find(testUserId);
        assertEquals("updateduser", updatedUser.getUsername());
        assertEquals("Updated City", updatedUser.getCity());
    }

    @Test
    void US204_configureNotificationSettings() {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setMembershipAccepted(true);
        request.setMembershipRejected(false);

        ResponseEntity<NotificationSettingsResponse> response = userController.configureNotificationSettings(validToken, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
