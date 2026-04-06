package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.QaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/qa")
public class QAControllerImpl {

  private final QaService qaService;

  public QAControllerImpl(QaService qaService) {
    this.qaService = qaService;
  }

  @PostMapping("/questions")
  public ResponseEntity<QuestionResponse> askQuestion(
    @AuthenticationPrincipal CurrentUserPrincipal currentUser,
    @Valid @RequestBody QuestionRequest request) {
    QuestionResponse response = qaService.askQuestion(currentUser.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/answers")
  public ResponseEntity<AnswerResponse> answerQuestion(
    @AuthenticationPrincipal CurrentUserPrincipal currentUser,
    @Valid @RequestBody AnswerRequest request) {
    AnswerResponse response = qaService.answerQuestion(currentUser.getUserId(), request);
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
      @Positive @PathVariable Integer questionId) {
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
}
