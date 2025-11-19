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
  Answer createAnswer(Integer questionId, Integer previousAnswerId, String currentAnswer, Integer ownerId);

  /**
   *
   * @param questionId
   * @return List<Answer>
   */
  List<Answer> getAnswers(Integer questionId);

  /**
   *
   * @param answer
   */
  void deleteAnswer(Answer answer);
}
