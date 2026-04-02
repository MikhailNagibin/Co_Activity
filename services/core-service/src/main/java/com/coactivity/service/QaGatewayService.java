package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.QaServiceUnavailableException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Internal HTTP client for qa-service.
 */
@Service
public class QaGatewayService {

  private static final ParameterizedTypeReference<List<QuestionResponse>> QUESTION_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };
  private static final Duration QA_CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration QA_READ_TIMEOUT = Duration.ofSeconds(3);

  private final RestClient restClient;

  @Autowired
  public QaGatewayService(
      @Value("${qa.service.base-url:http://qa-service:8081}") String qaServiceBaseUrl) {
    this(createRestClient(qaServiceBaseUrl));
  }

  QaGatewayService(RestClient restClient) {
    this.restClient = restClient;
  }

  public QuestionResponse askQuestion(String authorizationHeader, QuestionRequest request) {
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
    } catch (RestClientException ex) {
      throw translateTransportException(ex);
    }
  }

  public AnswerResponse answerQuestion(String authorizationHeader, AnswerRequest request) {
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
    } catch (RestClientException ex) {
      throw translateTransportException(ex);
    }
  }

  public List<QuestionResponse> getQuestions(Integer categoryId) {
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
    } catch (RestClientException ex) {
      throw translateTransportException(ex);
    }
  }

  public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
    try {
      return restClient.get()
          .uri("/api/qa/questions/{questionId}", questionId)
          .retrieve()
          .body(QuestionWithAnswersResponse.class);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    } catch (RestClientException ex) {
      throw translateTransportException(ex);
    }
  }

  public List<QuestionResponse> getAllQuestions() {
    try {
      return restClient.get()
          .uri("/api/qa/questions")
          .retrieve()
          .body(QUESTION_LIST_TYPE);
    } catch (RestClientResponseException ex) {
      throw translateRemoteException(ex);
    } catch (RestClientException ex) {
      throw translateTransportException(ex);
    }
  }

  private RuntimeException translateRemoteException(RestClientResponseException ex) {
    String message = "QA service error (" + ex.getStatusCode().value() + ")";

    return switch (ex.getStatusCode().value()) {
      case 400 -> new ValidationException(message, ex);
      case 401 -> new TokenValidationException(message, ex);
      case 403 -> new AuthorizationException(message);
      case 404 -> new ResourceNotFoundException(message, ex);
      default -> new QaServiceUnavailableException(message, ex);
    };
  }

  private QaServiceUnavailableException translateTransportException(RestClientException ex) {
    return new QaServiceUnavailableException("QA service is unavailable", ex);
  }

  private static RestClient createRestClient(String qaServiceBaseUrl) {
    return RestClient.builder()
        .baseUrl(qaServiceBaseUrl)
        .requestFactory(createRequestFactory())
        .build();
  }

  private static JdkClientHttpRequestFactory createRequestFactory() {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(QA_CONNECT_TIMEOUT)
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(QA_READ_TIMEOUT);
    return requestFactory;
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
}
