package com.coactivity.repository;

import com.coactivity.domain.Question;
import com.coactivity.domain.User;

import java.util.List;

public interface QuestionRepository {
  /*
  создание вопроса

  @param int userId - автор вопроса
  @param String question - текст вопроса
  @param int categoryId
  @return Question question
   */
  Question createQuestion(int userId, String question, int categoryId);

  /*
  получение всех вопросов

  @return List<Question>
   */
  List<Question> getAllQuestions();

  /*
  получение вопроса по его id

  @param int questionId
  @return Question
   */
  Question getQuestionById(int questionId);

  /*
  изменить текст вопроса

  @param int questionId
  @param String question
  @param int categoryId
  @return Question
   */
  Question updateQuestion(int questionId, String question, int categoryId);

  /*
  удаление вопроса

  @param int questionId
   */
  void deleteQuestion(int questionId);
}
