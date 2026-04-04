package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.service.QaGatewayService;
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
public class QAControllerImpl {

  private final QaGatewayService qaGatewayService;
  private final TokenService tokenService;

  public QAControllerImpl(QaGatewayService qaGatewayService, TokenService tokenService) {
    this.qaGatewayService = qaGatewayService;
    this.tokenService = tokenService;
  }

  @PostMapping("/questions")
  public ResponseEntity<QuestionResponse> askQuestion(
    @RequestHeader(name = "Authorization", required = false) String token,
    @Valid @RequestBody QuestionRequest request) {
    resolveAuthorizedUserId(token);
    QuestionResponse response = qaGatewayService.askQuestion(token, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/answers")
  public ResponseEntity<AnswerResponse> answerQuestion(
    @RequestHeader(name = "Authorization", required = false) String token,
    @Valid @RequestBody AnswerRequest request) {
    resolveAuthorizedUserId(token);
    AnswerResponse response = qaGatewayService.answerQuestion(token, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/questions/category")
  public ResponseEntity<List<QuestionResponse>> getQuestions(
    @RequestParam(name = "categoryId", required = false) Integer categoryId) {
    List<QuestionResponse> responses = qaGatewayService.getQuestions(categoryId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/questions/{questionId}")
  public ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(
      @Positive @PathVariable Integer questionId) {
    QuestionWithAnswersResponse response = qaGatewayService.getQuestionWithAnswers(questionId);
    return ResponseEntity.ok(response);
  }

  /**
   * Получает все вопросы без фильтрации по категории.
   *
   * @return список всех вопросов в системе
   */
  @GetMapping("/questions")
  public ResponseEntity<List<QuestionResponse>> getAllQuestions() {
    List<QuestionResponse> responses = qaGatewayService.getAllQuestions();
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
