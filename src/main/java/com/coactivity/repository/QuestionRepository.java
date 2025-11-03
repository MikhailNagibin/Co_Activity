package com.coactivity.repository;

import com.coactivity.domain.Question;
import com.coactivity.domain.User;

import java.util.List;

public interface QuestionRepository {
  /*
  создание вопроса

  @param User user - автор вопроса
  @param String question - текст вопроса
  @return Question question
   */
  Question createQuestion(User user, String question);

  /*
  получение всех вопросов

  @return List<Question>
   */
  List<Question> getAllQuestions();

  /*
  изменить текст вопроса

  @param String question
  @return Question question
   */
  Question updateQuestion(String question);

  /*
  удаление вопроса

  @param int questionId
   */
  void deleteQuestion(int questionId);
}
