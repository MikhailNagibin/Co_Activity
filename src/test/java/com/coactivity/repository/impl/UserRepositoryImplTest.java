package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.*;
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
class UserRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

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

    private UserRepositoryImpl userRepository;

    @BeforeEach
    void setUp() throws SQLException {
        // Настраиваем мок цепочку для DataSource
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);

        // Создаем экземпляр репозитория
        userRepository = new UserRepositoryImpl(dataRepository);

        // Инжектим мок roomRepository через reflection
        injectMockDependencies();
    }

    private void injectMockDependencies() {
        try {
            var roomRepoField = UserRepositoryImpl.class.getDeclaredField("roomRepository");
            roomRepoField.setAccessible(true);
            roomRepoField.set(userRepository, roomRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @Test
    @DisplayName("createUser - должен создать пользователя при валидных данных")
    void createUser_ShouldCreateUser_WhenValidData() throws SQLException {
        // Arrange
        String login = "testuser";
        String username = "testusername";
        String password = "password123";
        Instant birthday = Instant.now().minusSeconds(25 * 365 * 24 * 60 * 60); // 25 years ago
        String country = "Country";
        String city = "City";
        String description = "Test description";
        int avatarId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(1);

        // Act
        User result = userRepository.createUser(login, username, password, birthday, country, city, description, avatarId);

        // Assert
        assertNotNull(result, "Пользователь не должен быть null");
        assertEquals(1, result.getId(), "ID должен быть установлен");
        assertEquals(login, result.getLogin(), "Login должен совпадать");
        assertEquals(username, result.getUsername(), "Username должен совпадать");
        assertEquals(password, result.getPassword(), "Password должен совпадать");
        assertEquals(birthday, result.getDataOfBirth(), "Дата рождения должна совпадать");
        assertEquals(country, result.getCountry(), "Страна должна совпадать");
        assertEquals(city, result.getCity(), "Город должен совпадать");
        assertEquals(description, result.getDescription(), "Описание должно совпадать");
        assertEquals(avatarId, result.getAvatarId(), "Avatar ID должен совпадать");

        // Verify SQL execution
        verify(connection).prepareStatement(contains("INSERT INTO Users"));
        verify(preparedStatement).setString(1, login);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).setTimestamp(3, Timestamp.from(birthday));
        verify(preparedStatement).setString(4, country);
        verify(preparedStatement).setString(5, city);
        verify(preparedStatement).setString(6, description);
        verify(preparedStatement).setInt(7, avatarId);
        verify(preparedStatement).setString(8, username);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("createUser - должен бросить исключение при SQL ошибке")
    void createUser_ShouldThrowException_WhenSQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userRepository.createUser("login", "username", "pass", Instant.now(),
                    "country", "city", "desc", 1);
        }, "Должно бросаться RuntimeException при SQL ошибке");
    }

    @Test
    @DisplayName("createUser - должен бросить исключение когда нет результата")
    void createUser_ShouldThrowException_WhenNoResult() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Нет данных в ResultSet

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userRepository.createUser("login", "username", "pass", Instant.now(),
                    "country", "city", "desc", 1);
        }, "Должно бросаться RuntimeException когда запрос не вернул данные");
    }

    @Test
    @DisplayName("updateUser - должен обновить пользователя с новыми значениями")
    void updateUser_ShouldUpdateUser_WithNewValues() throws SQLException {
        // Arrange
        User existingUser = new User(1, "oldlogin", "oldusername", "oldpass",
                Instant.now(), "oldcountry", "oldcity", "olddesc", 1, List.of(), List.of());

        String newPassword = "newpassword";
        Instant newBirthday = Instant.now().minusSeconds(30 * 365 * 24 * 60 * 60);
        String newCountry = "NewCountry";
        String newCity = "NewCity";
        String newDescription = "New description";
        int newAvatarId = 2;
        String newUsername = "newusername";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> {
            userRepository.updateUser(existingUser, newPassword, newBirthday, newCountry,
                    newCity, newDescription, newAvatarId, newUsername);
        }, "Обновление не должно бросать исключение при успешном выполнении");

        // Assert that user object was updated
        assertEquals(newPassword, existingUser.getPassword(), "Password должен быть обновлен");
        assertEquals(newBirthday, existingUser.getDataOfBirth(), "Дата рождения должна быть обновлена");
        assertEquals(newCountry, existingUser.getCountry(), "Страна должна быть обновлена");
        assertEquals(newCity, existingUser.getCity(), "Город должен быть обновлен");
        assertEquals(newDescription, existingUser.getDescription(), "Описание должно быть обновлено");
        assertEquals(newAvatarId, existingUser.getAvatarId(), "Avatar ID должен быть обновлен");

        verify(preparedStatement).setString(1, newUsername);
        verify(preparedStatement).setString(2, newPassword);
        verify(preparedStatement).setTimestamp(3, Timestamp.from(newBirthday));
        verify(preparedStatement).setString(4, newCountry);
        verify(preparedStatement).setString(5, newCity);
        verify(preparedStatement).setString(6, newDescription);
        verify(preparedStatement).setInt(7, newAvatarId);
        verify(preparedStatement).setInt(8, existingUser.getId());
    }

    @Test
    @DisplayName("updateUser - должен использовать старые значения когда новые null")
    void updateUser_ShouldUseOldValues_WhenNewValuesAreNull() throws SQLException {
        // Arrange
        User existingUser = new User(1, "login", "username", "password",
                Instant.now(), "country", "city", "description", 1, List.of(), List.of());

        String originalPassword = existingUser.getPassword();
        Instant originalBirthday = existingUser.getDataOfBirth();
        String originalCountry = existingUser.getCountry();
        String originalCity = existingUser.getCity();
        String originalDescription = existingUser.getDescription();
        int originalAvatarId = existingUser.getAvatarId();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> {
            userRepository.updateUser(existingUser, null, null, null, null, null, 0, null);
        }, "Обновление не должно бросать исключение при null значениях");

        // Assert that old values are preserved
        assertEquals(originalPassword, existingUser.getPassword(), "Password должен остаться прежним");
        assertEquals(originalBirthday, existingUser.getDataOfBirth(), "Дата рождения должна остаться прежней");
        assertEquals(originalCountry, existingUser.getCountry(), "Страна должна остаться прежней");
        assertEquals(originalCity, existingUser.getCity(), "Город должен остаться прежним");
        assertEquals(originalDescription, existingUser.getDescription(), "Описание должно остаться прежним");
        assertEquals(originalAvatarId, existingUser.getAvatarId(), "Avatar ID должен остаться прежним");
    }

    // ВАЖНО: В коде есть критическая ошибка - метод updateUser всегда бросает исключение
    // после блока catch. Этот тест ожидает исправления кода.
    @Test
    @DisplayName("updateUser - должен бросить исключение при SQL ошибке")
    void updateUser_ShouldThrowException_WhenSQLFails() throws SQLException {
        // Arrange
        User existingUser = new User(1, "login", "username", "pass",
                Instant.now(), "country", "city", "desc", 1, List.of(), List.of());

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userRepository.updateUser(existingUser, "newpass", null, null, null, null, 0, null);
        }, "Должно бросаться RuntimeException при SQL ошибке");
    }

    @Test
    @DisplayName("deleteUser - должен удалить пользователя и связанные данные")
    void deleteUser_ShouldDeleteUserAndRelatedData() throws SQLException {
        // Arrange
        int userId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> {
            userRepository.deleteUser(userId);
        }, "Удаление не должно бросать исключение при успешном выполнении");

        // Verify that all related tables are cleared
        verify(preparedStatement, times(8)).setInt(anyInt(), eq(userId));
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("deleteUser - должен бросить исключение когда пользователь не найден")
    void deleteUser_ShouldThrowException_WhenUserNotFound() throws SQLException {
        // Arrange
        int userId = 999;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userRepository.deleteUser(userId);
        }, "Должно бросаться исключение когда пользователь не найден");
    }

    @Test
    @DisplayName("getUser - должен вернуть пользователя по login и password")
    void getUser_ShouldReturnUser_WhenValidCredentials() throws SQLException {
        // Arrange
        int login = 1; // ОШИБКА В КОДЕ: login должен быть String, а не int!
        String password = "password";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        setupResultSetForUser(1, "user1", "username1", password,
                Instant.now(), "Country", "City", "Description", 1);

        // Настраиваем моки для зависимых методов
        setupRoomAndNotificationMocks(1);

        // Act
        User result = userRepository.getUser(login, password);

        // Assert
        assertNotNull(result, "Пользователь не должен быть null");
        assertEquals(login, result.getId(), "ID должен совпадать");

        verify(preparedStatement).setInt(1, login); // ОШИБКА: setInt для login!
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("getUser - должен вернуть null когда пользователь не найден")
    void getUser_ShouldReturnNull_WhenUserNotFound() throws SQLException {
        // Arrange
        int login = 999;
        String password = "wrongpassword";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Пользователь не найден

        // Act
        User result = userRepository.getUser(login, password);

        // Assert
        assertNull(result, "Должен возвращать null когда пользователь не найден");
    }

    @Test
    @DisplayName("getUserById - должен вернуть пользователя по ID")
    void getUserById_ShouldReturnUser_WhenValidId() throws SQLException {
        // Arrange
        int userId = 1;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        setupResultSetForUser(userId, "user1", "username1", "password",
                Instant.now(), "Country", "City", "Description", 1);

        setupRoomAndNotificationMocks(userId);

        // Act
        User result = userRepository.getUserById(userId);

        // Assert
        assertNotNull(result, "Пользователь не должен быть null");
        assertEquals(userId, result.getId(), "ID должен совпадать");

        verify(preparedStatement).setInt(1, userId);
    }

    @Test
    @DisplayName("getUserById - должен вернуть null когда пользователь не найден")
    void getUserById_ShouldReturnNull_WhenUserNotFound() throws SQLException {
        // Arrange
        int userId = 999;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        User result = userRepository.getUserById(userId);

        // Assert
        assertNull(result, "Должен возвращать null когда пользователь не найден");
    }

    @Test
    @DisplayName("mapResultSetToUser - должен корректно маппить ResultSet в User")
    void mapResultSetToUser_ShouldMapCorrectly() throws SQLException {
        // Arrange
        int userId = 1;
        String login = "testlogin";
        String username = "testusername";
        String password = "testpassword";
        Instant birthday = Instant.now().minusSeconds(25 * 365 * 24 * 60 * 60);
        String country = "TestCountry";
        String city = "TestCity";
        String description = "Test description";
        int avatarId = 5;

        setupResultSetForUser(userId, login, username, password, birthday, country, city, description, avatarId);
        setupRoomAndNotificationMocks(userId);

        // Act
        User result = callPrivateMapResultSetMethod(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(login, result.getLogin());
        assertEquals(username, result.getUsername());
        assertEquals(password, result.getPassword());
        assertEquals(birthday, result.getDataOfBirth());
        assertEquals(country, result.getCountry());
        assertEquals(city, result.getCity());
        assertEquals(description, result.getDescription());
        assertEquals(avatarId, result.getAvatarId());
        assertNotNull(result.getRooms(), "Список комнат не должен быть null");
        assertNotNull(result.getNotifications(), "Список уведомлений не должен быть null");
    }

    // Вспомогательные методы

    private void setupResultSetForUser(int id, String login, String username, String password,
                                       Instant birthday, String country, String city,
                                       String description, int avatarId) throws SQLException {
        when(resultSet.getInt("id")).thenReturn(id);
        when(resultSet.getString("login")).thenReturn(login);
        when(resultSet.getString("username")).thenReturn(username);
        when(resultSet.getString("password")).thenReturn(password);
        when(resultSet.getTimestamp("birthday")).thenReturn(Timestamp.from(birthday));
        when(resultSet.getString("country")).thenReturn(country);
        when(resultSet.getString("city")).thenReturn(city);
        when(resultSet.getString("description")).thenReturn(description);
        when(resultSet.getInt("avatar_id")).thenReturn(avatarId);
    }

    private void setupRoomAndNotificationMocks(int userId) throws SQLException {
        // Мок для getRooms
        when(connection.prepareStatement(contains("roomId from Rooms_members"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Симулируем пустой список комнат
        when(resultSet.next()).thenReturn(false);

        // Мок для getNotification
        when(connection.prepareStatement(contains("notificationId from usersNotification"))).thenReturn(preparedStatement);

        // Мок для roomRepository.getRoomById (не будет вызываться т.к. нет комнат)
        Room mockRoom = new Room(1, true, false, "chat", Category.Sport,
                "Room", "Desc", Instant.now(), null, 12, 1, 10, null, null);
        when(roomRepository.getRoomById(anyInt())).thenReturn(mockRoom);
    }

    private User callPrivateMapResultSetMethod(ResultSet resultSet) {
        try {
            var method = UserRepositoryImpl.class.getDeclaredMethod("mapResultSetToUser", ResultSet.class);
            method.setAccessible(true);
            return (User) method.invoke(userRepository, resultSet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method", e);
        }
    }
}