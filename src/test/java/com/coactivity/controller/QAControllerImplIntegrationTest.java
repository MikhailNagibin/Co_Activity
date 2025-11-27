package com.coactivity.controller;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.impl.QAControllerImpl;
import com.coactivity.DataRepository;
import com.coactivity.repository.impl.*;
import com.coactivity.service.QAService;
import com.coactivity.service.TokenService;
import com.coactivity.service.dto.TokenPayload;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class QAControllerImplIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  static DataRepository dataRepository;
  static UserRepositoryImpl userRepository;
  static QuestionRepositoryImpl questionRepository;
  static AnswerRepositoryImpl answerRepository;
  static QAService qaService;
  static TokenService tokenService;
  static QAControllerImpl controller;

  @BeforeAll
  static void setup() throws Exception {
    postgres.start();
    runInitSql();
    seedLookupsAndUsers();

    dataRepository = buildDataRepositoryFromContainer();
    userRepository = new UserRepositoryImpl(dataRepository, new RoomRepositoryImpl(dataRepository, null));
    // fix circular
    RoomRepositoryImpl roomRepository = new RoomRepositoryImpl(dataRepository, userRepository);
    userRepository = new UserRepositoryImpl(dataRepository, roomRepository);

    questionRepository = new QuestionRepositoryImpl(dataRepository, userRepository);
    answerRepository = new AnswerRepositoryImpl(dataRepository, userRepository);

    qaService = new QAService(questionRepository, answerRepository, userRepository);

    tokenService = new TokenService() {
      @Override public boolean isTokenActive(String token) { return token != null; }
      @Override public TokenPayload decodeToken(String token) { return new TokenPayload(Integer.parseInt(token), Instant.now()); }
    };

    controller = new QAControllerImpl(qaService, tokenService);
  }

  @AfterAll
  static void teardown() { postgres.stop(); }

  private static DataRepository buildDataRepositoryFromContainer() {
    return new DataRepository() {
      final DataSource ds = new org.postgresql.ds.PGSimpleDataSource() {{
        setServerName(postgres.getHost());
        setPortNumber(postgres.getFirstMappedPort());
        setDatabaseName(postgres.getDatabaseName());
        setUser(postgres.getUsername());
        setPassword(postgres.getPassword());
      }};
      @Override public DataSource getDataSource() { return ds; }
    };
  }

  private static void runInitSql() throws Exception {
    var res = new ClassPathResource("sql/init_tables.sql");
    String sql = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    try (Connection c = buildDataRepositoryFromContainer().getDataSource().getConnection(); Statement st = c.createStatement()) {
      for (String s : sql.split(";")) { s = s.trim(); if (!s.isEmpty()) st.execute(s); }
    }
  }

  private static void seedLookupsAndUsers() throws Exception {
    try (Connection c = buildDataRepositoryFromContainer().getDataSource().getConnection(); Statement st = c.createStatement()) {
      st.execute("INSERT INTO Roles(role) VALUES ('OWNER'),('ADMIN'),('PARTICIPANT') ON CONFLICT DO NOTHING");
      st.execute("INSERT INTO Categories(name) VALUES ('Sport') ON CONFLICT DO NOTHING");
      st.execute("INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) VALUES (" +
          "'u1','user1','p', now(), 'ctry','city','desc',1)," +
          "('u2','user2','p', now(), 'ctry','city','desc',2)");
    }
  }

  @Test
  @DisplayName("askQuestion: authorized, valid request")
  void askQuestion_success() {
    QuestionRequest req = new QuestionRequest();
    req.setQuestion("What is integration testing?");
    req.setCategoryId(1);
    ApiResponse<QuestionResponse> resp = controller.askQuestion("1", req);
    assertTrue(resp.isSuccess());
    assertNotNull(resp.getData().getId());
  }

  @Test
  @DisplayName("askQuestion: unauthorized")
  void askQuestion_unauthorized() {
    ApiResponse<QuestionResponse> resp = controller.askQuestion(null, new QuestionRequest());
    assertFalse(resp.isSuccess());
    assertEquals("401", resp.getMessage());
  }

  @Test
  @DisplayName("answerQuestion: authorized, valid")
  void answerQuestion_success() {
    QuestionRequest q = new QuestionRequest(); q.setQuestion("Q1"); q.setCategoryId(1);
    Integer qid = controller.askQuestion("1", q).getData().getId();

    AnswerRequest ar = new AnswerRequest(); ar.setQuestionId(qid); ar.setAnswer("A1");
    ApiResponse<AnswerResponse> resp = controller.answerQuestion("2", ar);
    assertTrue(resp.isSuccess());
    assertNotNull(resp.getData().getId());
  }

  @Test
  @DisplayName("answerQuestion: unauthorized")
  void answerQuestion_unauthorized() {
    ApiResponse<AnswerResponse> resp = controller.answerQuestion(null, new AnswerRequest());
    assertFalse(resp.isSuccess());
    assertEquals("401", resp.getMessage());
  }

  @Test
  @DisplayName("getQuestions: public endpoint returns list")
  void getQuestions_success() {
    QuestionRequest q = new QuestionRequest(); q.setQuestion("Q2"); q.setCategoryId(1);
    controller.askQuestion("1", q);

    ApiResponse<List<QuestionResponse>> list = controller.getQuestions(1);
    assertTrue(list.isSuccess());
    assertFalse(list.getData().isEmpty());
  }

  @Test
  @DisplayName("getQuestionWithAnswers: public endpoint returns data and 404 for missing")
  void getQuestionWithAnswers_cases() {
    QuestionRequest q = new QuestionRequest(); q.setQuestion("Q3"); q.setCategoryId(1);
    Integer qid = controller.askQuestion("1", q).getData().getId();
    AnswerRequest ar = new AnswerRequest(); ar.setQuestionId(qid); ar.setAnswer("Ans");
    controller.answerQuestion("2", ar);

    ApiResponse<QuestionWithAnswersResponse> ok = controller.getQuestionWithAnswers(qid);
    assertTrue(ok.isSuccess());

    ApiResponse<QuestionWithAnswersResponse> notFound = controller.getQuestionWithAnswers(-1);
    assertFalse(notFound.isSuccess());
    assertEquals("404", notFound.getMessage());
  }
}
