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
  public Question createQuestion(int userId, String question, int categoryId) {
    String sql = "INSERT INTO questions (owner, question, category_id) VALUES (?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setString(2, question);
      statement.setInt(3, categoryId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int questionId = resultSet.getInt("id");
          return new Question(questionId, userRepository.getUserById(userId), question,
              Category.getByIndex(categoryId));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public List<Question> getAllQuestions() {
    var questions = new ArrayList<Question>();
    String sql = "SELECT * FROM questions ORDER BY id";

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

  public Question getQuestionById(int questionId) {
    String sql = "SELECT * FROM questions WHERE id = ?";

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
  public Question updateQuestion(int questionId, String question, int categoryId) {
    String sql = "UPDATE questions SET question = ?, category_id = ? WHERE id = ? RETURNING id, owner, question, category_id";

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
  public void deleteQuestion(int questionId) {
    deleteAllWithQuestion(questionId);
    String sql = "DELETE FROM questions WHERE id = ?";

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

  private void deleteAllWithQuestion(int questionId) {
    String sql = "DELETE FROM Answers WHERE question_id = ?";

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

  private Question mapResultSetToQuestion(ResultSet resultSet) throws SQLException {
    int id = resultSet.getInt("id");
    int ownerId = resultSet.getInt("owner");
    String questionText = resultSet.getString("question");
    int categoryId = resultSet.getInt("category_id");

    return new Question(id, userRepository.getUserById(ownerId), questionText,
        Category.getByIndex(categoryId));
  }
}