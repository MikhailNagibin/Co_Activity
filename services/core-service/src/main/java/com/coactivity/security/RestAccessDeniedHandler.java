package com.coactivity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.coactivity.web.RequestIdFilter;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    ProblemDetail payload = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    payload.setType(URI.create("urn:coactivity:error:ACCESS_DENIED"));
    payload.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
    payload.setDetail("Access is denied");
    payload.setInstance(URI.create(request.getRequestURI()));
    payload.setProperty("timestamp", Instant.now());
    payload.setProperty("code", "ACCESS_DENIED");
    String traceId = resolveTraceId(request);
    payload.setProperty("traceId", traceId);

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setHeader(RequestIdFilter.REQUEST_ID_HEADER, traceId);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), payload);
  }

  private String resolveTraceId(HttpServletRequest request) {
    Object traceId = request.getAttribute(RequestIdFilter.TRACE_ID_ATTRIBUTE);
    if (traceId instanceof String value && !value.isBlank()) {
      return value;
    }
    String header = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    if (header != null && !header.isBlank()) {
      return header.trim();
    }
    return UUID.randomUUID().toString();
  }
}
