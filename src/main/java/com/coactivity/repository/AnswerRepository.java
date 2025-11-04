package com.coactivity.repository;

import com.coactivity.domain.Answer;
import com.coactivity.domain.Question;
import com.coactivity.domain.User;

import java.util.List;

public interface AnswerRepository {
  /*
  создать ответ на вопрос

  @param Question question - вопрос, на который отвечают
  @param Answer previousAnswer - комментарием к какому ответу является данное сообщение
  @param String currentAnswer - текст ответа
  @param User owner - отвечающий
  @return Answer
   */
  Answer createAnswer(Question question, Answer previousAnswer, String currentAnswer, User owner);

  /*
  получить все ответы к вопросу

  @param Question question
  @return List<Answer>
   */
  List<Answer> getAnswers(Question question);

  /*

   */
}
