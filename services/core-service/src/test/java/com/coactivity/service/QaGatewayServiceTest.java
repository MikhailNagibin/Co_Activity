package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.coactivity.service.exception.QaServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class QaGatewayServiceTest {

  @Test
  void getAllQuestionsThrowsServiceUnavailableWhenConnectionFails() {
    QaGatewayService gatewayService = new QaGatewayService("http://127.0.0.1:65535");

    assertThrows(QaServiceUnavailableException.class, gatewayService::getAllQuestions);
  }

  @Test
  void getAllQuestionsTranslatesRemote500ToServiceUnavailable() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://qa-service");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://qa-service/api/qa/questions"))
        .andExpect(request -> GET.equals(request.getMethod()))
        .andRespond(withServerError());

    QaGatewayService gatewayService = new QaGatewayService(builder.build());

    assertThrows(QaServiceUnavailableException.class, gatewayService::getAllQuestions);
    server.verify();
  }
}
