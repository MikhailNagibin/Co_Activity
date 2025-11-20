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
        if (!tokenService.isTokenActive(token)) {
            return ApiResponse.error("401");
        }
        try {
            Integer userId = tokenService.decodeToken(token).userId();
            QuestionResponse response = qaService.askQuestion(userId, request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("400");
        } catch (Exception e) {
            return ApiResponse.error("500");
        }
    }

    @Override
    public ApiResponse<AnswerResponse> answerQuestion(String token, AnswerRequest request) {
        if (!tokenService.isTokenActive(token)) {
            return ApiResponse.error("401");
        }
        try {
            Integer userId = tokenService.decodeToken(token).userId();
            AnswerResponse response = qaService.answerQuestion(userId, request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("400");
        } catch (Exception e) {
            return ApiResponse.error("500");
        }
    }

    @Override
    public ApiResponse<List<QuestionResponse>> getQuestions(Integer categoryId) {
        // This endpoint is public, no token validation needed
        try {
            List<QuestionResponse> responses = qaService.getQuestions(categoryId);
            return ApiResponse.success(responses);
        } catch (Exception e) {
            return ApiResponse.error("500");
        }
    }

    @Override
    public ApiResponse<QuestionWithAnswersResponse> getQuestionWithAnswers(Integer questionId) {
        // This endpoint is public, no token validation needed
        try {
            QuestionWithAnswersResponse response = qaService.getQuestionWithAnswers(questionId);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("404");
        } catch (Exception e) {
            return ApiResponse.error("500");
        }
    }
}