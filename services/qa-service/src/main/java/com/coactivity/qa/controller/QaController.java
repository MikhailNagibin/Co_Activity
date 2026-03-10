package com.coactivity.qa.controller;

import com.coactivity.qa.dto.request.AnswerRequest;
import com.coactivity.qa.dto.request.QuestionRequest;
import com.coactivity.qa.dto.response.AnswerResponse;
import com.coactivity.qa.dto.response.QuestionResponse;
import com.coactivity.qa.dto.response.QuestionWithAnswersResponse;
import com.coactivity.qa.security.JwtTokenService;
import com.coactivity.qa.service.QaService;
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
public class QaController {

  private final QaService qaService;
  private final JwtTokenService jwtTokenService;

  public QaController(QaService qaService, JwtTokenService jwtTokenService) {
    this.qaService = qaService;
    this.jwtTokenService = jwtTokenService;
  }

  @PostMapping("/questions")
  public ResponseEntity<QuestionResponse> askQuestion(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody QuestionRequest request) {
    Integer userId = jwtTokenService.resolveAuthorizedUserId(token);
    QuestionResponse response = qaService.askQuestion(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/answers")
  public ResponseEntity<AnswerResponse> answerQuestion(
      @RequestHeader(name = "Authorization", required = false) String token,
      @Valid @RequestBody AnswerRequest request) {
    Integer userId = jwtTokenService.resolveAuthorizedUserId(token);
    AnswerResponse response = qaService.answerQuestion(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/questions/category")
  public ResponseEntity<List<QuestionResponse>> getQuestions(
      @RequestParam(name = "categoryId", required = false) Integer categoryId) {
    List<QuestionResponse> responses = qaService.getQuestions(categoryId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/questions/{questionId}")
  public ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(
      @PathVariable @Positive Integer questionId) {
    QuestionWithAnswersResponse response = qaService.getQuestionWithAnswers(questionId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/questions")
  public ResponseEntity<List<QuestionResponse>> getAllQuestions() {
    List<QuestionResponse> responses = qaService.getQuestions(null);
    return ResponseEntity.ok(responses);
  }
}
