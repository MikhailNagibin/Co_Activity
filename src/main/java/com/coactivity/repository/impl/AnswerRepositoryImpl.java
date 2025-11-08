package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Answer;
import com.coactivity.repository.AnswerRepository;
import com.coactivity.repository.impl.UserRepositoryImpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class AnswerRepositoryImpl implements AnswerRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;

  public AnswerRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = new UserRepositoryImpl(dataRepository);
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
          return new Answer(answerId, questionId, previousAnswerId, currentAnswer, userRepository.getUserById(ownerId));
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

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
            resultSet.getInt("prevAnsId"), resultSet.getString("answer"),
            userRepository.getUserById(resultSet.getInt("owner")));
          answers.add(answer);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
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
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }
}