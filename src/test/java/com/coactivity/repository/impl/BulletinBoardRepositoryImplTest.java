package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulletinBoardRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private BulletinBoardRepositoryImpl bulletinBoardRepository;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    private User createMockUser(int id) {
        return new User(
                id,
                "user" + id,
                "User " + id,
                "password123",
                Instant.now().minusSeconds(86400 * 365 * 25),
                "City " + id,
                "Country " + id,
                "Description of user " + id,
                1,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private Room createMockRoom(int id) {
        Map<User, Role> users = new HashMap<>();
        users.put(createMockUser(1), Role.Owner);

        List<User> bans = new ArrayList<>();

        return new Room(
                id,
                true,
                true,
                "chat-link-" + id,
                Category.Sport, // Используем enum Category
                "Room " + id,
                "Description of room " + id,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                18,
                0,
                10,
                users,
                bans
        );
    }

    @Test
    void createBulletinBoard_Success() throws SQLException {
        // Arrange
        int roomId = 1;
        String content = "Test bulletin board content";
        int authorId = 2;
        int generatedBoardId = 10;
        Instant createdAt = Instant.now();

        User mockAuthor = createMockUser(authorId);
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedBoardId);
            when(resultSet.getTimestamp(2)).thenReturn(Timestamp.from(createdAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.createBulletinBoard(roomId, content, authorId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedBoardId, result.getId());
            assertEquals(mockRoom, result.getRoom());
            assertEquals(content, result.getContent());
            assertEquals(mockAuthor, result.getAuthor());
            assertEquals(createdAt, result.getUpdatedAt());

            verify(preparedStatement).setInt(1, roomId);
            verify(preparedStatement).setString(2, content);
            verify(preparedStatement).setInt(3, authorId);
        }
    }

    @Test
    void createBulletinBoard_NoResultSet_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.createBulletinBoard(1, "content", 2)
            );
        }
    }

    @Test
    void createBulletinBoard_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.createBulletinBoard(1, "content", 2)
            );
        }
    }

    @Test
    void updateBulletinBoard_Success() throws SQLException {
        // Arrange
        int roomId = 1;
        String newContent = "Updated bulletin board content";
        int authorId = 2;
        int boardId = 10;
        Instant updatedAt = Instant.now();

        User mockAuthor = createMockUser(authorId);
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(boardId);
            when(resultSet.getTimestamp(2)).thenReturn(Timestamp.from(updatedAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.updateBulletinBoard(roomId, newContent, authorId);

            // Assert
            assertNotNull(result);
            assertEquals(boardId, result.getId());
            assertEquals(mockRoom, result.getRoom());
            assertEquals(newContent, result.getContent());
            assertEquals(mockAuthor, result.getAuthor());
            assertEquals(updatedAt, result.getUpdatedAt());

            verify(preparedStatement).setString(1, newContent);
            verify(preparedStatement).setInt(2, authorId);
            verify(preparedStatement).setInt(3, roomId);
        }
    }

    @Test
    void updateBulletinBoard_NoResultSet_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.updateBulletinBoard(1, "updated content", 2)
            );
        }
    }

    @Test
    void getBulletinBoard_Success() throws SQLException {
        // Arrange
        int roomId = 1;
        int boardId = 10;
        String content = "Bulletin board content";
        int authorId = 2;
        Instant updatedAt = Instant.now();

        User mockAuthor = createMockUser(authorId);
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getInt("id")).thenReturn(boardId);
            when(resultSet.getInt("room_id")).thenReturn(roomId);
            when(resultSet.getString("content")).thenReturn(content);
            when(resultSet.getInt("author_id")).thenReturn(authorId);
            when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(updatedAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.getBulletinBoard(roomId);

            // Assert
            assertNotNull(result);
            assertEquals(boardId, result.getId());
            assertEquals(mockRoom, result.getRoom());
            assertEquals(content, result.getContent());
            assertEquals(mockAuthor, result.getAuthor());
            assertEquals(updatedAt, result.getUpdatedAt());

            verify(preparedStatement).setInt(1, roomId);
        }
    }

    @Test
    void getBulletinBoard_NotFound_ReturnsNull() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            BulletinBoard result = bulletinBoardRepository.getBulletinBoard(roomId);

            // Assert
            assertNull(result);
        }
    }

    @Test
    void getBulletinBoard_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.getBulletinBoard(roomId)
            );
        }
    }

    @Test
    void deleteBulletinBoard_Success() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act & Assert
            assertDoesNotThrow(() -> bulletinBoardRepository.deleteBulletinBoard(roomId));

            // Assert
            verify(preparedStatement).setInt(1, roomId);
            verify(preparedStatement).executeUpdate();
        }
    }

    @Test
    void deleteBulletinBoard_NoRowsAffected_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.deleteBulletinBoard(roomId)
            );
        }
    }

    @Test
    void deleteBulletinBoard_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored1 =
                     mockConstruction(UserRepositoryImpl.class);
             MockedConstruction<RoomRepositoryImpl> ignored2 =
                     mockConstruction(RoomRepositoryImpl.class)) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    bulletinBoardRepository.deleteBulletinBoard(roomId)
            );
        }
    }

    @Test
    void createBulletinBoard_WithDifferentCategories() throws SQLException {
        // Arrange
        int roomId = 1;
        String content = "Test content";
        int authorId = 2;
        int generatedBoardId = 10;
        Instant createdAt = Instant.now();

        User mockAuthor = createMockUser(authorId);

        // Комната с разными категориями
        Room musicRoom = createRoomWithCategory(roomId, Category.Music);
        Room businessRoom = createRoomWithCategory(roomId, Category.Business);
        Room educationRoom = createRoomWithCategory(roomId, Category.Education);

        // Test with Music category
        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(musicRoom))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedBoardId);
            when(resultSet.getTimestamp(2)).thenReturn(Timestamp.from(createdAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.createBulletinBoard(roomId, content, authorId);

            // Assert
            assertNotNull(result);
            assertEquals(Category.Music, result.getRoom().getCategory());
        }
    }

    @Test
    void createBulletinBoard_WithDifferentRoles() throws SQLException {
        // Arrange
        int roomId = 1;
        String content = "Test content";
        int authorId = 2;
        int generatedBoardId = 10;
        Instant createdAt = Instant.now();

        User mockAuthor = createMockUser(authorId);

        // Комната с разными ролями пользователей
        Map<User, Role> usersWithDifferentRoles = new HashMap<>();
        usersWithDifferentRoles.put(createMockUser(1), Role.Owner);
        usersWithDifferentRoles.put(createMockUser(2), Role.Admin);
        usersWithDifferentRoles.put(createMockUser(3), Role.Participant);

        Room roomWithMultipleRoles = new Room(
                roomId,
                true,
                true,
                "chat-link",
                Category.Sport,
                "Room with multiple roles",
                "Description",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                18,
                0,
                10,
                usersWithDifferentRoles,
                new ArrayList<>()
        );

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(roomWithMultipleRoles))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedBoardId);
            when(resultSet.getTimestamp(2)).thenReturn(Timestamp.from(createdAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.createBulletinBoard(roomId, content, authorId);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getRoom().getUsers().size());
            assertTrue(result.getRoom().getUsers().containsValue(Role.Owner));
            assertTrue(result.getRoom().getUsers().containsValue(Role.Admin));
            assertTrue(result.getRoom().getUsers().containsValue(Role.Participant));
        }
    }

    @Test
    void getBulletinBoard_WithAllCategoryValues() throws SQLException {
        // Arrange
        int roomId = 1;
        int boardId = 10;
        String content = "Bulletin board content";
        int authorId = 2;
        Instant updatedAt = Instant.now();

        User mockAuthor = createMockUser(authorId);

        // Test all category values
        for (Category category : Category.values()) {
            Room roomWithCategory = createRoomWithCategory(roomId, category);

            try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                         mockConstruction(UserRepositoryImpl.class,
                                 (mock, context) -> when(mock.getUserById(authorId)).thenReturn(mockAuthor));
                 MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                         mockConstruction(RoomRepositoryImpl.class,
                                 (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(roomWithCategory))) {

                bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
                when(preparedStatement.executeQuery()).thenReturn(resultSet);
                when(resultSet.next()).thenReturn(true);

                when(resultSet.getInt("id")).thenReturn(boardId);
                when(resultSet.getInt("room_id")).thenReturn(roomId);
                when(resultSet.getString("content")).thenReturn(content);
                when(resultSet.getInt("author_id")).thenReturn(authorId);
                when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(updatedAt));

                // Act
                BulletinBoard result = bulletinBoardRepository.getBulletinBoard(roomId);

                // Assert
                assertNotNull(result);
                assertEquals(category, result.getRoom().getCategory());
            }
        }
    }

    // Вспомогательный метод для создания комнаты с определенной категорией
    private Room createRoomWithCategory(int roomId, Category category) {
        Map<User, Role> users = new HashMap<>();
        users.put(createMockUser(1), Role.Owner);

        List<User> bans = new ArrayList<>();

        return new Room(
                roomId,
                true,
                true,
                "chat-link-" + roomId,
                category,
                "Room " + roomId,
                "Description of room " + roomId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                18,
                0,
                10,
                users,
                bans
        );
    }

    @Test
    void createBulletinBoard_WithNullUserFromRepository() throws SQLException {
        // Arrange
        int roomId = 1;
        String content = "Test content";
        int authorId = 999; // Несуществующий пользователь
        int generatedBoardId = 10;
        Instant createdAt = Instant.now();

        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(authorId)).thenReturn(null));
             MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedBoardId);
            when(resultSet.getTimestamp(2)).thenReturn(Timestamp.from(createdAt));

            // Act
            BulletinBoard result = bulletinBoardRepository.createBulletinBoard(roomId, content, authorId);

            // Assert
            assertNotNull(result);
            assertNull(result.getAuthor());
            assertEquals(mockRoom, result.getRoom());
        }
    }
}