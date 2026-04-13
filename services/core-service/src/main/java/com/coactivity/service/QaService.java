package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.AnswerUpdateRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.repository.QaRepository;
import com.coactivity.repository.QaRepository.AnswerEntity;
import com.coactivity.repository.QaRepository.QuestionEntity;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public List<QuestionResponse> getQuestions(Integer categoryId, String query) {
    if (query == null || query.trim().isEmpty()) {
      return getQuestions(categoryId);
    }
    List<QuestionEntity> questions = qaRepository.findQuestions(categoryId, query);
    List<QuestionResponse> responses = new ArrayList<>(questions.size());

    for (QuestionEntity question : questions) {
      responses.add(
          new QuestionResponse(question.id(), question.category(), question.question(),
              question.author()));
    }

    return responses;
  }

  public QuestionResponse updateQuestion(Integer userId, Integer questionId,
      QuestionRequest request) {
    Integer authorId = requireUserId(userId);
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }
    validateQuestionRequest(request);

    QuestionEntity existing = qaRepository.findQuestionById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
    if (!authorId.equals(existing.ownerId())) {
      throw new AuthorizationException("Cannot update question created by another user");
    }

    Integer categoryId = qaRepository.findCategoryIdByName(request.getCategory())
        .orElseThrow(() -> new ValidationException("Category not found: " + request.getCategory()));
    QuestionEntity updated = qaRepository.updateQuestion(questionId, request.getQuestion(),
        categoryId);
    return new QuestionResponse(updated.id(), updated.category(), updated.question(),
        updated.author());
  }

  public void deleteQuestion(Integer userId, Integer questionId) {
    Integer authorId = requireUserId(userId);
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }

    QuestionEntity existing = qaRepository.findQuestionById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
    if (!authorId.equals(existing.ownerId())) {
      throw new AuthorizationException("Cannot delete question created by another user");
    }

    qaRepository.deleteQuestion(questionId);
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }

    QuestionEntity question = qaRepository.findQuestionById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    List<AnswerEntity> answers = qaRepository.findAnswersByQuestionId(questionId);
    List<AnswerResponse> answerResponses = buildAnswerTree(answers);

    QuestionResponse questionResponse = new QuestionResponse(question.id(), question.category(),
        question.question(), question.author());

    return new QuestionWithAnswersResponse(questionResponse, answerResponses);
  }

  public AnswerResponse updateAnswer(Integer userId, Integer answerId, AnswerUpdateRequest request) {
    Integer authorId = requireUserId(userId);
    if (answerId == null) {
      throw new ValidationException("Answer ID is required");
    }
    validateAnswerUpdateRequest(request);

    AnswerEntity existing = qaRepository.findAnswerById(answerId)
        .orElseThrow(() -> new ResourceNotFoundException("Answer not found: " + answerId));
    if (!authorId.equals(existing.ownerId())) {
      throw new AuthorizationException("Cannot update answer created by another user");
    }

    return mapAnswer(qaRepository.updateAnswer(answerId, request.getAnswer()));
  }

  public void deleteAnswer(Integer userId, Integer answerId) {
    Integer authorId = requireUserId(userId);
    if (answerId == null) {
      throw new ValidationException("Answer ID is required");
    }

    AnswerEntity existing = qaRepository.findAnswerById(answerId)
        .orElseThrow(() -> new ResourceNotFoundException("Answer not found: " + answerId));
    if (!authorId.equals(existing.ownerId())) {
      throw new AuthorizationException("Cannot delete answer created by another user");
    }

    qaRepository.deleteAnswer(answerId);
  }

  private AnswerResponse mapAnswer(AnswerEntity answer) {
    return mapAnswer(answer, List.of());
  }

  private List<AnswerResponse> buildAnswerTree(List<AnswerEntity> answers) {
    Map<Integer, AnswerResponse> responsesById = new LinkedHashMap<>();
    for (AnswerEntity answer : answers) {
      responsesById.put(answer.id(), mapAnswer(answer, new ArrayList<>()));
    }

    List<AnswerResponse> rootAnswers = new ArrayList<>();
    for (AnswerEntity answer : answers) {
      AnswerResponse response = responsesById.get(answer.id());
      Integer previousAnswerId = answer.previousAnswerId();
      if (previousAnswerId == null) {
        rootAnswers.add(response);
        continue;
      }

      AnswerResponse parent = responsesById.get(previousAnswerId);
      if (parent == null) {
        rootAnswers.add(response);
        continue;
      }

      parent.getReplies().add(response);
    }

    return rootAnswers;
  }

  private AnswerResponse mapAnswer(AnswerEntity answer, List<AnswerResponse> replies) {
    return new AnswerResponse(
        answer.id(),
        answer.questionId(),
        answer.previousAnswerId(),
        answer.answer(),
        answer.author(),
        answer.createdAt(),
        replies);
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

  private void validateAnswerUpdateRequest(AnswerUpdateRequest request) {
    if (request == null) {
      throw new ValidationException("Answer request is required");
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
