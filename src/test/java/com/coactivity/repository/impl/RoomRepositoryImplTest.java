// test/java/com/coactivity/repository/impl/RoomRepositoryImplTest.java
package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private UserRepositoryImpl userRepository;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private RoomRepositoryImpl roomRepository;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() throws SQLException {
        // Создаем тестовую базу данных в памяти
        testDataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();

        when(dataRepository.getDataSource()).thenReturn(testDataSource);

        // Создаем реальный экземпляр с мокками
        roomRepository = new RoomRepositoryImpl(dataRepository);

        // Используем reflection чтобы подменить userRepository
        try {
            var field = RoomRepositoryImpl.class.getDeclaredField("userRepository");
            field.setAccessible(true);
            field.set(roomRepository, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createRoom_ShouldCreateRoom_WhenValidData() throws SQLException {
        // Arrange
        Instant startDate = Instant.now().plusSeconds(3600);
        Instant endDate = Instant.now().plusSeconds(7200);
        var users = new AbstractMap.SimpleEntry<>(1, 1); // userId=1, roleId=1

        // Mock User для избежания NPE
        User mockUser = new User(1, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(anyInt())).thenReturn(mockUser);

        // Act
        Room result = roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Test Description", startDate, endDate, 12, 1, 10, users
        );

        // Assert
        assertNotNull(result);
        assertEquals("Test Room", result.getName());
        assertEquals("Test Description", result.getDescription());
    }

    @Test
    void getRoomById_ShouldReturnRoom_WhenRoomExists() throws SQLException {
        // Arrange
        int roomId = 1;

        // Сначала создаем комнату
        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(1, 1);

        User mockUser = new User(1, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(anyInt())).thenReturn(mockUser);

        Room createdRoom = roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Description", startDate, null, 12, 1, 10, users
        );

        // Act
        Room result = roomRepository.getRoomById(createdRoom.getId());

        // Assert
        assertNotNull(result);
        assertEquals(createdRoom.getId(), result.getId());
        assertEquals("Test Room", result.getName());
    }

    @Test
    void getRoomById_ShouldReturnNull_WhenRoomNotExists() {
        // Arrange
        int nonExistentRoomId = 999;

        // Act
        Room result = roomRepository.getRoomById(nonExistentRoomId);

        // Assert
        assertNull(result);
    }

    @Test
    void updateRoom_ShouldUpdateRoom_WhenValidData() throws SQLException {
        // Arrange
        int roomId = 1;
        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(1, 1);

        User mockUser = new User(1, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(anyInt())).thenReturn(mockUser);

        Room originalRoom = roomRepository.createRoom(
                true, false, "chat-link", 1, "Original Room",
                "Original Description", startDate, null, 12, 1, 10, users
        );

        // Act
        Room updatedRoom = roomRepository.updateRoom(
                originalRoom, roomId, false, true, "Updated Description",
                startDate.plusSeconds(3600), null, 16, 2, 15
        );

        // Assert
        assertNotNull(updatedRoom);
        assertEquals("Updated Description", updatedRoom.getDescription());
        assertEquals(16, updatedRoom.getAgeRating());
        assertEquals(15, updatedRoom.getMaximumNumberOfPeople());
        assertFalse(updatedRoom.isActive());
        assertTrue(updatedRoom.isPrivate());
    }

    @Test
    void addUserToRoom_ShouldAddUser_WhenValidData() throws SQLException {
        // Arrange
        int roomId = 1;
        int userId = 2;
        int roleId = 2; // Participant

        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(1, 1);

        User mockUser = new User(1, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(anyInt())).thenReturn(mockUser);

        roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Description", startDate, null, 12, 1, 10, users
        );

        // Act & Assert
        assertDoesNotThrow(() -> {
            roomRepository.addUserToRoom(roomId, userId, roleId);
        });
    }

    @Test
    void deleteRoom_ShouldDeleteRoom_WhenRoomExists() throws SQLException {
        // Arrange
        int roomId = 1;
        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(1, 1);

        User mockUser = new User(1, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(anyInt())).thenReturn(mockUser);

        roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Description", startDate, null, 12, 1, 10, users
        );

        // Act & Assert
        assertDoesNotThrow(() -> {
            roomRepository.deleteRoom(roomId);
        });

        // Verify room was deleted
        Room deletedRoom = roomRepository.getRoomById(roomId);
        assertNull(deletedRoom);
    }

    @Test
    void isUserInMembers_ShouldReturnTrue_WhenUserIsMember() throws SQLException {
        // Arrange
        int roomId = 1;
        int userId = 1;
        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(userId, 1); // Add user as member

        User mockUser = new User(userId, "test", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        when(userRepository.getUserById(userId)).thenReturn(mockUser);

        roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Description", startDate, null, 12, 1, 10, users
        );

        // Act
        boolean result = roomRepository.isUserInMembers(roomId, userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isUserInMembers_ShouldReturnFalse_WhenUserIsNotMember() throws SQLException {
        // Arrange
        int roomId = 1;
        int userId = 1;
        int nonMemberUserId = 2;
        Instant startDate = Instant.now().plusSeconds(3600);
        var users = new AbstractMap.SimpleEntry<>(userId, 1);

        User memberUser = new User(userId, "member", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);
        User nonMemberUser = new User(nonMemberUserId, "nonmember", "username2", "pass",
                Instant.now(), "country", "city", "desc", 1, null, null);

        when(userRepository.getUserById(userId)).thenReturn(memberUser);
        when(userRepository.getUserById(nonMemberUserId)).thenReturn(nonMemberUser);

        roomRepository.createRoom(
                true, false, "chat-link", 1, "Test Room",
                "Description", startDate, null, 12, 1, 10, users
        );

        // Act
        boolean result = roomRepository.isUserInMembers(roomId, nonMemberUserId);

        // Assert
        assertFalse(result);
    }
}