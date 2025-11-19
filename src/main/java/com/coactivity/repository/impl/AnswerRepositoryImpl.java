package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Answer;
import com.coactivity.repository.AnswerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AnswerRepositoryImpl implements AnswerRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;

  public AnswerRepositoryImpl(DataRepository dataRepository, UserRepositoryImpl userRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Answer createAnswer(Integer questionId, Integer previousAnswerId, String currentAnswer,
      Integer ownerId) {
    String sql = "INSERT INTO answers (question_id, prev_ans_id, answer, owner) VALUES (?, ?, ?, ?) RETURNING id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      statement.setInt(2, previousAnswerId);
      statement.setString(3, currentAnswer);
      statement.setInt(4, ownerId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer answerId = resultSet.getInt("id");
          return new Answer(answerId, questionId, previousAnswerId, currentAnswer,
              userRepository.getUserById(ownerId), Instant.now());
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public List<Answer> getAnswers(Integer questionId) {
    var answers = new ArrayList<Answer>();
    String sql = "SELECT * FROM answers WHERE question_id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, questionId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var answer = new Answer(resultSet.getInt("id"), questionId,
              resultSet.getInt("prev_ans_id"), resultSet.getString("answer"),
              userRepository.getUserById(resultSet.getInt("owner")),
              resultSet.getTimestamp("created_at").toInstant());
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