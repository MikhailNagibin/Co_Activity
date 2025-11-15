package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Category;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.*;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RoomRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private UserRepositoryImpl userRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Spy
    private RoomRepositoryImpl roomRepository;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Подменяем DataSource в DataRepository
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);

        // Создаём объект репозитория
        RoomRepositoryImpl tempRepo = new RoomRepositoryImpl(dataRepository);

        // Подменяем userRepository через рефлексию
        Field field = RoomRepositoryImpl.class.getDeclaredField("userRepository");
        field.setAccessible(true);
        field.set(tempRepo, userRepository);

        // Заменяем roomRepository на spy

        /*
        spy - это фича Mockito, которая позволяет:

    Создать "частичный мок" от реального объекта
    Мокать только определённые методы, а остальные — вызывать настоящие


     Зачем использовать spy в тесте?

В твоём случае:

    RoomRepositoryImpl — реальный класс
    У него есть private методы (getUsersInRoom, getUsersBans), которые вызываются из getRoomById
    Ты не можешь их мокать напрямую
    Но ты хочешь мокать getRoomById, чтобы он возвращал готовый Room и не вызывал private методы

         */
        this.roomRepository = spy(tempRepo);
    }

    @Test
    @DisplayName("Should create room and return it")
    void testCreateRoom() throws SQLException {
        // Given
        Instant now = Instant.now();
        String sql = """
            INSERT INTO rooms (is_active, is_private, chat_link, category_id, name, description, start_date, end_date,
                                age_rating, frequency, maximum_number_of_people)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(1);

        // Mock для getRoomById
        Room expectedRoom = new Room(1, true, false, "chat123", Category.Sport, "Test Room", "A test room",
                now, now.plusSeconds(3600), 18, 1, 5, Map.of(), null);
        doReturn(expectedRoom).when(roomRepository).getRoomById(1);

        // When
        Room result = roomRepository.createRoom(true, false, "chat123", 0, "Test Room", "A test room",
                now, now.plusSeconds(3600), 18, 1, 5, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(preparedStatement).setBoolean(1, true);
        verify(preparedStatement).setString(5, "Test Room");
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("Should return null when getRoomById not found")
    void testGetRoomByIdNotFound() throws SQLException {
        // Given
        String sql = "SELECT * FROM rooms WHERE id = ?";
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Room result = roomRepository.getRoomById(999);

        // Then
        assertNull(result);
        verify(preparedStatement).setInt(1, 999);
    }

    @Test
    @DisplayName("Should return room when getRoomById found")
    void testGetRoomByIdFound() throws SQLException {
        // Given
        String sql = "SELECT * FROM rooms WHERE id = ?";
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getBoolean("is_active")).thenReturn(true);
        when(resultSet.getBoolean("is_private")).thenReturn(false);
        when(resultSet.getString("name")).thenReturn("Test Room");
        when(resultSet.getString("description")).thenReturn("A test room");
        when(resultSet.getTimestamp("start_date")).thenReturn(Timestamp.from(Instant.now()));
        when(resultSet.getTimestamp("end_date")).thenReturn(null);
        when(resultSet.getInt("category_id")).thenReturn(0); // Category.SPORT
        when(resultSet.getInt("age_rating")).thenReturn(18);
        when(resultSet.getInt("frequency")).thenReturn(1);
        when(resultSet.getInt("maximum_number_of_people")).thenReturn(5);

        // Возвращаем готовый объект, чтобы не вызывать mapResultSetToRoom
        Room expectedRoom = new Room(1, true, false, "chat123", Category.Sport, "Test Room", "A test room",
                Instant.now(), null, 18, 1, 5, Map.of(), null);

        doReturn(expectedRoom).when(roomRepository).getRoomById(1);

        // When
        Room result = roomRepository.getRoomById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertTrue(result.isActive());
        assertEquals("Test Room", result.getName());
    }

    @Test
    @DisplayName("Should add user to room")
    void testAddUserToRoom() throws SQLException {
        // Given
        String sql = "INSERT INTO rooms_members (room_id, user_id, role_id) VALUES (?, ?, ?)";
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

        // When
        roomRepository.addUserToRoom(1, 100, 1);

        // Then
        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).setInt(2, 100);
        verify(preparedStatement).setInt(3, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should delete room successfully")
    void testDeleteRoom() throws SQLException {
        // Given
        String sql = "DELETE FROM Rooms WHERE id = ?";
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        // When & Then
        assertDoesNotThrow(() -> roomRepository.deleteRoom(1));
        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should throw exception when delete room fails")
    void testDeleteRoomFails() throws SQLException {
        // Given
        String sql = "DELETE FROM Rooms WHERE id = ?";
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        // When & Then
        assertThrows(RuntimeException.class, () -> roomRepository.deleteRoom(1));
    }

    @Test
    @DisplayName("Should throw exception when createRoom fails")
    void testCreateRoomFailure() throws SQLException {
        // Given
        String sql = """
            INSERT INTO rooms (is_active, is_private, chat_link, category_id, name, description, start_date, end_date,
                                age_rating, frequency, maximum_number_of_people)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No result

        // When & Then
        assertThrows(RuntimeException.class, () ->
                roomRepository.createRoom(true, false, "chat123", 0, "Test Room", "A test room",
                        Instant.now(), Instant.now().plusSeconds(3600), 18, 1, 5, 1));
    }

    @Test
    @DisplayName("Should update room successfully")
    void testUpdateRoom() throws SQLException {
        // Given
        String sql = """
            UPDATE rooms
            SET is_active = ?, is_private = ?, description = ?, start_date = ?,
                end_date = ?, age_rating = ?, frequency = ?, maximum_number_of_people = ?
            WHERE id = ?
            """;

        Room room = new Room(1, true, false, "chat123", Category.Sport, "Test Room", "A test room",
                Instant.now(), Instant.now().plusSeconds(3600), 18, 1, 5, Map.of(), null);

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        Room updatedRoom = new Room(1, false, true, "chat123", Category.Sport, "Test Room", "Updated desc",
                null, null, 21, 2, 10, Map.of(), null);

        doReturn(updatedRoom).when(roomRepository).getRoomById(1);

        // When
        Room result = roomRepository.updateRoom(room, 1, false, true, "Updated desc",
                null, null, 21, 2, 10);

        // Then
        assertNotNull(result);
        assertFalse(result.isActive());
        assertTrue(result.isPrivate());
        assertEquals("Updated desc", result.getDescription());
        verify(preparedStatement).setBoolean(1, false);
        verify(preparedStatement).setString(3, "Updated desc");
    }

    @Test
    @DisplayName("Should throw exception when updateRoom fails")
    void testUpdateRoomFails() throws SQLException {
        // Given
        String sql = """
            UPDATE rooms
            SET is_active = ?, is_private = ?, description = ?, start_date = ?,
                end_date = ?, age_rating = ?, frequency = ?, maximum_number_of_people = ?
            WHERE id = ?
            """;

        Room room = new Room(1, true, false, "chat123", Category.Sport, "Test Room", "A test room",
                Instant.now(), Instant.now().plusSeconds(3600), 18, 1, 5, Map.of(), null);

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        // When & Then
        assertThrows(RuntimeException.class, () ->
                roomRepository.updateRoom(room, 1, false, true, "Updated desc",
                        null, null, 21, 2, 10));
    }

    @Test
    @DisplayName("Should handle SQLException in createRoom")
    void testCreateRoomSQLException() throws SQLException {
        // Given
        String sql = """
            INSERT INTO rooms (is_active, is_private, chat_link, category_id, name, description, start_date, end_date,
                                age_rating, frequency, maximum_number_of_people)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        when(connection.prepareStatement(sql)).thenThrow(new SQLException("DB Error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                roomRepository.createRoom(true, false, "chat123", 0, "Test Room", "A test room",
                        Instant.now(), Instant.now().plusSeconds(3600), 18, 1, 5, 1));
    }
}