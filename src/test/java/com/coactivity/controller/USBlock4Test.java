package com.coactivity.controller;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.domain.Category;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
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
public class RoomCreationAndManagementTest {

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
    private RoomControllerImpl roomController;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private RoomRepositoryImpl roomRepository;

    @Autowired
    private DataSource dataSource;

    private String validToken;
    private Integer testUserId;
    private Integer roomToJoinId;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/init_tables.sql"));
        }
        userRepository.deleteAll();
        roomRepository.deleteAll();

        User testUser = createTestUser("testuser@example.com", "testuser", "password");
        testUserId = testUser.getId();

        User anotherUser = createTestUser("anotheruser@example.com", "anotheruser", "password");

        Room roomToJoin = createTestRoom("Room to Join", true, anotherUser.getId());
        roomToJoinId = roomToJoin.getId();


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

    private Room createTestRoom(String name, boolean isPublic, Integer ownerId) {
        RoomCreationRequest request = new RoomCreationRequest();
        request.setName(name);
        request.setIsPublic(isPublic);
        request.setCategoryId(Category.SPORT.ordinal());
        return roomRepository.createRoom(ownerId, request);
    }

    @Test
    void US401_createRoom() {
        RoomCreationRequest request = new RoomCreationRequest();
        request.setName("New Room");
        request.setIsPublic(true);
        request.setCategoryId(Category.MUSIC.ordinal());

        ResponseEntity<RoomCreationResponse> response = roomController.createRoom(validToken, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("New Room", roomRepository.getRoom(response.getBody().getRoomId()).getName());
    }

    @Test
    void US402_joinRoom() {
        ResponseEntity<Void> response = roomController.joinRoom(validToken, roomToJoinId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void US403_leaveRoom() {
        roomController.joinRoom(validToken, roomToJoinId); // First join the room
        ResponseEntity<Void> response = roomController.leaveRoom(validToken, roomToJoinId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void US404_deleteRoom() {
        Room roomToDelete = createTestRoom("Room to Delete", true, testUserId);
        ResponseEntity<Void> response = roomController.deleteRoom(validToken, roomToDelete.getId());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(roomRepository.getRoom(roomToDelete.getId()));
    }
}
