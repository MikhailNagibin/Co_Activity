package com.coactivity.qa.service;

import com.coactivity.qa.dto.request.AnswerRequest;
import com.coactivity.qa.dto.request.QuestionRequest;
import com.coactivity.qa.dto.response.AnswerResponse;
import com.coactivity.qa.dto.response.QuestionResponse;
import com.coactivity.qa.dto.response.QuestionWithAnswersResponse;
import com.coactivity.qa.dto.response.UserSummaryResponse;
import com.coactivity.qa.exception.ResourceNotFoundException;
import com.coactivity.qa.exception.ValidationException;
import com.coactivity.qa.repository.QaRepository;
import com.coactivity.qa.repository.QaRepository.AnswerEntity;
import com.coactivity.qa.repository.QaRepository.QuestionEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QaService {

  private final QaRepository qaRepository;

  public QaService(QaRepository qaRepository) {
    this.qaRepository = qaRepository;
  }

  public QuestionResponse askQuestion(Integer userId, QuestionRequest request) {
    validateQuestionRequest(request);

    Integer categoryId = qaRepository.findCategoryIdByName(request.category())
        .orElseThrow(() -> new ValidationException("Category not found: " + request.category()));

    QuestionEntity created = qaRepository.createQuestion(userId, request.question(), categoryId);
    UserSummaryResponse author = getExistingUserSummary(userId);

    return new QuestionResponse(created.id(), created.category(), created.question(), author);
  }

  public AnswerResponse answerQuestion(Integer userId, AnswerRequest request) {
    validateAnswerRequest(request);

    if (!qaRepository.questionExists(request.questionId())) {
      throw new ResourceNotFoundException("Question not found: " + request.questionId());
    }

    if (request.previousAnswerId() != null
        && !qaRepository.answerExistsForQuestion(request.previousAnswerId(), request.questionId())) {
      throw new ResourceNotFoundException("Previous answer not found: " + request.previousAnswerId());
    }

    AnswerEntity created = qaRepository.createAnswer(request.questionId(), request.previousAnswerId(),
        request.answer(), userId);

    UserSummaryResponse author = getExistingUserSummary(userId);
    return mapAnswer(created, author);
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
    List<QuestionEntity> questions = qaRepository.findQuestions(categoryId);
    List<QuestionResponse> responses = new ArrayList<>(questions.size());

    for (QuestionEntity question : questions) {
      UserSummaryResponse author = getExistingUserSummary(question.ownerId());
      responses.add(new QuestionResponse(question.id(), question.category(), question.question(), author));
    }

    return responses;
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }

    QuestionEntity question = qaRepository.findQuestionById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    UserSummaryResponse questionAuthor = getExistingUserSummary(question.ownerId());

    List<AnswerEntity> answers = qaRepository.findAnswersByQuestionId(questionId);
    List<AnswerResponse> answerResponses = new ArrayList<>(answers.size());
    for (AnswerEntity answer : answers) {
      UserSummaryResponse author = getExistingUserSummary(answer.ownerId());
      answerResponses.add(mapAnswer(answer, author));
    }

    QuestionResponse questionResponse = new QuestionResponse(question.id(), question.category(),
        question.question(), questionAuthor);

    return new QuestionWithAnswersResponse(questionResponse, answerResponses);
  }

  private AnswerResponse mapAnswer(AnswerEntity answer, UserSummaryResponse author) {
    return new AnswerResponse(
        answer.id(),
        answer.questionId(),
        answer.previousAnswerId(),
        answer.answer(),
        author,
        answer.createdAt(),
        List.of());
  }

  private UserSummaryResponse getExistingUserSummary(Integer userId) {
    return qaRepository.findUserSummaryById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }

  private void validateQuestionRequest(QuestionRequest request) {
    if (request == null) {
      throw new ValidationException("Question request is required");
    }
    if (request.question() == null || request.question().isBlank()) {
      throw new ValidationException("Question text cannot be empty");
    }
    if (request.category() == null || request.category().isBlank()) {
      throw new ValidationException("Category is required");
    }
  }

  private void validateAnswerRequest(AnswerRequest request) {
    if (request == null) {
      throw new ValidationException("Answer request is required");
    }
    if (request.questionId() == null || request.questionId() <= 0) {
      throw new ValidationException("Question ID is required");
    }
    if (request.answer() == null || request.answer().isBlank()) {
      throw new ValidationException("Answer text cannot be empty");
    }
  }
}
