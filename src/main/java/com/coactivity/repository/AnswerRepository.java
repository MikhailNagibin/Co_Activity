package com.coactivity.repository;

import com.coactivity.domain.Answer;
import java.util.List;

public interface AnswerRepository {

  /**
   *
   * @param questionId
   * @param previousAnswerId
   * @param currentAnswer
   * @param ownerId
   * @return Answer
   */
  Answer createAnswer(int questionId, int previousAnswerId, String currentAnswer, int ownerId);

  /**
   *
   * @param questionId
   * @return List<Answer>
   */
  List<Answer> getAnswers(int questionId);

  /**
   *
   * @param answer
   */
  void deleteAnswer(Answer answer);
}
