package com.coactivity.controller;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.Role;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = CoActivityApplication.class)
public class USBlock3Test {

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
    private UserControllerImpl userController;

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
    private Integer room1Id;
    private Integer room2Id;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/init_complete.sql"));
        }
        for (Integer userId : userRepository.getAllUsers()) {
            userRepository.deleteUser(userId);
        }
        for (Room room : roomRepository.getAllRooms()) {
            roomRepository.deleteRoom(room.getId());
        }

        User testUser = createTestUser("testuser@example.com", "testuser", "password");
        testUserId = testUser.getId();

        Room room1 = createTestRoom("Public Room 1", true, testUser.getId());
        room1Id = room1.getId();
        Room room2 = createTestRoom("Private Room 1", false, testUser.getId());
        room2Id = room2.getId();

        roomRepository.addUserToRoom(room1Id, testUserId, Role.OWNER);
        roomRepository.addUserToRoom(room2Id, testUserId, Role.OWNER);


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
        request.setCategory("Sport");
        return roomRepository.createRoom(ownerId, request);
    }

    @Test
    void US301_getRooms() {
        ResponseEntity<List<RoomSummaryResponse>> response = roomController.getRooms(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Public Room 1", response.getBody().get(0).getName());
    }

    @Test
    void US302_getRoomById() {
        ResponseEntity<RoomDetailedResponse> response = roomController.getRoomById(room1Id, validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(room1Id, response.getBody().getId());
        assertEquals("Public Room 1", response.getBody().getName());
    }

    @Test
    void US303_getUserRooms() {
        ResponseEntity<List<RoomDetailedResponse>> response = roomController.getUserRooms(validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void US304_getBanRooms() {
        roomRepository.addUserBan(room1Id, testUserId);
        ResponseEntity<List<RoomSummaryResponse>> response = userController.getBanRooms(validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(room1Id, response.getBody().get(0).getId());
    }
}
