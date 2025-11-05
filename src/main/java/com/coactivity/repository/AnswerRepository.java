package com.coactivity.repository;

import com.coactivity.domain.Answer;
import com.coactivity.domain.Question;
import com.coactivity.domain.User;

import java.util.List;

public interface AnswerRepository {
  /*
  создать ответ на вопрос

  @param int questionId - вопрос, на который отвечают
  @param int previousAnswerID - комментарием к какому ответу является данное сообщение
  @param String currentAnswer - текст ответа
  @param int ownerId - отвечающий
  @return Answer
   */
  Answer createAnswer(int questionId, int previousAnswerId, String currentAnswer, int ownerId);

  /*
  получить все ответы к вопросу

  @param int questionId
  @return List<Answer>
   */
  List<Answer> getAnswers(int questionId);

  /*
   удалить ответ на вопрос

   @param Answer answer
   */
  void deleteAnswer(Answer answer);
}
