package com.coactivity.controller.impl;

import com.coactivity.controller.QAController;
import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.service.QAService;
import com.coactivity.service.TokenService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
        String authToken = extractToken(token);
        if (isInvalidToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Integer userId = tokenService.decodeToken(authToken).userId();
            QuestionResponse response = qaService.askQuestion(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PostMapping("/answers")
    public ResponseEntity<AnswerResponse> answerQuestion(
        @RequestHeader(name = "Authorization", required = false) String token,
        @Valid @RequestBody AnswerRequest request) {
        String authToken = extractToken(token);
        if (isInvalidToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Integer userId = tokenService.decodeToken(authToken).userId();
            AnswerResponse response = qaService.answerQuestion(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @GetMapping("/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
        @RequestParam(name = "categoryId", required = false) Integer categoryId) {
        List<QuestionResponse> responses = qaService.getQuestions(categoryId);
        return ResponseEntity.ok(responses);
    }

    @Override
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(
        @PathVariable Integer questionId) {
        try {
            QuestionWithAnswersResponse response = qaService.getQuestionWithAnswers(questionId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isInvalidToken(String token) {
        return token == null || token.isBlank() || !tokenService.isTokenActive(token);
    }

    private String extractToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
    }
}