package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomsRequestRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private UserRepositoryImpl userRepository;

    @Mock
    private RoomRepositoryImpl roomRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private RoomsRequestRepositoryImpl roomsRequestRepository;

    @BeforeEach
    void setUp() throws SQLException {
        // Настраиваем мок цепочку для DataSource
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);

        // Создаем экземпляр репозитория
        roomsRequestRepository = new RoomsRequestRepositoryImpl(dataRepository);

        // Инжектим моки зависимостей через reflection
        injectMockDependencies();
    }

    private void injectMockDependencies() {
        try {
            var userRepoField = RoomsRequestRepositoryImpl.class.getDeclaredField("userRepository");
            var roomRepoField = RoomsRequestRepositoryImpl.class.getDeclaredField("roomRepository");

            userRepoField.setAccessible(true);
            roomRepoField.setAccessible(true);

            userRepoField.set(roomsRequestRepository, userRepository);
            roomRepoField.set(roomsRequestRepository, roomRepository);

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @Test
    @DisplayName("createRequest - должен создать заявку при валидных данных")
    void createRequest_ShouldCreateRequest_WhenValidData() throws SQLException {
        // Arrange
        int userId = 1;
        int roomId = 1;
        int statusId = 1;

        // Настраиваем моки для успешного выполнения SQL
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("user_id")).thenReturn(userId);
        when(resultSet.getInt("room_id")).thenReturn(roomId);
        when(resultSet.getString("status")).thenReturn("PENDING");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));

        // Моки для доменных объектов
        User mockUser = new User(userId, "testuser", "username", "password",
                Instant.now(), "Country", "City", "Description", 1, List.of(), List.of());
        Room mockRoom = new Room(roomId, true, false, "chat-link", Category.Sport,
                "Test Room", "Description", Instant.now(), null, 12, 1, 10, null, null);

        when(userRepository.getUserById(userId)).thenReturn(mockUser);
        when(roomRepository.getRoomById(roomId)).thenReturn(mockRoom);

        // Act
        RoomsRequest result = roomsRequestRepository.createRequest(userId, roomId, statusId);

        // Assert
        assertNotNull(result, "Заявка не должна быть null");
        assertEquals(mockUser, result.getUser(), "Пользователь должен совпадать");
        assertEquals(mockRoom, result.getRoom(), "Комната должна совпадать");
        assertEquals(RequestStatus.PENDING, result.getStatus(), "Статус должен быть PENDING");
        assertNotNull(result.getCreatedAt(), "Дата создания не должна быть null");

        // Verify SQL execution
        verify(connection).prepareStatement(contains("INSERT INTO rooms_requests"));
        verify(preparedStatement).setInt(1, userId);
        verify(preparedStatement).setInt(2, roomId);
        verify(preparedStatement).setInt(3, statusId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("createRequest - должен бросить исключение при SQL ошибке")
    void createRequest_ShouldThrowException_WhenSQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            roomsRequestRepository.createRequest(1, 1, 1);
        }, "Должно бросаться RuntimeException при SQL ошибке");

        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("createRequest - должен бросить исключение когда нет результата")
    void createRequest_ShouldThrowException_WhenNoResult() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Нет данных в ResultSet

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            roomsRequestRepository.createRequest(1, 1, 1);
        }, "Должно бросаться RuntimeException когда запрос не вернул данные");
    }

    @Test
    @DisplayName("updateRequest - должен обновить статус заявки")
    void updateRequest_ShouldUpdateStatus_WhenValidRequest() throws SQLException {
        // Arrange
        int requestId = 1;
        RequestStatus newStatus = RequestStatus.APPROVED;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("user_id")).thenReturn(1);
        when(resultSet.getInt("room_id")).thenReturn(1);
        when(resultSet.getString("status")).thenReturn("APPROVED");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));

        User mockUser = new User(1, "user1", "user1", "pass", null, null, null, null, 1, null, null);
        Room mockRoom = new Room(1, true, false, "chat", Category.Sport, "Room", "Desc", null, null, 0, 0, 0, null, null);

        when(userRepository.getUserById(1)).thenReturn(mockUser);
        when(roomRepository.getRoomById(1)).thenReturn(mockRoom);

        // Act
        RoomsRequest result = roomsRequestRepository.updateRequest(requestId, newStatus);

        // Assert
        assertNotNull(result);
        assertEquals(RequestStatus.APPROVED, result.getStatus());

        verify(preparedStatement).setString(1, "APPROVED");
        verify(preparedStatement).setInt(2, requestId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("deleteRequest - должен удалить заявку")
    void deleteRequest_ShouldDeleteRequest_WhenValidId() throws SQLException {
        // Arrange
        int requestId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        // Act & Assert
        assertDoesNotThrow(() -> {
            roomsRequestRepository.deleteRequest(requestId);
        }, "Удаление не должно бросать исключение при успешном выполнении");

        verify(preparedStatement).setInt(1, requestId);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("deleteRequest - должен бросить исключение когда заявка не найдена")
    void deleteRequest_ShouldThrowException_WhenRequestNotFound() throws SQLException {
        // Arrange
        int requestId = 999;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roomsRequestRepository.deleteRequest(requestId);
        }, "Должно бросаться исключение когда заявка не найдена");

        assertTrue(exception.getMessage().contains("Request not found"),
                "Сообщение об ошибке должно содержать информацию о ненайденной заявке");
    }

    @Test
    @DisplayName("getRoomRequest - должен вернуть список заявок для комнаты")
    void getRoomRequest_ShouldReturnRequests_ForRoom() throws SQLException {
        // Arrange
        int roomId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Симулируем 2 заявки в ResultSet
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("user_id")).thenReturn(1, 2);
        when(resultSet.getInt("room_id")).thenReturn(roomId, roomId);
        when(resultSet.getString("status")).thenReturn("PENDING", "PENDING");
        when(resultSet.getTimestamp("created_at")).thenReturn(
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now().plusSeconds(10))
        );

        User mockUser1 = new User(1, "user1", "user1", "pass", null, null, null, null, 1, null, null);
        User mockUser2 = new User(2, "user2", "user2", "pass", null, null, null, null, 1, null, null);
        Room mockRoom = new Room(roomId, true, false, "chat", Category.Sport, "Room", "Desc", null, null, 0, 0, 0, null, null);

        when(userRepository.getUserById(1)).thenReturn(mockUser1);
        when(userRepository.getUserById(2)).thenReturn(mockUser2);
        when(roomRepository.getRoomById(roomId)).thenReturn(mockRoom);

        // Act
        List<RoomsRequest> requests = roomsRequestRepository.getRoomRequest(roomId);

        // Assert
        assertNotNull(requests);
        assertEquals(2, requests.size(), "Должно вернуть 2 заявки");

        // Проверяем что все заявки для правильной комнаты
        for (RoomsRequest request : requests) {
            assertEquals(roomId, request.getRoom().getId(), "Все заявки должны быть для указанной комнаты");
        }

        verify(preparedStatement).setInt(1, roomId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("getRoomRequest - должен вернуть пустой список когда заявок нет")
    void getRoomRequest_ShouldReturnEmptyList_WhenNoRequests() throws SQLException {
        // Arrange
        int roomId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Нет данных

        // Act
        List<RoomsRequest> requests = roomsRequestRepository.getRoomRequest(roomId);

        // Assert
        assertNotNull(requests);
        assertTrue(requests.isEmpty(), "Список должен быть пустым когда заявок нет");
    }

    @Test
    @DisplayName("getRequestsByUser - должен вернуть список заявок пользователя")
    void getRequestsByUser_ShouldReturnRequests_ForUser() throws SQLException {
        // Arrange
        int userId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("user_id")).thenReturn(userId, userId);
        when(resultSet.getInt("room_id")).thenReturn(1, 2);
        when(resultSet.getString("status")).thenReturn("PENDING", "APPROVED");
        when(resultSet.getTimestamp("created_at")).thenReturn(
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now().plusSeconds(10))
        );

        User mockUser = new User(userId, "user1", "user1", "pass", null, null, null, null, 1, null, null);
        Room mockRoom1 = new Room(1, true, false, "chat1", Category.Sport, "Room1", "Desc", null, null, 0, 0, 0, null, null);
        Room mockRoom2 = new Room(2, true, false, "chat2", Category.Music, "Room2", "Desc", null, null, 0, 0, 0, null, null);

        when(userRepository.getUserById(userId)).thenReturn(mockUser);
        when(roomRepository.getRoomById(1)).thenReturn(mockRoom1);
        when(roomRepository.getRoomById(2)).thenReturn(mockRoom2);

        // Act
        List<RoomsRequest> requests = roomsRequestRepository.getRequestsByUser(userId);

        // Assert
        assertNotNull(requests);
        assertEquals(2, requests.size(), "Должно вернуть 2 заявки");

        for (RoomsRequest request : requests) {
            assertEquals(userId, request.getUser().getId(), "Все заявки должны быть от указанного пользователя");
        }
    }

    @Test
    @DisplayName("getRequestsByUser - должен бросить исключение при SQL ошибке")
    void getRequestsByUser_ShouldThrowException_WhenSQLFails() throws SQLException {
        // Arrange
        int userId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            roomsRequestRepository.getRequestsByUser(userId);
        });
    }

    @Test
    @DisplayName("mapResultSetToRoomsRequest - должен корректно маппить ResultSet")
    void mapResultSetToRoomsRequest_ShouldMapCorrectly() throws SQLException {
        // Arrange
        int userId = 1;
        int roomId = 1;
        Instant createdAt = Instant.now();

        when(resultSet.getInt("user_id")).thenReturn(userId);
        when(resultSet.getInt("room_id")).thenReturn(roomId);
        when(resultSet.getString("status")).thenReturn("PENDING");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));

        User mockUser = new User(userId, "user1", "user1", "pass", null, null, null, null, 1, null, null);
        Room mockRoom = new Room(roomId, true, false, "chat", Category.Sport, "Room", "Desc", null, null, 0, 0, 0, null, null);

        when(userRepository.getUserById(userId)).thenReturn(mockUser);
        when(roomRepository.getRoomById(roomId)).thenReturn(mockRoom);

        // Act
        // Используем reflection для вызова приватного метода
        RoomsRequest request = callPrivateMapResultSetMethod(resultSet);

        // Assert
        assertNotNull(request);
        assertEquals(mockUser, request.getUser());
        assertEquals(mockRoom, request.getRoom());
        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertEquals(createdAt, request.getCreatedAt());
    }

    // Вспомогательный метод для вызова приватного метода через reflection
    private RoomsRequest callPrivateMapResultSetMethod(ResultSet resultSet) {
        try {
            var method = RoomsRequestRepositoryImpl.class.getDeclaredMethod("mapResultSetToRoomsRequest", ResultSet.class);
            method.setAccessible(true);
            return (RoomsRequest) method.invoke(roomsRequestRepository, resultSet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method", e);
        }
    }
}