package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Question;
import com.coactivity.domain.User;
import com.coactivity.domain.Category;
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
class QuestionRepositoryImplTest {

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

    private QuestionRepositoryImpl questionRepository;

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

    @Test
    void createQuestion_Success() throws SQLException {
        // Arrange
        int userId = 1;
        String questionText = "Test question";
        int categoryId = 2; // Category.Art
        int generatedQuestionId = 10;
        User mockUser = createMockUser(userId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(userId)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedQuestionId);

            // Act
            Question result = questionRepository.createQuestion(userId, questionText, categoryId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedQuestionId, result.getId());
            assertEquals(mockUser, result.getOwner()); // Исправлено на getOwner()
            assertEquals(questionText, result.getQuestion());
            assertEquals(Category.Art, result.getCategory());

            verify(preparedStatement).setInt(1, userId);
            verify(preparedStatement).setString(2, questionText);
            verify(preparedStatement).setInt(3, categoryId);
        }
    }

    @Test
    void createQuestion_WithInvalidCategoryId_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int userId = 1;
        String questionText = "Test question";
        int invalidCategoryId = 999; // Не существует в Category enum
        User mockUser = createMockUser(userId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(userId)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(10);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.createQuestion(userId, questionText, invalidCategoryId)
            );
        }
    }

    @Test
    void createQuestion_NoResultSet_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.createQuestion(1, "test", 1)
            );
        }
    }

    @Test
    void createQuestion_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.createQuestion(1, "test", 1)
            );
        }
    }

    @Test
    void getAllQuestions_Success() throws SQLException {
        // Arrange
        User mockUser1 = createMockUser(1);
        User mockUser2 = createMockUser(2);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> {
                                 when(mock.getUserById(1)).thenReturn(mockUser1);
                                 when(mock.getUserById(2)).thenReturn(mockUser2);
                             })) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            when(resultSet.getInt("id")).thenReturn(1, 2);
            when(resultSet.getInt("owner")).thenReturn(1, 2);
            when(resultSet.getString("question")).thenReturn("Question 1", "Question 2");
            when(resultSet.getInt("category_id")).thenReturn(0, 1); // Sport, Music

            // Act
            List<Question> questions = questionRepository.getAllQuestions();

            // Assert
            assertNotNull(questions);
            assertEquals(2, questions.size());

            Question firstQuestion = questions.get(0);
            assertEquals(1, firstQuestion.getId());
            assertEquals(mockUser1, firstQuestion.getOwner()); // Исправлено на getOwner()
            assertEquals("Question 1", firstQuestion.getQuestion());
            assertEquals(Category.Sport, firstQuestion.getCategory());

            Question secondQuestion = questions.get(1);
            assertEquals(2, secondQuestion.getId());
            assertEquals(mockUser2, secondQuestion.getOwner()); // Исправлено на getOwner()
            assertEquals("Question 2", secondQuestion.getQuestion());
            assertEquals(Category.Music, secondQuestion.getCategory());
        }
    }

    @Test
    void getAllQuestions_EmptyResult() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            List<Question> questions = questionRepository.getAllQuestions();

            // Assert
            assertNotNull(questions);
            assertTrue(questions.isEmpty());
        }
    }

    @Test
    void getAllQuestions_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.getAllQuestions()
            );
        }
    }

    @Test
    void getQuestionById_Success() throws SQLException {
        // Arrange
        int questionId = 1;
        User mockUser = createMockUser(1);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(1)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getInt("id")).thenReturn(questionId);
            when(resultSet.getInt("owner")).thenReturn(1);
            when(resultSet.getString("question")).thenReturn("Test question");
            when(resultSet.getInt("category_id")).thenReturn(3); // Entertainments

            // Act
            Question result = questionRepository.getQuestionById(questionId);

            // Assert
            assertNotNull(result);
            assertEquals(questionId, result.getId());
            assertEquals(mockUser, result.getOwner()); // Исправлено на getOwner()
            assertEquals("Test question", result.getQuestion());
            assertEquals(Category.Entertainments, result.getCategory());

            verify(preparedStatement).setInt(1, questionId);
        }
    }

    @Test
    void getQuestionById_NotFound_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 999;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.getQuestionById(questionId)
            );
        }
    }

    @Test
    void getQuestionById_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.getQuestionById(questionId)
            );
        }
    }

    @Test
    void updateQuestion_Success() throws SQLException {
        // Arrange
        int questionId = 1;
        String updatedQuestion = "Updated question";
        int newCategoryId = 4; // Business
        User mockUser = createMockUser(1);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(1)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getInt("id")).thenReturn(questionId);
            when(resultSet.getInt("owner")).thenReturn(1);
            when(resultSet.getString("question")).thenReturn(updatedQuestion);
            when(resultSet.getInt("category_id")).thenReturn(newCategoryId);

            // Act
            Question result = questionRepository.updateQuestion(questionId, updatedQuestion, newCategoryId);

            // Assert
            assertNotNull(result);
            assertEquals(questionId, result.getId());
            assertEquals(mockUser, result.getOwner()); // Исправлено на getOwner()
            assertEquals(updatedQuestion, result.getQuestion());
            assertEquals(Category.Business, result.getCategory());

            verify(preparedStatement).setString(1, updatedQuestion);
            verify(preparedStatement).setInt(2, newCategoryId);
            verify(preparedStatement).setInt(3, questionId);
        }
    }

    @Test
    void updateQuestion_NoResultSet_ThrowsRuntimeException() throws SQLException {
        // Arrange
        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.updateQuestion(1, "updated", 1)
            );
        }
    }

    @Test
    void deleteQuestion_Success() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            // Mock для deleteAllWithQuestion
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1); // Для обоих DELETE

            // Act & Assert
            assertDoesNotThrow(() -> questionRepository.deleteQuestion(questionId));

            // Assert - проверяем что оба DELETE вызваны
            verify(preparedStatement, times(2)).executeUpdate();
            verify(preparedStatement, atLeastOnce()).setInt(1, questionId);
        }
    }

    @Test
    void deleteQuestion_NoAnswers_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            // Первый DELETE (ответы) возвращает 0 affected rows
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0); // Нет ответов

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.deleteQuestion(questionId)
            );
        }
    }

    @Test
    void deleteQuestion_QuestionNotFound_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 999;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            // Первый DELETE успешен, второй - нет
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1, 0); // Ответы удалены, вопрос не найден

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.deleteQuestion(questionId)
            );
        }
    }

    @Test
    void deleteQuestion_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.deleteQuestion(questionId)
            );
        }
    }

    @Test
    void deleteAllWithQuestion_Success() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1); // Ответы удалены

            // Используем reflection для тестирования приватного метода
            java.lang.reflect.Method method = QuestionRepositoryImpl.class.getDeclaredMethod("deleteAllWithQuestion", int.class);
            method.setAccessible(true);

            // Act & Assert
            assertDoesNotThrow(() -> method.invoke(questionRepository, questionId));

            verify(preparedStatement).setInt(1, questionId);
            verify(preparedStatement).executeUpdate();
        } catch (Exception e) {
            fail("Reflection call failed: " + e.getMessage());
        }
    }

    @Test
    void deleteAllWithQuestion_NoAnswers_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;

        try (MockedConstruction<UserRepositoryImpl> ignored =
                     mockConstruction(UserRepositoryImpl.class)) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0); // Нет ответов

            // Используем reflection для тестирования приватного метода
            java.lang.reflect.Method method = QuestionRepositoryImpl.class.getDeclaredMethod("deleteAllWithQuestion", int.class);
            method.setAccessible(true);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    method.invoke(questionRepository, questionId)
            );
        } catch (Exception e) {
            fail("Reflection call failed: " + e.getMessage());
        }
    }

    @Test
    void mapResultSetToQuestion_Success() throws SQLException {
        // Arrange
        int questionId = 1;
        int ownerId = 2;
        String questionText = "Test question";
        int categoryId = 5; // Education
        User mockUser = createMockUser(ownerId);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(ownerId)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(resultSet.getInt("id")).thenReturn(questionId);
            when(resultSet.getInt("owner")).thenReturn(ownerId);
            when(resultSet.getString("question")).thenReturn(questionText);
            when(resultSet.getInt("category_id")).thenReturn(categoryId);

            // Act (через вызов getQuestionById)
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            Question result = questionRepository.getQuestionById(questionId);

            // Assert
            assertNotNull(result);
            assertEquals(questionId, result.getId());
            assertEquals(mockUser, result.getOwner()); // Исправлено на getOwner()
            assertEquals(questionText, result.getQuestion());
            assertEquals(Category.Education, result.getCategory());
        }
    }

    @Test
    void createQuestion_WithAllCategoryValues() throws SQLException {
        // Test all valid category values
        for (int i = 0; i < Category.values().length; i++) {
            int categoryId = i;
            User mockUser = createMockUser(1);

            try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                         mockConstruction(UserRepositoryImpl.class,
                                 (mock, context) -> when(mock.getUserById(1)).thenReturn(mockUser))) {

                questionRepository = new QuestionRepositoryImpl(dataRepository);

                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
                when(preparedStatement.executeQuery()).thenReturn(resultSet);
                when(resultSet.next()).thenReturn(true);
                when(resultSet.getInt("id")).thenReturn(10);

                // Act
                Question result = questionRepository.createQuestion(1, "Test question", categoryId);

                // Assert
                assertNotNull(result);
                assertEquals(Category.getByIndex(categoryId), result.getCategory());
            }
        }
    }

    @Test
    void createQuestion_WithNullUserFromRepository() throws SQLException {
        // Arrange
        int userId = 999; // Несуществующий пользователь
        String questionText = "Test question";
        int categoryId = 1;
        int generatedQuestionId = 10;

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(userId)).thenReturn(null))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("id")).thenReturn(generatedQuestionId);

            // Act
            Question result = questionRepository.createQuestion(userId, questionText, categoryId);

            // Assert
            assertNotNull(result);
            assertNull(result.getOwner()); // Owner должен быть null
            assertEquals(questionText, result.getQuestion());
        }
    }

    @Test
    void updateQuestion_WithInvalidCategory_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int questionId = 1;
        String updatedQuestion = "Updated question";
        int invalidCategoryId = 999; // Не существует в Category enum
        User mockUser = createMockUser(1);

        try (MockedConstruction<UserRepositoryImpl> userRepoMock =
                     mockConstruction(UserRepositoryImpl.class,
                             (mock, context) -> when(mock.getUserById(1)).thenReturn(mockUser))) {

            questionRepository = new QuestionRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getInt("id")).thenReturn(questionId);
            when(resultSet.getInt("owner")).thenReturn(1);
            when(resultSet.getString("question")).thenReturn(updatedQuestion);
            when(resultSet.getInt("category_id")).thenReturn(invalidCategoryId);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    questionRepository.updateQuestion(questionId, updatedQuestion, invalidCategoryId)
            );
        }
    }

    @Test
    void questionConstructor_ValidParameters() {
        // Arrange
        int id = 1;
        User owner = createMockUser(1);
        String questionText = "Test question";
        Category category = Category.Sport;

        // Act
        Question question = new Question(id, owner, questionText, category);

        // Assert
        assertNotNull(question);
        assertEquals(id, question.getId());
        assertEquals(owner, question.getOwner());
        assertEquals(questionText, question.getQuestion());
        assertEquals(category, question.getCategory());
    }

    @Test
    void questionConstructor_NullOwner() {
        // Arrange
        int id = 1;
        String questionText = "Test question";
        Category category = Category.Sport;

        // Act
        Question question = new Question(id, null, questionText, category);

        // Assert
        assertNotNull(question);
        assertNull(question.getOwner());
        assertEquals(questionText, question.getQuestion());
        assertEquals(category, question.getCategory());
    }

    @Test
    void questionGettersAndSetters() {
        // Arrange
        Question question = new Question(1, createMockUser(1), "Original question", Category.Sport);

        // Act
        User newOwner = createMockUser(2);
        String newQuestionText = "Updated question";
        Category newCategory = Category.Music;

        question.setId(2);
        question.setOwner(newOwner);
        question.setQuestion(newQuestionText);
        question.setCategory(newCategory);

        // Assert
        assertEquals(2, question.getId());
        assertEquals(newOwner, question.getOwner());
        assertEquals(newQuestionText, question.getQuestion());
        assertEquals(newCategory, question.getCategory());
    }
}