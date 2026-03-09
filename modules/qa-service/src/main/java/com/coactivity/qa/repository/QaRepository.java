package com.coactivity.qa.repository;

import com.coactivity.qa.domain.Category;
import com.coactivity.qa.dto.response.UserSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QaRepository {

  private final JdbcTemplate jdbcTemplate;

  public QaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<Integer> findCategoryIdByName(String categoryName) {
    String normalized = toDbCategoryName(categoryName);
    String sql = "SELECT id FROM Categories WHERE LOWER(name) = LOWER(?)";
    try {
      Integer id = jdbcTemplate.queryForObject(sql, Integer.class, normalized);
      return Optional.ofNullable(id);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Optional<QuestionEntity> findQuestionById(Integer questionId) {
    String sql = """
        SELECT q.id, q.owner, q.question, q.category_id, c.name AS category_name
        FROM Questions q
        JOIN Categories c ON c.id = q.category_id
        WHERE q.id = ?
        """;
    List<QuestionEntity> rows = jdbcTemplate.query(sql, this::mapQuestion, questionId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.getFirst());
  }

  public List<QuestionEntity> findQuestions(Integer categoryId) {
    if (categoryId != null) {
      String sql = """
          SELECT q.id, q.owner, q.question, q.category_id, c.name AS category_name
          FROM Questions q
          JOIN Categories c ON c.id = q.category_id
          WHERE q.category_id = ?
          ORDER BY q.id
          """;
      return jdbcTemplate.query(sql, this::mapQuestion, categoryId);
    }

    String sql = """
        SELECT q.id, q.owner, q.question, q.category_id, c.name AS category_name
        FROM Questions q
        JOIN Categories c ON c.id = q.category_id
        ORDER BY q.id
        """;
    return jdbcTemplate.query(sql, this::mapQuestion);
  }

  public QuestionEntity createQuestion(Integer userId, String question, Integer categoryId) {
    String sql = """
        INSERT INTO Questions (owner, question, category_id)
        VALUES (?, ?, ?)
        RETURNING id
        """;
    Integer questionId = jdbcTemplate.queryForObject(sql, Integer.class, userId, question, categoryId);
    if (questionId == null) {
      throw new IllegalStateException("Unable to create question");
    }
    return findQuestionById(questionId)
        .orElseThrow(() -> new IllegalStateException("Question was created but not found"));
  }

  public boolean questionExists(Integer questionId) {
    String sql = "SELECT COUNT(*) FROM Questions WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, questionId);
    return count != null && count > 0;
  }

  public boolean answerExistsForQuestion(Integer answerId, Integer questionId) {
    String sql = "SELECT COUNT(*) FROM Answers WHERE id = ? AND question_id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, answerId, questionId);
    return count != null && count > 0;
  }

  public AnswerEntity createAnswer(Integer questionId, Integer previousAnswerId, String answer,
      Integer ownerId) {
    String sql = """
        INSERT INTO Answers (question_id, prev_ans_id, answer, owner)
        VALUES (?, ?, ?, ?)
        RETURNING id, question_id, prev_ans_id, answer, owner, created_at
        """;
    return jdbcTemplate.queryForObject(sql, this::mapAnswer, questionId, previousAnswerId, answer,
        ownerId);
  }

  public List<AnswerEntity> findAnswersByQuestionId(Integer questionId) {
    String sql = """
        SELECT id, question_id, prev_ans_id, answer, owner, created_at
        FROM Answers
        WHERE question_id = ?
        ORDER BY id
        """;
    return jdbcTemplate.query(sql, this::mapAnswer, questionId);
  }

  public Optional<UserSummaryResponse> findUserSummaryById(Integer userId) {
    String sql = """
        SELECT id, username, birthday, city, country, description, avatar_id
        FROM Users
        WHERE id = ?
        """;
    List<UserSummaryResponse> rows = jdbcTemplate.query(sql, (rs, rowNum) ->
        new UserSummaryResponse(
            rs.getInt("id"),
            rs.getString("username"),
            toInstant(rs.getTimestamp("birthday")),
            rs.getString("city"),
            rs.getString("country"),
            rs.getString("description"),
            rs.getInt("avatar_id")), userId);

    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.getFirst());
  }

  private QuestionEntity mapQuestion(ResultSet rs, int rowNum) throws SQLException {
    return new QuestionEntity(
        rs.getInt("id"),
        rs.getInt("owner"),
        rs.getInt("category_id"),
        toCategoryEnum(rs.getString("category_name")),
        rs.getString("question"));
  }

  private AnswerEntity mapAnswer(ResultSet rs, int rowNum) throws SQLException {
    int prevAnsRaw = rs.getInt("prev_ans_id");
    Integer previousAnswerId = rs.wasNull() ? null : prevAnsRaw;

    return new AnswerEntity(
        rs.getInt("id"),
        rs.getInt("question_id"),
        previousAnswerId,
        rs.getInt("owner"),
        rs.getString("answer"),
        toInstant(rs.getTimestamp("created_at")));
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp != null ? timestamp.toInstant() : Instant.now();
  }

  private String toDbCategoryName(String rawCategoryName) {
    if (rawCategoryName == null || rawCategoryName.isBlank()) {
      return rawCategoryName;
    }

    String normalized = rawCategoryName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    return switch (normalized) {
      case "sport" -> "Sport";
      case "music" -> "Music";
      case "art" -> "Art";
      case "entertainments" -> "Entertainments";
      case "business" -> "Business";
      case "education" -> "Education";
      case "activerecreation", "activerecreationcategory" -> "ActiveRecreation";
      case "passiverecreation", "passiverecreationcategory" -> "PassiveRecreation";
      case "massevent", "isamassevent" -> "MassEvent";
      case "other" -> "Other";
      case "notspecified" -> "NotSpecified";
      default -> rawCategoryName;
    };
  }

  private Category toCategoryEnum(String dbCategoryName) {
    String normalized = dbCategoryName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    return switch (normalized) {
      case "sport" -> Category.SPORT;
      case "music" -> Category.MUSIC;
      case "art" -> Category.ART;
      case "entertainments" -> Category.ENTERTAINMENTS;
      case "business" -> Category.BUSINESS;
      case "education" -> Category.EDUCATION;
      case "activerecreation" -> Category.ACTIVE_RECREATION;
      case "passiverecreation" -> Category.PASSIVE_RECREATION;
      case "massevent", "isamassevent" -> Category.IS_A_MASS_EVENT;
      case "other" -> Category.OTHER;
      case "notspecified" -> Category.NOT_SPECIFIED;
      default -> throw new IllegalArgumentException("Unsupported category in DB: " + dbCategoryName);
    };
  }

  public record QuestionEntity(
      Integer id,
      Integer ownerId,
      Integer categoryId,
      Category category,
      String question) {
  }

  public record AnswerEntity(
      Integer id,
      Integer questionId,
      Integer previousAnswerId,
      Integer ownerId,
      String answer,
      Instant createdAt) {
  }
}
