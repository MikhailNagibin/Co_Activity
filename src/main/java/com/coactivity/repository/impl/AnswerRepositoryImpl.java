package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Answer;
import com.coactivity.repository.AnswerRepository;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AnswerRepositoryImpl implements AnswerRepository {

  private final DataRepository dataRepository;

  // Внедряем зависимость через конструктор
  public AnswerRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @Override
  public Answer createAnswer(int questionId, int previousAnswerId, String currentAnswer, int ownerId) {
    String sql = "INSERT INTO answers (question_id, prev_ans_id, answer, owner) VALUES (?, ?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      statement.setInt(2, previousAnswerId);
      statement.setString(3, currentAnswer);
      statement.setInt(4, ownerId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int answerId = resultSet.getInt("id");
          return new Answer(answerId, questionId, previousAnswerId, currentAnswer, ownerId);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException("Failed to create answer", e);
    }
    throw new RuntimeException("Answer creation failed - no ID returned");
  }

  // Остальные методы остаются без изменений
  @Override
  public List<Answer> getAnswers(int questionId) {
    var answers = new ArrayList<Answer>();
    String sql = "SELECT * FROM answers WHERE question_id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var answer = new Answer(resultSet.getInt("id"), questionId,
            resultSet.getString("prev_ans_id"), resultSet.getString("answer"),
            resultSet.getInt("owner"));
          answers.add(answer);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get answers for question: " + questionId, e);
    }
    return answers;
  }

  @Override
  public void deleteAnswer(Answer answer) {
    String sql = "DELETE FROM answers WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, answer.getId());
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
  }
}