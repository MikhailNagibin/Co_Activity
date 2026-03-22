package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.coactivity.service.exception.QaServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QaGatewayServiceTest {

  private HttpServer httpServer;

  @AfterEach
  void tearDown() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
  }

  @Test
  void getAllQuestionsThrowsServiceUnavailableWhenConnectionFails() {
    QaGatewayService gatewayService = new QaGatewayService("http://127.0.0.1:65535");

    assertThrows(QaServiceUnavailableException.class, gatewayService::getAllQuestions);
  }

  @Test
  void getAllQuestionsTranslatesRemote500ToServiceUnavailable() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    httpServer.createContext("/api/qa/questions", exchange -> {
      byte[] body = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(500, body.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(body);
      }
    });
    httpServer.start();

    QaGatewayService gatewayService = new QaGatewayService(
        "http://127.0.0.1:" + httpServer.getAddress().getPort());

    assertThrows(QaServiceUnavailableException.class, gatewayService::getAllQuestions);
  }
}
