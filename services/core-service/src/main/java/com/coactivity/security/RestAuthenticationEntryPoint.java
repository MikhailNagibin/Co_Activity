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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.coactivity.web.RequestIdFilter;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    ProblemDetail payload = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    payload.setType(URI.create("urn:coactivity:error:AUTH_REQUIRED"));
    payload.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
    payload.setDetail("Authentication is required");
    payload.setInstance(URI.create(request.getRequestURI()));
    payload.setProperty("timestamp", Instant.now());
    payload.setProperty("code", "AUTH_REQUIRED");
    String traceId = resolveTraceId(request);
    payload.setProperty("traceId", traceId);

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
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
