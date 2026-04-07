package com.coactivity.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("UserRepository delete user integration tests")
class UserRepositoryDeleteIntegrationTest {

  @Autowired
  private UserRepositoryImpl userRepository;

  @Autowired
  private DataSource dataSource;

  @Value("${app.storage.local.root}")
  private String storageRoot;

  private Integer categoryId;

  @BeforeEach
  void setUp() throws SQLException {
    cleanupTables();
    cleanupStorage();
    categoryId = loadCategoryId("Sport");
  }

  @Test
  void deleteUserRemovesForeignAnswersToUsersQuestions() throws SQLException {
    Integer ownerId = createUser("question-owner@example.com", "questionOwner");
    Integer otherUserId = createUser("responder@example.com", "responder");

    Integer questionId = insertQuestion(ownerId, "Owner question");
    insertAnswer(questionId, null, otherUserId, "Foreign answer");

    userRepository.deleteUser(ownerId);

    assertFalse(userExists(ownerId));
    assertEquals(0, countById("questions", questionId));
    assertEquals(0, countAnswersByQuestionId(questionId));
    assertTrue(userExists(otherUserId));
  }

  @Test
  void deleteUserKeepsForeignReplyButDetachesPrevAnswer() throws SQLException {
    Integer questionOwnerId = createUser("another-owner@example.com", "anotherOwner");
    Integer deletedUserId = createUser("target@example.com", "targetUser");
    Integer replyAuthorId = createUser("reply@example.com", "replyAuthor");

    Integer questionId = insertQuestion(questionOwnerId, "Question by another user");
    Integer deletedUsersAnswerId = insertAnswer(questionId, null, deletedUserId, "Target answer");
    Integer replyId = insertAnswer(questionId, deletedUsersAnswerId, replyAuthorId, "Reply answer");

    userRepository.deleteUser(deletedUserId);

    assertFalse(userExists(deletedUserId));
    assertEquals(0, countById("answers", deletedUsersAnswerId));
    assertEquals(1, countById("answers", replyId));
    assertEquals(replyAuthorId, getAnswerOwner(replyId));
    assertNull(getPrevAnswerId(replyId));
  }

  private Integer createUser(String login, String username) {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail(login);
    request.setUserName(username);
    request.setPassword("password123");
    request.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC).minusYears(20).toInstant());
    return userRepository.createUser(request).getId();
  }

  private void cleanupTables() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             TRUNCATE TABLE
               bulletin_board,
               user_notifications,
               answers,
               questions,
               bans,
               room_requests,
               room_members,
               pictures,
               user_avatars,
               rooms,
               users
             RESTART IDENTITY CASCADE
             """)) {
      statement.executeUpdate();
    }
  }

  private Integer loadCategoryId(String categoryName) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement =
             connection.prepareStatement("SELECT id FROM categories WHERE name = ?")) {
      statement.setString(1, categoryName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private Integer insertQuestion(Integer ownerId, String questionText) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO questions (owner, question, category_id)
             VALUES (?, ?, ?)
             RETURNING id
             """)) {
      statement.setInt(1, ownerId);
      statement.setString(2, questionText);
      statement.setInt(3, categoryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private Integer insertAnswer(Integer questionId, Integer previousAnswerId, Integer ownerId,
      String answerText) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO answers (question_id, prev_ans_id, answer, owner)
             VALUES (?, ?, ?, ?)
             RETURNING id
             """)) {
      statement.setInt(1, questionId);
      if (previousAnswerId != null) {
        statement.setInt(2, previousAnswerId);
      } else {
        statement.setNull(2, java.sql.Types.INTEGER);
      }
      statement.setString(3, answerText);
      statement.setInt(4, ownerId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private boolean userExists(Integer userId) throws SQLException {
    return countById("users", userId) == 1;
  }

  private int countById(String tableName, Integer id) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
             "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?")) {
      statement.setInt(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countAnswersByQuestionId(Integer questionId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement =
             connection.prepareStatement("SELECT COUNT(*) FROM answers WHERE question_id = ?")) {
      statement.setInt(1, questionId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private Integer getPrevAnswerId(Integer answerId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement =
             connection.prepareStatement("SELECT prev_ans_id FROM answers WHERE id = ?")) {
      statement.setInt(1, answerId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        int value = resultSet.getInt(1);
        return resultSet.wasNull() ? null : value;
      }
    }
  }

  private Integer getAnswerOwner(Integer answerId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement =
             connection.prepareStatement("SELECT owner FROM answers WHERE id = ?")) {
      statement.setInt(1, answerId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private void cleanupStorage() {
    try {
      Path rootPath = Path.of(storageRoot).toAbsolutePath().normalize();
      if (!Files.exists(rootPath)) {
        return;
      }
      Files.walk(rootPath)
          .sorted((left, right) -> right.getNameCount() - left.getNameCount())
          .forEach(path -> {
            if (path.equals(rootPath)) {
              return;
            }
            try {
              Files.deleteIfExists(path);
            } catch (IOException ex) {
              throw new IllegalStateException("Failed to cleanup test storage", ex);
            }
          });
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to cleanup test storage", ex);
    }
  }
}
