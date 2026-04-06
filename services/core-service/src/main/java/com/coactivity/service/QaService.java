package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.repository.QaRepository;
import com.coactivity.repository.QaRepository.AnswerEntity;
import com.coactivity.repository.QaRepository.QuestionEntity;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
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
    Integer authorId = requireUserId(userId);
    validateQuestionRequest(request);

    Integer categoryId = qaRepository.findCategoryIdByName(request.getCategory())
        .orElseThrow(() -> new ValidationException("Category not found: " + request.getCategory()));

    QuestionEntity created = qaRepository.createQuestion(authorId, request.getQuestion(), categoryId);
    return new QuestionResponse(created.id(), created.category(), created.question(),
        created.author());
  }

  public AnswerResponse answerQuestion(Integer userId, AnswerRequest request) {
    Integer authorId = requireUserId(userId);
    validateAnswerRequest(request);

    if (!qaRepository.questionExists(request.getQuestionId())) {
      throw new ResourceNotFoundException("Question not found: " + request.getQuestionId());
    }
    if (request.getPreviousAnswerId() != null
        && !qaRepository.answerExistsForQuestion(request.getPreviousAnswerId(),
        request.getQuestionId())) {
      throw new ResourceNotFoundException(
          "Previous answer not found: " + request.getPreviousAnswerId());
    }

    AnswerEntity created = qaRepository.createAnswer(request.getQuestionId(),
        request.getPreviousAnswerId(), request.getAnswer(), authorId);

    return mapAnswer(created);
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
    List<QuestionEntity> questions = qaRepository.findQuestions(categoryId);
    List<QuestionResponse> responses = new ArrayList<>(questions.size());

    for (QuestionEntity question : questions) {
      responses.add(
          new QuestionResponse(question.id(), question.category(), question.question(),
              question.author()));
    }

    return responses;
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }

    QuestionEntity question = qaRepository.findQuestionById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    List<AnswerEntity> answers = qaRepository.findAnswersByQuestionId(questionId);
    List<AnswerResponse> answerResponses = new ArrayList<>(answers.size());
    for (AnswerEntity answer : answers) {
      answerResponses.add(mapAnswer(answer));
    }

    QuestionResponse questionResponse = new QuestionResponse(question.id(), question.category(),
        question.question(), question.author());

    return new QuestionWithAnswersResponse(questionResponse, answerResponses);
  }

  private AnswerResponse mapAnswer(AnswerEntity answer) {
    return new AnswerResponse(
        answer.id(),
        answer.questionId(),
        answer.previousAnswerId(),
        answer.answer(),
        answer.author(),
        answer.createdAt(),
        List.of());
  }

  private void validateQuestionRequest(QuestionRequest request) {
    if (request == null) {
      throw new ValidationException("Question request is required");
    }
    if (request.getQuestion() == null || request.getQuestion().isBlank()) {
      throw new ValidationException("Question text cannot be empty");
    }
    if (request.getCategory() == null || request.getCategory().isBlank()) {
      throw new ValidationException("Category is required");
    }
  }

  private void validateAnswerRequest(AnswerRequest request) {
    if (request == null) {
      throw new ValidationException("Answer request is required");
    }
    if (request.getQuestionId() == null || request.getQuestionId() <= 0) {
      throw new ValidationException("Question ID is required");
    }
    if (request.getAnswer() == null || request.getAnswer().isBlank()) {
      throw new ValidationException("Answer text cannot be empty");
    }
  }

  private Integer requireUserId(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User ID is required");
    }
    return userId;
  }
}
