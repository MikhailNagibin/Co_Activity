package com.coactivity.controller;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.impl.QAControllerImpl;
import com.coactivity.domain.Category;
import com.coactivity.repository.impl.AnswerRepositoryImpl;
import com.coactivity.service.QAService;
import com.coactivity.service.TokenService;
import com.coactivity.service.dto.TokenPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class QAControllerImplIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
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
  private QAControllerImpl qaController;

  @Autowired
  private DataRepository dataRepository;

  @Autowired
  private AnswerRepositoryImpl answerRepository;

  @Autowired
  private QAService qaService;

  private TokenService tokenService;
  private Integer testUserId1;
  private Integer testUserId2;
  private String validToken1;
  private String validToken2;

  @BeforeEach
  void setUp() throws Exception {
    cleanupDatabase();
    initializeDatabase();

    tokenService = new TokenService() {
      private final Map<Integer, String> activeTokens = new ConcurrentHashMap<>();

      @Override
      public String createToken(Integer userId) {
        Instant expiresAt = Instant.now().plusSeconds(30 * 60);
        String payload = userId + ":" + expiresAt.toEpochMilli();
        String token = java.util.Base64.getEncoder().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        activeTokens.put(userId, token);
        return token;
      }

      @Override
      public TokenPayload decodeToken(String token) {
        try {
          String payload = new String(java.util.Base64.getDecoder().decode(token), java.nio.charset.StandardCharsets.UTF_8);
          String[] parts = payload.split(":", 2);
          if (parts.length != 2) {
            throw new com.coactivity.service.exception.TokenValidationException("Invalid token format");
          }
          Integer userId = Integer.parseInt(parts[0]);
          Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
          return new TokenPayload(userId, expiresAt);
        } catch (Exception e) {
          throw new com.coactivity.service.exception.TokenValidationException("Invalid token", e);
        }
      }

      @Override
      public boolean isTokenActive(String token) {
        try {
          TokenPayload payload = decodeToken(token);
          return token.equals(activeTokens.get(payload.userId())) &&
            payload.expiresAt().isAfter(Instant.now());
        } catch (com.coactivity.service.exception.TokenValidationException e) {
          return false;
        }
      }
    };

    validToken1 = "Bearer " + tokenService.createToken(testUserId1);
    validToken2 = "Bearer " + tokenService.createToken(testUserId2);

    var field = QAControllerImpl.class.getDeclaredField("tokenService");
    field.setAccessible(true);
    field.set(qaController, tokenService);
  }

  private void cleanupDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      statement.execute("SET session_replication_role = 'replica'");

      String[] tables = {
        "usersNotification", "BulletinBoard", "Answers", "Questions",
        "Bans", "Rooms_requests", "Rooms_members", "Pictures",
        "Rooms", "Users",
      };

      for (String table : tables) {
        statement.execute("DELETE FROM " + table);
      }

      statement.execute("SET session_replication_role = 'origin'");
    }
  }

  public void printAllCategories() throws SQLException {
    String sql = "SELECT * FROM Categories ORDER BY id";

    System.out.println("=== Все категории из базы данных ===");
    System.out.println("ID | Название");
    System.out.println("-----------");

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {

      boolean hasData = false;
      while (resultSet.next()) {
        hasData = true;
        int id = resultSet.getInt("id");
        String name = resultSet.getString("name");
        System.out.printf("%-3d | %s%n", id, name);
      }

      if (!hasData) {
        System.out.println("Таблица Categories пустая!");
      }

      System.out.println("================================");

    } catch (SQLException e) {
      System.err.println("Ошибка при получении категорий: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void initializeDatabase() throws Exception {
    DataSource dataSource = dataRepository.getDataSource();

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("init_tables.sql");
      if (inputStream != null) {
        String sql = new String(inputStream.readAllBytes());
        String[] statements = sql.split(";");

        for (String sqlStatement : statements) {
          if (!sqlStatement.trim().isEmpty()) {
            statement.execute(sqlStatement.trim());
          }
        }
      } else {
        System.out.println("Не найден источник данных !!!");
      }

      insertTestData(connection);
    }
  }

  private void insertTestData(Connection connection) throws Exception {
    try (PreparedStatement ps = connection.prepareStatement(
      "INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {

      ps.setString(1, "user1@test.com");
      ps.setString(2, "TestUser1");
      ps.setString(3, "hashed_password_1");
      ps.setTimestamp(4, java.sql.Timestamp.from(Instant.now().minusSeconds(86400 * 365 * 25)));
      ps.setString(5, "USA");
      ps.setString(6, "New York");
      ps.setString(7, "Test user 1");
      ps.setInt(8, 1);

      var rs = ps.executeQuery();
      if (rs.next()) {
        testUserId1 = rs.getInt(1);
      }
    }

    try (PreparedStatement ps = connection.prepareStatement(
      "INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {

      ps.setString(1, "user2@test.com");
      ps.setString(2, "TestUser2");
      ps.setString(3, "hashed_password_2");
      ps.setTimestamp(4, java.sql.Timestamp.from(Instant.now().minusSeconds(86400 * 365 * 30)));
      ps.setString(5, "UK");
      ps.setString(6, "London");
      ps.setString(7, "Test user 2");
      ps.setInt(8, 2);

      var rs = ps.executeQuery();
      if (rs.next()) {
        testUserId2 = rs.getInt(1);
      }
    }

    try (Statement stmt = connection.createStatement()) {
      stmt.execute("SET session_replication_role = 'replica'");

      stmt.executeUpdate(
        "INSERT INTO Users (id, login, username, password, birthday, country, city, description, avatar_id) " +
          "VALUES (0, 'empty@user.com', 'EmptyUser', 'empty_hash', '2000-01-01', 'N/A', 'N/A', 'Пустой пользователь', 0) " +
          "ON CONFLICT (id) DO NOTHING"
      );

      stmt.execute("SET session_replication_role = 'origin'");

      try (ResultSet rs = stmt.executeQuery(
        "SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM Users), 0) + 1, false)")) {
      }
    }

    try (Statement stmt = connection.createStatement()) {
      stmt.execute("SET session_replication_role = 'replica'");

      stmt.executeUpdate(
        "INSERT INTO Questions (id, owner, question, category_id) " +
          "VALUES (0, 0, 'Пустой вопрос для тестов', 12) " +
          "ON CONFLICT (id) DO NOTHING"
      );

      stmt.execute("SET session_replication_role = 'origin'");

      try (ResultSet rs = stmt.executeQuery(
        "SELECT setval('questions_id_seq', COALESCE((SELECT MAX(id) FROM Questions), 0) + 1, false)")) {
      }
    }

    try (Statement stmt = connection.createStatement()) {
      stmt.execute("SET session_replication_role = 'replica'");

      stmt.executeUpdate(
        "INSERT INTO Answers (id, question_id, prev_ans_id, answer, owner, created_at) " +
          "VALUES (0, 0, NULL, 'Пустой ответ для тестов', 0, CURRENT_TIMESTAMP) " +
          "ON CONFLICT (id) DO NOTHING"
      );

      stmt.execute("SET session_replication_role = 'origin'");

      try (ResultSet rs = stmt.executeQuery(
        "SELECT setval('answers_id_seq', COALESCE((SELECT MAX(id) FROM Answers), 0) + 1, false)")) {
      }
    }
  }

  private void createTestQuestions() {
    QuestionRequest sportQuestion = new QuestionRequest();
    sportQuestion.setCategory("Business");
    sportQuestion.setQuestion("What are good sports for kids?");
    qaController.askQuestion(validToken1, sportQuestion);

    QuestionRequest musicQuestion = new QuestionRequest();
    musicQuestion.setCategory("Music");
    musicQuestion.setQuestion("How to learn guitar quickly?");
    qaController.askQuestion(validToken2, musicQuestion);

    QuestionRequest artQuestion = new QuestionRequest();
    artQuestion.setCategory("Art");
    artQuestion.setQuestion("Best drawing techniques for beginners?");
    qaController.askQuestion(validToken1, artQuestion);
  }

  @Nested
  @DisplayName("US-701: As a community member, I want to ask questions")
  class AskQuestionsTest {

    @Test
    @DisplayName("Should create question with valid request")
    void testAskQuestion_ValidRequest_ShouldCreateQuestion() {
      QuestionRequest request = new QuestionRequest();
      request.setCategory("Sport");
      request.setQuestion("What are the best exercises for beginners?");

      ResponseEntity<QuestionResponse> response = qaController.askQuestion(validToken1, request);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());

      QuestionResponse questionResponse = response.getBody();
      assertEquals("What are the best exercises for beginners?", questionResponse.getQuestion());
      assertEquals(Category.SPORT, questionResponse.getCategory());
      assertNotNull(questionResponse.getAuthor());
      assertEquals(testUserId1, questionResponse.getAuthor().getId());
      assertEquals("TestUser1", questionResponse.getAuthor().getUserName());
    }

    @Test
    @DisplayName("Should throw exception with empty question")
    void testAskQuestion_InvalidToken_ShouldThrowException() {
      QuestionRequest request = new QuestionRequest();
      request.setCategory("Sport");
      request.setQuestion("");

      assertThrows(com.coactivity.service.exception.ValidationException.class,
        () -> qaController.askQuestion(validToken1, request));
    }
  }

  @Nested
  @DisplayName("US-702: As a community member, I want to answer questions")
  class AnswerQuestionsTest {

    @Test
    @DisplayName("Should create answer to existing question")
    void testAnswerQuestion_ValidRequest_ShouldCreateAnswer() {
      QuestionRequest questionRequest = new QuestionRequest();
      questionRequest.setCategory("Music");
      questionRequest.setQuestion("What is the best guitar for beginners?");
      ResponseEntity<QuestionResponse> questionResponse = qaController.askQuestion(validToken1, questionRequest);
      Integer questionId = questionResponse.getBody().getId();

      AnswerRequest answerRequest = new AnswerRequest();
      answerRequest.setQuestionId(questionId);
      answerRequest.setAnswer("I recommend starting with a Yamaha F310 acoustic guitar.");

      ResponseEntity<AnswerResponse> response = qaController.answerQuestion(validToken2, answerRequest);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());

      AnswerResponse answerResponse = response.getBody();
      assertEquals(questionId, answerResponse.getQuestionId());
      assertEquals("I recommend starting with a Yamaha F310 acoustic guitar.", answerResponse.getAnswer());
      assertNotNull(answerResponse.getAuthor());
      assertEquals(testUserId2, answerResponse.getAuthor().getId());
      assertEquals("TestUser2", answerResponse.getAuthor().getUserName());
      assertNotNull(answerResponse.getCreatedAt());
    }

    @Test
    @DisplayName("Should create nested reply to existing answer")
    void testAnswerQuestion_WithThreadedReply_ShouldCreateNestedAnswer() {
      QuestionRequest questionRequest = new QuestionRequest();
      questionRequest.setCategory("Art");
      questionRequest.setQuestion("What are good watercolor brands for beginners?");
      ResponseEntity<QuestionResponse> questionResponse = qaController.askQuestion(validToken1, questionRequest);
      Integer questionId = questionResponse.getBody().getId();

      AnswerRequest firstAnswer = new AnswerRequest();
      firstAnswer.setQuestionId(questionId);
      firstAnswer.setAnswer("Winsor & Newton Cotman sets are excellent for beginners.");
      ResponseEntity<AnswerResponse> firstAnswerResponse = qaController.answerQuestion(validToken2, firstAnswer);
      Integer firstAnswerId = firstAnswerResponse.getBody().getId();

      AnswerRequest replyAnswer = new AnswerRequest();
      replyAnswer.setQuestionId(questionId);
      replyAnswer.setPreviousAnswerId(firstAnswerId);
      replyAnswer.setAnswer("I agree! Also check out Van Gogh student grade watercolors.");

      ResponseEntity<AnswerResponse> response = qaController.answerQuestion(validToken1, replyAnswer);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());

      AnswerResponse replyResponse = response.getBody();
      assertEquals(questionId, replyResponse.getQuestionId());
      assertEquals(firstAnswerId, replyResponse.getPreviousAnswerId());
      assertEquals("I agree! Also check out Van Gogh student grade watercolors.", replyResponse.getAnswer());
      assertEquals(testUserId1, replyResponse.getAuthor().getId());
    }
  }

  @Nested
  @DisplayName("US-703: As a visitor, I want to browse all questions")
  class BrowseQuestionsTest {

    @Test
    @DisplayName("Should return all questions without filter")
    void testGetQuestions_NoFilter_ShouldReturnAllQuestions() {
      createTestQuestions();

      ResponseEntity<List<QuestionResponse>> response = qaController.getAllQuestions();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<QuestionResponse> questions = response.getBody();
      assertEquals(3, questions.size());
      System.out.println(questions.get(1));
      assertEquals("BUSINESS", questions.get(0).getCategory().toString());
      assertEquals("What are good sports for kids?", questions.get(0).getQuestion());

      assertEquals("MUSIC", questions.get(1).getCategory().toString());
      assertEquals("How to learn guitar quickly?", questions.get(1).getQuestion());

      assertEquals("ART", questions.get(2).getCategory().toString());
      assertEquals("Best drawing techniques for beginners?", questions.get(2).getQuestion());
    }

    @Test
    @DisplayName("Should return filtered questions by category")
    void testGetQuestions_WithCategoryFilter_ShouldReturnFilteredQuestions() throws Exception {
      createTestQuestions();
      printAllCategories();

      ResponseEntity<List<QuestionResponse>> response = qaController.getQuestions(16);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      List<QuestionResponse> questions = response.getBody();
      assertEquals(1, questions.size());
      assertEquals("What are good sports for kids?", questions.get(0).getQuestion());
      assertEquals(Category.BUSINESS, questions.get(0).getCategory());
    }

    @Test
    @DisplayName("Should return empty list when database is empty")
    void testGetQuestions_EmptyDatabase_ShouldReturnEmptyList() {
      ResponseEntity<List<QuestionResponse>> response = qaController.getQuestions(null);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertTrue(response.getBody().isEmpty());
    }
  }

  @Nested
  @DisplayName("US-704: As a visitor, I want to view complete question threads")
  class ViewQuestionThreadsTest {

    @Test
    @DisplayName("Should return complete thread with answers and replies")
    void testGetQuestionWithAnswers_ValidQuestionId_ShouldReturnCompleteThread() {
      QuestionRequest questionRequest = new QuestionRequest();
      questionRequest.setCategory("Entertainments");
      questionRequest.setQuestion("What are the best board games for 4 players?");
      ResponseEntity<QuestionResponse> questionResponse = qaController.askQuestion(validToken1, questionRequest);
      Integer questionIdA = questionResponse.getBody().getId();

      AnswerRequest answer1 = new AnswerRequest();
      answer1.setQuestionId(questionIdA);
      answer1.setAnswer("Catan is excellent for 4 players!");
      ResponseEntity<AnswerResponse> answer1Response = qaController.answerQuestion(validToken2, answer1);
      Integer answer1Id = answer1Response.getBody().getId();


      AnswerRequest reply1 = new AnswerRequest();
      reply1.setQuestionId(questionIdA);
      reply1.setPreviousAnswerId(answer1Id);
      reply1.setAnswer("I prefer Ticket to Ride for 4 players.");
      qaController.answerQuestion(validToken1, reply1);

      AnswerRequest answer2 = new AnswerRequest();
      answer2.setQuestionId(questionIdA);
      answer2.setAnswer("Pandemic is a great cooperative game for 4.");
      qaController.answerQuestion(validToken2, answer2);

      ResponseEntity<QuestionWithAnswersResponse> response =
        qaController.getQuestionWithAnswers(questionIdA);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      QuestionWithAnswersResponse thread = response.getBody();
      assertNotNull(thread.getQuestion());
      assertEquals(questionIdA, thread.getQuestion().getId());
      assertEquals("What are the best board games for 4 players?", thread.getQuestion().getQuestion());

      assertEquals(3, thread.getAnswers().size());

      AnswerResponse firstAnswer = thread.getAnswers().get(0);
      assertEquals("Catan is excellent for 4 players!", firstAnswer.getAnswer());

      AnswerResponse secondAnswer = thread.getAnswers().get(1);
      assertEquals("I prefer Ticket to Ride for 4 players.", secondAnswer.getAnswer());

      AnswerResponse thirdAnswer = thread.getAnswers().get(2);
      assertEquals("Pandemic is a great cooperative game for 4.", thirdAnswer.getAnswer());
    }

    @Test
    @DisplayName("Should return question with empty answers list when no answers")
    void testGetQuestionWithAnswers_QuestionWithNoAnswers_ShouldReturnEmptyAnswersList() {
      QuestionRequest questionRequest = new QuestionRequest();
      questionRequest.setCategory("Business");
      questionRequest.setQuestion("How to start a small business?");
      ResponseEntity<QuestionResponse> questionResponse = qaController.askQuestion(validToken1, questionRequest);
      Integer questionId = questionResponse.getBody().getId();

      ResponseEntity<QuestionWithAnswersResponse> response =
        qaController.getQuestionWithAnswers(questionId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());

      QuestionWithAnswersResponse thread = response.getBody();
      assertEquals(questionId, thread.getQuestion().getId());
      assertTrue(thread.getAnswers().isEmpty());
    }
  }
}