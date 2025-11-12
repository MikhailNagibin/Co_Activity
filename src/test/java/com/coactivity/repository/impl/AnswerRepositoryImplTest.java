package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Answer;
import com.coactivity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerRepositoryImplTest {

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

    private AnswerRepositoryImpl answerRepository;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    private User createMockUser(int id) {
        return new User(
                id,                    // id
                "user" + id,           // login
                "User " + id,          // username
                "password123",         // password
                Instant.now().minusSeconds(86400 * 365 * 25), // dataOfBirth (25 years ago)
                "City " + id,          // city
                "Country " + id,       // country
                "Description of user " + id, // description
                1,                     // avatarId
                Collections.emptyList(), // rooms
                Collections.emptyList()  // notifications
        );
    }

    private User createMockUser(int id, String username) {
        return new User(
                id,
                "login" + id,
                username,
                "password123",
                Instant.parse("1990-01-01T00:00:00Z"), // dataOfBirth
                "TestCity",
                "TestCountry",
                "Test description for " + username,
                1,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    void createAnswer_Success() throws SQLException {
        // Arrange
        int questionId = 1;
        int previousAnswerId = 2;
        String currentAnswer = "Test answer";
        int ownerId = 3;
        int generatedAnswerId = 10;
        User mockUser = createMockUser(ownerId, "testuser");

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(ownerId)).thenReturn(mockUser))) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedAnswerId);

            // Act
            Answer result = answerRepository.createAnswer(questionId, previousAnswerId, currentAnswer, ownerId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedAnswerId, result.getId());
            assertEquals(questionId, result.getQuestionId());
            assertEquals(previousAnswerId, result.getPreviousAnswerId());
            assertEquals(currentAnswer, result.getAnswer());
            assertEquals(mockUser, result.getOwner());

            verify(preparedStatement).setInt(1, questionId);
            verify(preparedStatement).setInt(2, previousAnswerId);
            verify(preparedStatement).setString(3, currentAnswer);
            verify(preparedStatement).setInt(4, ownerId);
        }
    }

    @Test
    void createAnswer_WithNullPreviousAnswer() throws SQLException {
        // Arrange
        int questionId = 1;
        int previousAnswerId = 0; // 0 может означать NULL в вашей логике
        String currentAnswer = "Root answer";
        int ownerId = 3;
        int generatedAnswerId = 10;
        User mockUser = createMockUser(ownerId, "testuser");

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(ownerId)).thenReturn(mockUser))) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedAnswerId);

            // Act
            Answer result = answerRepository.createAnswer(questionId, previousAnswerId, currentAnswer, ownerId);

            // Assert
            assertNotNull(result);
            assertEquals(previousAnswerId, result.getPreviousAnswerId());
        }
    }

    @Test
    void getAnswers_Success() throws SQLException {
        // Arrange
        int questionId = 1;
        User mockUser1 = createMockUser(1, "user1");
        User mockUser2 = createMockUser(2, "user2");

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> {
                                 when(mock.getUserById(1)).thenReturn(mockUser1);
                                 when(mock.getUserById(2)).thenReturn(mockUser2);
                             })) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false); // Две записи

            // Настройка ResultSet для двух ответов
            when(resultSet.getInt("id")).thenReturn(1, 2);
            when(resultSet.getInt("prev_ans_id")).thenReturn(0, 1);
            when(resultSet.getString("answer")).thenReturn("First answer", "Second answer");
            when(resultSet.getInt("owner")).thenReturn(1, 2);

            // Act
            List<Answer> answers = answerRepository.getAnswers(questionId);

            // Assert
            assertNotNull(answers);
            assertEquals(2, answers.size());

            Answer firstAnswer = answers.get(0);
            assertEquals(1, firstAnswer.getId());
            assertEquals(questionId, firstAnswer.getQuestionId());
            assertEquals(0, firstAnswer.getPreviousAnswerId());
            assertEquals("First answer", firstAnswer.getAnswer());
            assertEquals(mockUser1, firstAnswer.getOwner());

            Answer secondAnswer = answers.get(1);
            assertEquals(2, secondAnswer.getId());
            assertEquals(questionId, secondAnswer.getQuestionId());
            assertEquals(1, secondAnswer.getPreviousAnswerId());
            assertEquals("Second answer", secondAnswer.getAnswer());
            assertEquals(mockUser2, secondAnswer.getOwner());

            verify(preparedStatement).setInt(1, questionId);
        }
    }

    @Test
    void getAnswers_EmptyResult() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // Нет записей

            // Act
            List<Answer> answers = answerRepository.getAnswers(questionId);

            // Assert
            assertNotNull(answers);
            assertTrue(answers.isEmpty());
        }
    }

    @Test
    void getAnswers_WithNullUserFromRepository() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(anyInt())).thenReturn(null))) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getInt("prev_ans_id")).thenReturn(0);
            when(resultSet.getString("answer")).thenReturn("Test answer");
            when(resultSet.getInt("owner")).thenReturn(999); // Несуществующий пользователь

            // Act
            List<Answer> answers = answerRepository.getAnswers(questionId);

            // Assert
            assertEquals(1, answers.size());
            assertNull(answers.get(0).getOwner()); // User должен быть null
        }
    }

    @Test
    void deleteAnswer_Success() throws SQLException {
        // Arrange
        User owner = createMockUser(1, "owner");
        Answer answer = new Answer(1, 1, 0, "Test answer", owner);

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1); // Одна строка удалена

            // Act & Assert
            assertDoesNotThrow(() -> answerRepository.deleteAnswer(answer));

            // Assert
            verify(preparedStatement).setInt(1, answer.getId());
            verify(preparedStatement).executeUpdate();
        }
    }

    @Test
    void deleteAnswer_NoRowsAffected_ThrowsRuntimeException() throws SQLException {
        // Arrange
        User owner = createMockUser(1, "owner");
        Answer answer = new Answer(1, 1, 0, "Test answer", owner);

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0); // Ничего не удалено

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    answerRepository.deleteAnswer(answer)
            );
        }
    }

    @Test
    void createAnswer_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    answerRepository.createAnswer(1, 2, "test", 3)
            );
        }
    }

    @Test
    void getAnswers_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    answerRepository.getAnswers(questionId)
            );
        }
    }

    @Test
    void deleteAnswer_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        User owner = createMockUser(1, "owner");
        Answer answer = new Answer(1, 1, 0, "Test answer", owner);

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    answerRepository.deleteAnswer(answer)
            );
        }
    }

    @Test
    void createAnswer_WithMinimalUserData() throws SQLException {
        // Arrange
        int questionId = 1;
        int previousAnswerId = 0;
        String currentAnswer = "Test answer";
        int ownerId = 5;

        // Пользователь с минимальными данными
        User minimalUser = new User(
                ownerId,
                "minimal",
                "Minimal User",
                "pass",
                null,  // dataOfBirth null
                null,  // city null
                null,  // country null
                null,  // description null
                0,     // avatarId = 0
                null,  // rooms null
                null   // notifications null
        );

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(ownerId)).thenReturn(minimalUser))) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(10);

            // Act
            Answer result = answerRepository.createAnswer(questionId, previousAnswerId, currentAnswer, ownerId);

            // Assert
            assertNotNull(result);
            assertEquals(minimalUser, result.getOwner());
            assertNull(result.getOwner().getDataOfBirth()); // Проверяем, что поля null обрабатываются
            assertEquals(0, result.getOwner().getAvatarId());
        }
    }

    @Test
    void getAnswers_WithDifferentUserDataTypes() throws SQLException {
        // Arrange
        int questionId = 1;

        // Пользователь с разными типами данных
        User complexUser = new User(
                100,
                "complex.login",
                "Complex Username",
                "encrypted_password_123",
                Instant.parse("1985-05-15T10:30:00Z"),
                "Москва", // Кириллица
                "Россия", // Кириллица
                "Очень длинное описание пользователя с различными символами: !@#$%^&*()",
                42,
                Collections.emptyList(),
                Collections.emptyList()
        );

        try (MockedConstruction<UserRepositoryImpl> mockedConstruction =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(100)).thenReturn(complexUser))) {

            answerRepository = new AnswerRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            when(resultSet.getInt("id")).thenReturn(50);
            when(resultSet.getInt("prev_ans_id")).thenReturn(25);
            when(resultSet.getString("answer")).thenReturn("Ответ с кириллицей и символами: тест!");
            when(resultSet.getInt("owner")).thenReturn(100);

            // Act
            List<Answer> answers = answerRepository.getAnswers(questionId);

            // Assert
            assertEquals(1, answers.size());
            Answer answer = answers.get(0);
            assertEquals(complexUser, answer.getOwner());
            assertEquals("Москва", answer.getOwner().getCity());
            assertEquals("Россия", answer.getOwner().getCountry());
            assertEquals(42, answer.getOwner().getAvatarId());
        }
    }
}