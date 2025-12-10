package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Category;
import com.coactivity.domain.Question;
import com.coactivity.repository.QuestionRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;

  public QuestionRepositoryImpl(DataRepository dataRepository, UserRepositoryImpl userRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Question createQuestion(Integer userId, String question, String category) {
    String sql = "INSERT INTO Questions (owner, question, category_id) VALUES (?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      int categoryId = getCategoryIdByName(category);
      statement.setInt(1, userId);
      statement.setString(2, question);
      statement.setInt(3, categoryId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer questionId = resultSet.getInt("id");
          return new Question(questionId, userRepository.getUserById(userId), question,
            getCategoryById(categoryId));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException();
  }

  public Integer getCategoryIdByName(String categoryName) {
    String sql = "SELECT id FROM Categories WHERE LOWER(name) = LOWER(?)";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, categoryName);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }

      return null;

    } catch (SQLException e) {
      System.err.println("Error getting category ID by name: " + e.getMessage());
      throw new RuntimeException("Database error while getting category ID", e);
    }
  }

  public Category getCategoryById(Integer categoryId) {
    String sql = "SELECT name FROM Categories WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, categoryId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String name = resultSet.getString("name");
          return Category.valueOf(name.toUpperCase().replace(" ", "_"));
        }
      }

      throw new IllegalArgumentException("Category not found: " + categoryId);

    } catch (SQLException e) {
      throw new RuntimeException("Database error", e);
    }
  }

  @Override
  public List<Question> getAllQuestions() {
    var questions = new ArrayList<Question>();
    // Добавлено условие id > 0
    String sql = "SELECT * FROM Questions WHERE id > 0 ORDER BY id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        var question = mapResultSetToQuestion(resultSet);
        questions.add(question);
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return questions;
  }

  public Question getQuestionById(Integer questionId) {
    // Добавлено условие id > 0
    String sql = "SELECT * FROM Questions WHERE id = ? AND id > 0";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToQuestion(resultSet);
        } else {
          throw new RuntimeException();
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
  }

  @Override
  public Question updateQuestion(Integer questionId, String question, Integer categoryId) {
    // Добавлено условие id > 0
    String sql = "UPDATE Questions SET question = ?, category_id = ? WHERE id = ? AND id > 0 RETURNING id, owner, question, category_id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setString(1, question);
      statement.setInt(2, categoryId);
      statement.setInt(3, questionId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToQuestion(resultSet);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public void deleteQuestion(Integer questionId) {
    deleteAllWithQuestion(questionId);
    // Добавлено условие id > 0
    String sql = "DELETE FROM Questions WHERE id = ? AND id > 0";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
  }

  private void deleteAllWithQuestion(Integer questionId) {
    // Добавлено условие question_id > 0
    String sql = "DELETE FROM Answers WHERE question_id = ? AND question_id > 0";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, questionId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
  }

  /**
   * Получает вопросы по категории с фильтрацией id > 0
   */
  public List<Question> getQuestionsByCategory(Integer categoryId) {
    var questions = new ArrayList<Question>();
    // Добавлено условие id > 0
    String sql = "SELECT * FROM Questions WHERE category_id = ? AND id > 0 ORDER BY id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, categoryId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var question = mapResultSetToQuestion(resultSet);
          questions.add(question);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return questions;
  }

  /**
   * Получает вопросы пользователя с фильтрацией id > 0
   */
  public List<Question> getQuestionsByUser(Integer userId) {
    var questions = new ArrayList<Question>();
    // Добавлено условие id > 0
    String sql = "SELECT * FROM Questions WHERE owner = ? AND id > 0 ORDER BY id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var question = mapResultSetToQuestion(resultSet);
          questions.add(question);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return questions;
  }

  /**
   * Проверяет существование вопроса с фильтрацией id > 0
   */
  public boolean questionExists(Integer questionId) {
    // Добавлено условие id > 0
    String sql = "SELECT COUNT(*) FROM Questions WHERE id = ? AND id > 0";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1) > 0;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return false;
  }

  /**
   * Получает количество вопросов с фильтрацией id > 0
   */
  public int getQuestionsCount() {
    // Добавлено условие id > 0
    String sql = "SELECT COUNT(*) FROM Questions WHERE id > 0";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {

      if (resultSet.next()) {
        return resultSet.getInt(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return 0;
  }

  private Question mapResultSetToQuestion(ResultSet resultSet) throws SQLException {
    Integer id = resultSet.getInt("id");
    Integer ownerId = resultSet.getInt("owner");
    String questionText = resultSet.getString("question");
    Integer categoryId = resultSet.getInt("category_id");

    return new Question(id, userRepository.getUserById(ownerId), questionText,
      getCategoryById(categoryId));
  }

  public Question createNullQuestion() {
    String sql = "INSERT INTO Questions (id, owner, question, category_id) VALUES (0, ?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      int categoryId = getCategoryIdByName("SPORT");
      statement.setInt(1, 0);
      statement.setString(2, "Test");
      statement.setInt(3, categoryId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer questionId = resultSet.getInt("id");
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException();
  }

}