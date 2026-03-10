package com.coactivity.controller;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.TokenService;
import com.coactivity.service.dto.PendingVerification;
import org.junit.jupiter.api.BeforeEach;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = CoActivityApplication.class)
@Tag("docker")
public class USBlock1Test {

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

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DataSource dataSource;

    private String validToken;
    private Integer testUserId;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/init_tables.sql"));
        }
        for (Integer userId : userRepository.getAllUsers()) {
            userRepository.deleteUser(userId);
        }
        User testUser = createTestUser(
                "testuser@example.com",
                "testuser",
                "hashedpassword123"
        );
        testUserId = testUser.getId();

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
    void US101_registerUser() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setLogin("newuser@example.com");
        request.setUserName("newuser");
        request.setPassword("password123");
        request.setDateOfBirth(Instant.now().minus(20 * 365, ChronoUnit.DAYS));

        ResponseEntity<RegistrationResponse> response = userController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newuser", response.getBody().getUserName());
    }

    @Test
    void US102_loginUser() {
        LoginRequest request = new LoginRequest();
        request.setLogin("testuser@example.com");
        request.setPassword("hashedpassword123");

        ResponseEntity<Void> response = userController.loginUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void US103_verifyLogin() {
        String login = "testuser@example.com";
        String verificationCode = "123456";
        tokenService.addPendingVerification(login, new PendingVerification(Objects.requireNonNull(userRepository.getUserByLogin(login)).getId(), verificationCode, Instant.now().plus(10, ChronoUnit.MINUTES)));

        ResponseEntity<LoginResponse> response = userController.verifyLogin(login, verificationCode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(Objects.requireNonNull(response.getBody()).getToken());
    }

    @Test
    void US104_logoutUser() {
        ResponseEntity<Void> response = userController.logoutUser(validToken);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void US105_deleteAccount() {
        ResponseEntity<Void> response = userController.deleteAccount(validToken);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
