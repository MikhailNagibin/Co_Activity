package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Answer;
import com.coactivity.domain.Category;
import com.coactivity.domain.Question;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.AnswerRepositoryImpl;
import com.coactivity.repository.impl.QuestionRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class QAService {

  private final QuestionRepositoryImpl questionRepository;
  private final AnswerRepositoryImpl answerRepository;
  private final UserRepositoryImpl userRepository;

  public QAService(QuestionRepositoryImpl questionRepository, AnswerRepositoryImpl answerRepository,
      UserRepositoryImpl userRepository) {
    this.questionRepository = questionRepository;
    this.answerRepository = answerRepository;
    this.userRepository = userRepository;
  }

  public QuestionResponse askQuestion(Integer userId, QuestionRequest request) {
    validateQuestionRequest(request);

    Question domainQuestion = questionRepository.createQuestion(userId, request.getQuestion(),
        request.getCategory());

    User author = getExistingUser(userId);
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    return mapQuestionResponse(domainQuestion, authorProfile);
  }

  public AnswerResponse answerQuestion(Integer userId, AnswerRequest request) {
    validateAnswerRequest(request);

    getExistingQuestion(request.getQuestionId());
    if (request.getPreviousAnswerId() != null) {
      ensurePreviousAnswerExists(request.getQuestionId(), request.getPreviousAnswerId());
    }
    int prevAnsId = request.getPreviousAnswerId() != null ? request.getPreviousAnswerId() : 0;
    Answer domainAnswer = answerRepository.createAnswer(request.getQuestionId(),
        prevAnsId, request.getAnswer(), userId);

    User author = getExistingUser(userId);
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    return mapAnswerResponse(domainAnswer, authorProfile);
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
    Category categoryFilter = resolveCategory(categoryId);
    List<Question> domainQuestions;

    if (categoryFilter != null) {
      domainQuestions = questionRepository.getAllQuestions().stream()
          .filter(q -> q.getCategory().equals(categoryFilter))
          .collect(Collectors.toList());
    } else {
      domainQuestions = questionRepository.getAllQuestions();
    }

    List<QuestionResponse> responses = new ArrayList<>();
    for (Question q : domainQuestions) {
      User author = getExistingUser(q.getOwner().getId());
      UserSummaryResponse authorProfile = mapUserSummaryResponse(author);

      QuestionResponse resp = mapQuestionResponse(q, authorProfile);
      responses.add(resp);
    }

    return responses;
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    if (questionId == null) {
      throw new ValidationException("Question ID is required");
    }
    Question domainQuestion = getExistingQuestion(questionId);

    QuestionResponse questionResponse = new QuestionResponse();
    questionResponse.setId(domainQuestion.getId());
    questionResponse.setCategory(domainQuestion.getCategory());
    questionResponse.setQuestion(domainQuestion.getQuestion());

    User author = getExistingUser(domainQuestion.getOwner().getId());
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    questionResponse.setAuthor(authorProfile);

    List<Answer> allAnswers = answerRepository.getAnswers(questionId);
    ArrayList<AnswerResponse> answerResponse = new ArrayList<>();
    for (Answer a : allAnswers) {
      User answerAuthor = getExistingUser(a.getOwnerId().getId());
      UserSummaryResponse answerAuthorProfile = mapUserSummaryResponse(answerAuthor);
      AnswerResponse answerResp = mapAnswerResponse(a, answerAuthorProfile);

      answerResponse.add(answerResp);
    }

    return new QuestionWithAnswersResponse(questionResponse, answerResponse);
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }
    return user;
  }

  private Question getExistingQuestion(Integer questionId) {
            Question question = questionRepository.getQuestionById(questionId);
    if (question == null) {
      throw new ResourceNotFoundException("Question not found: " + questionId);
    }
    return question;
  }

  private void ensurePreviousAnswerExists(Integer questionId, Integer previousAnswerId) {
    List<Answer> allAnswers = answerRepository.getAnswers(questionId);
    boolean prevAnswerExists = allAnswers.stream()
        .anyMatch(answer -> answer.getId().equals(previousAnswerId));
    if (!prevAnswerExists) {
      throw new ResourceNotFoundException("Previous answer not found: " + previousAnswerId);
    }
  }

  private Category resolveCategory(Integer categoryId) {
    if (categoryId == null) {
      return null;
    }
    try {
      return questionRepository.getCategoryById(categoryId);
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid category id: " + categoryId, e);
    }
  }

  private void validateQuestionRequest(QuestionRequest request) {
    if (request == null) {
      throw new ValidationException("Question request is required");
    }
    if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
      throw new ValidationException("Question text cannot be empty");
    }
    if (request.getCategory() == null) {
      throw new ValidationException("Category ID is required");
    }
  }

  private void validateAnswerRequest(AnswerRequest request) {
    if (request == null) {
      throw new ValidationException("Answer request is required");
    }
    if (request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
      throw new ValidationException("Answer text cannot be empty");
    }
    if (request.getQuestionId() == null) {
      throw new ValidationException("Question ID is required");
    }
  }

  private UserSummaryResponse mapUserSummaryResponse(User author) {
    UserSummaryResponse userSummaryResponse = new UserSummaryResponse();
    userSummaryResponse.setId(author.getId());
    userSummaryResponse.setUserName(author.getUserName());
    userSummaryResponse.setDateOfBirth(author.getDataOfBirth());
    userSummaryResponse.setCity(author.getCity());
    userSummaryResponse.setCountry(author.getCountry());
    userSummaryResponse.setDescription(author.getDescription());
    userSummaryResponse.setAvatarId(author.getAvatarId());

    return userSummaryResponse;
  }

  private AnswerResponse mapAnswerResponse(Answer answer, UserSummaryResponse authorProfile) {
    AnswerResponse answerResponse = new AnswerResponse();
    answerResponse.setId(answer.getId());
    answerResponse.setQuestionId(answer.getQuestionId());
    answerResponse.setPreviousAnswerId(answer.getPreviousAnswerId());
    answerResponse.setAnswer(answer.getAnswer());
    answerResponse.setAuthor(authorProfile);
    answerResponse.setCreatedAt(answer.getCreatedAt());
    answerResponse.setReplies(new ArrayList<>());

    return answerResponse;
  }

  private QuestionResponse mapQuestionResponse(Question question,
      UserSummaryResponse authorProfile) {
    QuestionResponse questionResponse = new QuestionResponse();
    questionResponse.setId(question.getId());
    questionResponse.setCategory(question.getCategory());
    questionResponse.setQuestion(question.getQuestion());
    questionResponse.setAuthor(authorProfile);

    return questionResponse;
  }
}
