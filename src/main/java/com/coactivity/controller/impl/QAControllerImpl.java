package com.coactivity.controller.impl;

import com.coactivity.controller.QAController;
import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.service.QAService;
import com.coactivity.service.TokenService;
import com.coactivity.service.exception.TokenValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/qa")
public class QAControllerImpl implements QAController {

  private final QAService qaService;
  private final TokenService tokenService;

  public QAControllerImpl(QAService qaService, TokenService tokenService) {
    this.qaService = qaService;
    this.tokenService = tokenService;
  }

  @Override
  @PostMapping("/questions")
  public ResponseEntity<QuestionResponse> askQuestion(
    @RequestHeader(name = "Authorization", required = false) String token,
    @Valid @RequestBody QuestionRequest request) {
    Integer userId = resolveAuthorizedUserId(token);
    QuestionResponse response = qaService.askQuestion(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PostMapping("/answers")
  public ResponseEntity<AnswerResponse> answerQuestion(
    @RequestHeader(name = "Authorization", required = false) String token,
    @Valid @RequestBody AnswerRequest request) {
    Integer userId = resolveAuthorizedUserId(token);
    AnswerResponse response = qaService.answerQuestion(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @GetMapping("/questions/category")
  public ResponseEntity<List<QuestionResponse>> getQuestions(
    @RequestParam(name = "categoryId", required = false) Integer categoryId) {
    List<QuestionResponse> responses = qaService.getQuestions(categoryId);
    return ResponseEntity.ok(responses);
  }

  @Override
  @GetMapping("/questions/{questionId}")
  public ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(
    @PathVariable @Positive Integer questionId) {
    QuestionWithAnswersResponse response = qaService.getQuestionWithAnswers(questionId);
    return ResponseEntity.ok(response);
  }

  /**
   * Получает все вопросы без фильтрации по категории.
   *
   * @return список всех вопросов в системе
   */
  @GetMapping("/questions")
  public ResponseEntity<List<QuestionResponse>> getAllQuestions() {
    List<QuestionResponse> responses = qaService.getQuestions(null);
    return ResponseEntity.ok(responses);
  }

  private Integer resolveAuthorizedUserId(String tokenHeader) {
    String authToken = extractToken(tokenHeader);
    if (authToken == null || authToken.isBlank()) {
      throw new TokenValidationException("Authorization token is required");
    }
    if (!tokenService.isTokenActive(authToken)) {
      throw new TokenValidationException("Token is inactive or expired");
    }
    return tokenService.decodeToken(authToken).userId();
  }

  private String extractToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
  }
}