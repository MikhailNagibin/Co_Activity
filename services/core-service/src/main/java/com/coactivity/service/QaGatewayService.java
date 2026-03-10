package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

/**
 * Routes Q&A requests either to local monolith service or external qa-service.
 */
@Service
public class QaGatewayService {

  private static final ParameterizedTypeReference<List<QuestionResponse>> QUESTION_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final QAService localQaService;
  private final RestClient restClient;
  private final QaMode mode;

  public QaGatewayService(QAService localQaService,
      @Value("${qa.mode:MONOLITH}") String mode,
      @Value("${qa.service.base-url:http://localhost:8081}") String qaServiceBaseUrl) {
    this.localQaService = localQaService;
    this.restClient = RestClient.builder().baseUrl(qaServiceBaseUrl).build();
    this.mode = QaMode.from(mode);
  }

  public QuestionResponse askQuestion(String authorizationHeader, Integer userId,
      QuestionRequest request) {
    if (mode == QaMode.MONOLITH) {
      return localQaService.askQuestion(userId, request);
    }

    try {
      return restClient.post()
          .uri("/api/qa/questions")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", normalizeAuthorizationHeader(authorizationHeader))
          .body(request)
          .retrieve()
          .body(QuestionResponse.class);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    }
  }

  public AnswerResponse answerQuestion(String authorizationHeader, Integer userId,
      AnswerRequest request) {
    if (mode == QaMode.MONOLITH) {
      return localQaService.answerQuestion(userId, request);
    }

    try {
      return restClient.post()
          .uri("/api/qa/answers")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", normalizeAuthorizationHeader(authorizationHeader))
          .body(request)
          .retrieve()
          .body(AnswerResponse.class);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    }
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
    if (mode == QaMode.MONOLITH) {
      return localQaService.getQuestions(categoryId);
    }

    try {
      if (categoryId == null) {
        return getAllQuestions();
      }

      return restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/api/qa/questions/category")
              .queryParam("categoryId", categoryId)
              .build())
          .retrieve()
          .body(QUESTION_LIST_TYPE);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    }
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    if (mode == QaMode.MONOLITH) {
      return localQaService.getQuestionWithAnswers(questionId);
    }

    try {
      return restClient.get()
          .uri("/api/qa/questions/{questionId}", questionId)
          .retrieve()
          .body(QuestionWithAnswersResponse.class);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    }
  }

  public List<QuestionResponse> getAllQuestions() {
    if (mode == QaMode.MONOLITH) {
      return localQaService.getQuestions(null);
    }

    try {
      return restClient.get()
          .uri("/api/qa/questions")
          .retrieve()
          .body(QUESTION_LIST_TYPE);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    }
  }

  private RuntimeException translateRemoteException(RestClientResponseException ex) {
    String message = "QA service error (" + ex.getStatusCode().value() + ")";

    return switch (ex.getStatusCode().value()) {
      case 400 -> new ValidationException(message, ex);
      case 401 -> new TokenValidationException(message, ex);
      case 403 -> new AuthorizationException(message);
      case 404 -> new ResourceNotFoundException(message, ex);
      default -> new ValidationException(message, ex);
    };
  }

  private String normalizeAuthorizationHeader(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new TokenValidationException("Authorization token is required");
    }

    if (authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader;
    }
    return "Bearer " + authorizationHeader;
  }

  private enum QaMode {
    MONOLITH,
    SERVICE;

    static QaMode from(String rawValue) {
      if (rawValue == null || rawValue.isBlank()) {
        return MONOLITH;
      }
      try {
        return QaMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        return MONOLITH;
      }
    }
  }
}
