package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Category;
import com.coactivity.repository.QaRepository;
import com.coactivity.repository.QaRepository.AnswerEntity;
import com.coactivity.repository.QaRepository.QuestionEntity;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("QaService tests")
class QaServiceTest {

  private QaRepository qaRepository;
  private QaService qaService;

  @BeforeEach
  void setUp() {
    qaRepository = Mockito.mock(QaRepository.class);
    qaService = new QaService(qaRepository);
  }

  @Test
  void getQuestionsReturnsAuthorSummaryFromRepository() {
    UserSummaryResponse author = new UserSummaryResponse(5, "author", null, "Moscow", "Russia",
        "bio", 1);
    when(qaRepository.findQuestions(null)).thenReturn(List.of(
        new QuestionEntity(10, 5, 1, Category.SPORT, "How to train?", author)));

    var questions = qaService.getQuestions(null);

    assertEquals(1, questions.size());
    assertEquals("author", questions.getFirst().getAuthor().getUserName());
    assertEquals("Moscow", questions.getFirst().getAuthor().getCity());
  }

  @Test
  void getQuestionWithAnswersReturnsAnswerAuthorsFromRepository() {
    UserSummaryResponse questionAuthor = new UserSummaryResponse(1, "questionAuthor", null, null,
        null, null, null);
    UserSummaryResponse answerAuthor = new UserSummaryResponse(2, "answerAuthor", null, null, null,
        null, null);

    when(qaRepository.findQuestionById(7)).thenReturn(Optional.of(
        new QuestionEntity(7, 1, 1, Category.SPORT, "Question text", questionAuthor)));
    when(qaRepository.findAnswersByQuestionId(7)).thenReturn(List.of(
        new AnswerEntity(12, 7, null, 2, "Answer text", Instant.parse("2026-01-01T00:00:00Z"),
            answerAuthor)));

    var thread = qaService.getQuestionWithAnswers(7);

    assertEquals("questionAuthor", thread.getQuestion().getAuthor().getUserName());
    assertEquals(1, thread.getAnswers().size());
    assertEquals("answerAuthor", thread.getAnswers().getFirst().getAuthor().getUserName());
    assertEquals("Answer text", thread.getAnswers().getFirst().getAnswer());
  }

  @Test
  void askQuestionRejectsNullUserId() {
    QuestionRequest request = new QuestionRequest("How to train?", "sport");

    ValidationException exception = assertThrows(ValidationException.class,
        () -> qaService.askQuestion(null, request));

    assertEquals("User ID is required", exception.getMessage());
    verify(qaRepository, never()).findCategoryIdByName("sport");
    verify(qaRepository, never()).createQuestion(null, "How to train?", 1);
  }

  @Test
  void answerQuestionRejectsNullUserId() {
    AnswerRequest request = new AnswerRequest(7, null, "Answer text");

    ValidationException exception = assertThrows(ValidationException.class,
        () -> qaService.answerQuestion(null, request));

    assertEquals("User ID is required", exception.getMessage());
    verify(qaRepository, never()).questionExists(7);
    verify(qaRepository, never()).createAnswer(7, null, "Answer text", null);
  }
}
