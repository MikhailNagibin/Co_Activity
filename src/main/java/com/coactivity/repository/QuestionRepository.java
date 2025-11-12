package com.coactivity.repository;

import com.coactivity.domain.Question;
import com.coactivity.domain.User;

import java.util.List;

public interface QuestionRepository {
  /**
   *
   * @param userId
   * @param question
   * @param categoryId
   * @return
   */
  Question createQuestion(int userId, String question, int categoryId);

  /**
   *
   * @return
   */
  List<Question> getAllQuestions();

  /**
   *
   * @param questionId
   * @return
   */
  Question getQuestionById(int questionId);

  /**
   *
   * @param questionId
   * @param question
   * @param categoryId
   * @return
   */
  Question updateQuestion(int questionId, String question, int categoryId);

  /**
   *
   * @param questionId
   */
  void deleteQuestion(int questionId);
}
