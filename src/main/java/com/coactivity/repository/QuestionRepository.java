package com.coactivity.repository;

import com.coactivity.domain.Question;
import java.util.List;

public interface QuestionRepository {

  /**
   *
   * @param userId
   * @param question
   * @param categoryId
   * @return
   */
  Question createQuestion(Integer userId, String question, String categoryId);

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
  Question getQuestionById(Integer questionId);

  /**
   *
   * @param questionId
   * @param question
   * @param categoryId
   * @return
   */
  Question updateQuestion(Integer questionId, String question, Integer categoryId);

  /**
   *
   * @param questionId
   */
  void deleteQuestion(Integer questionId);
}
