package com.coactivity.controller.impl;

import com.coactivity.controller.QAController;
import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.service.QAService;
import com.coactivity.service.TokenService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QAControllerImpl implements QAController {

  private final QAService qaService;
  private final TokenService tokenService;

  public QAControllerImpl(QAService qaService, TokenService tokenService) {
    this.qaService = qaService;
    this.tokenService = tokenService;
  }

  @Override
  public ApiResponse<QuestionResponse> askQuestion(String token, QuestionRequest request) {
    return null;
  }

  @Override
  public ApiResponse<AnswerResponse> answerQuestion(String token, AnswerRequest request) {
    return null;
  }

  @Override
  public ApiResponse<List<QuestionResponse>> getQuestions(Integer categoryId) {
    return null;
  }

  @Override
  public ApiResponse<QuestionWithAnswersResponse> getQuestionWithAnswers(Integer questionId) {
    return null;
  }
}
