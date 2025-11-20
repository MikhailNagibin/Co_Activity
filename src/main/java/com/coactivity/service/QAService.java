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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // 1. Validate input
    if (request == null || request.getQuestion() == null || request.getQuestion().trim()
        .isEmpty()) {
      throw new IllegalArgumentException("Question text cannot be empty");
    }
    if (request.getCategoryId() == null) {
      throw new IllegalArgumentException("Category ID is required");
    }

    // 2. Create domain object via repository
    // Note: This call uses Category.getByIndex(categoryId), which is problematic if categoryId is a DB ID.
    // For now, we assume the request's categoryId aligns with the enum's index or is handled correctly by the repository.
    Question domainQuestion = questionRepository.createQuestion(userId, request.getQuestion(),
        request.getCategoryId());

    // 3. Map domain object to DTO
    // The author is the user making the request
    User author = userRepository.getUserById(userId);
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    QuestionResponse response = mapQuestionResponse(domainQuestion, authorProfile);

    return response;
  }

  public AnswerResponse answerQuestion(Integer userId, AnswerRequest request) {
    // 1. Validate input
    if (request == null || request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
      throw new IllegalArgumentException("Answer text cannot be empty");
    }
    if (request.getQuestionId() == null) {
      throw new IllegalArgumentException("Question ID is required");
    }

    // 2. Check if the parent question exists
    Question parentQuestion = questionRepository.getQuestionById(request.getQuestionId());
    if (parentQuestion == null) {
      throw new IllegalArgumentException("Question not found");
    }

    // 3. Check if the previous answer exists (if provided)
    if (request.getPreviousAnswerId() != null) {
      List<Answer> allAnswers = answerRepository.getAnswers(request.getQuestionId());
      boolean prevAnswerExists = allAnswers.stream()
          .anyMatch(answer -> answer.getId().equals(request.getPreviousAnswerId()));
      if (!prevAnswerExists) {
        throw new IllegalArgumentException("Previous answer not found");
      }
    }

    // 4. Create domain object via repository
    Answer domainAnswer = answerRepository.createAnswer(request.getQuestionId(),
        request.getPreviousAnswerId(), request.getAnswer(), userId);

    // 5. Map domain object to DTO
    User author = userRepository.getUserById(userId);
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    AnswerResponse response = mapAnswerResponse(domainAnswer, authorProfile);

    return response;
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
    List<Question> domainQuestions;

    if (categoryId != null) {
      // We need to filter manually since there's no repository method for category
      domainQuestions = questionRepository.getAllQuestions().stream()
          .filter(q -> q.getCategory().equals(Category.getByIndex(
              categoryId))) // This assumes getByIndex works correctly for DB IDs
          .collect(Collectors.toList());
    } else {
      domainQuestions = questionRepository.getAllQuestions();
    }

    List<QuestionResponse> responses = new ArrayList<>();
    for (Question q : domainQuestions) {
      // Map each domain question to DTO
      User author = q.getOwner();
      UserSummaryResponse authorProfile = mapUserSummaryResponse(author);

      QuestionResponse resp = mapQuestionResponse(q, authorProfile);
      responses.add(resp);
    }

    return responses;
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    // 1. Get the question
    Question domainQuestion = questionRepository.getQuestionById(questionId);
    if (domainQuestion == null) {
      throw new IllegalArgumentException("Question not found");
    }

    // 2. Map the question to DTO
    QuestionResponse questionResponse = new QuestionResponse();
    questionResponse.setId(domainQuestion.getId());
    questionResponse.setCategory(domainQuestion.getCategory());
    questionResponse.setQuestion(domainQuestion.getQuestion());

    User author = domainQuestion.getOwner();
    UserSummaryResponse authorProfile = mapUserSummaryResponse(author);
    questionResponse.setAuthor(authorProfile);

    List<Answer> allAnswers = answerRepository.getAnswers(questionId);

    // Build the hierarchical answer structure
    // First, create DTOs for all answers
    Map<Integer, AnswerResponse> answerResponseMap = new HashMap<>();
    for (Answer a : allAnswers) {
      // Fetch the author of the answer from the repository
      User answerAuthor = userRepository.getUserById(a.getOwnerId().getId());
      UserSummaryResponse answerAuthorProfile = mapUserSummaryResponse(answerAuthor);
      AnswerResponse answerResp = mapAnswerResponse(a, answerAuthorProfile);

      answerResponseMap.put(answerResp.getId(), answerResp);
    }

    // Now build the tree: top-level answers and nested replies
    List<AnswerResponse> topLevelAnswers = new ArrayList<>();
    for (AnswerResponse answerResp : answerResponseMap.values()) {
      Integer parentId = answerResp.getPreviousAnswerId();
      if (parentId == null) {
        // This is a top-level answer
        topLevelAnswers.add(answerResp);
      } else {
        // This is a nested answer, add it to the parent's replies
        AnswerResponse parent = answerResponseMap.get(parentId);
        if (parent != null) { // Just in case
          parent.getReplies().add(answerResp);
        }
      }
    }

    return new QuestionWithAnswersResponse(questionResponse, topLevelAnswers);
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

  private QuestionResponse mapQuestionResponse(Question question, UserSummaryResponse authorProfile) {
    QuestionResponse questionResponse = new QuestionResponse();
    questionResponse.setId(question.getId());
    questionResponse.setCategory(question.getCategory());
    questionResponse.setQuestion(question.getQuestion());
    questionResponse.setAuthor(authorProfile);

    return questionResponse;
  }
}
