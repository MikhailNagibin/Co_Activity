package com.coactivity.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coactivity.CoActivityApplication;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = CoActivityApplication.class, properties = "spring.jpa.hibernate.ddl-auto=none")
@Tag("docker")
@DisplayName("UserRepository delete user integration tests")
class UserRepositoryDeleteIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
      .withDatabaseName("delete_user_test_db")
      .withUsername("postgres")
      .withPassword("postgres");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserRepositoryImpl userRepository;

  @Autowired
  private DataSource dataSource;

  private Integer categoryId;

  @BeforeEach
  void setUp() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/init_tables.sql"));
      cleanupTables(connection);
      seedCategory(connection);
    }
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
    request.setLogin(login);
    request.setUserName(username);
    request.setPassword("password123");
    request.setDateOfBirth(Instant.now().minus(20, ChronoUnit.YEARS));
    return userRepository.createUser(request).getId();
  }

  private void cleanupTables(Connection connection) throws SQLException {
    try (PreparedStatement disableTriggers =
             connection.prepareStatement("SET session_replication_role = 'replica'");
         PreparedStatement enableTriggers =
             connection.prepareStatement("SET session_replication_role = 'origin'")) {
      disableTriggers.executeUpdate();
      for (String table : new String[]{
          "user_notifications", "bulletin_board", "answers", "questions", "bans", "room_requests",
          "room_members", "pictures", "rooms", "notifications", "request_statuses", "roles",
          "categories", "users"}) {
        try (PreparedStatement deleteStatement =
                 connection.prepareStatement("DELETE FROM " + table)) {
          deleteStatement.executeUpdate();
        }
      }
      enableTriggers.executeUpdate();
    }
  }

  private void seedCategory(Connection connection) throws SQLException {
    try (PreparedStatement insert =
             connection.prepareStatement(
                 "INSERT INTO categories (name) VALUES ('Sport') ON CONFLICT (name) DO NOTHING");
         PreparedStatement select =
             connection.prepareStatement("SELECT id FROM categories WHERE name = 'Sport'")) {
      insert.executeUpdate();
      try (ResultSet resultSet = select.executeQuery()) {
        resultSet.next();
        categoryId = resultSet.getInt(1);
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
}
