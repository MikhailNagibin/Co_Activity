package com.coactivity.controller.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Standard error payload returned by REST controllers via {@code @ControllerAdvice}.
 *
 * @param timestamp time when the error was generated
 * @param status    HTTP status code
 * @param error     reason phrase or short error code
 * @param message   detailed human-readable explanation
 * @param path      request path that triggered the error
 * @param details   optional list of field/validation details
 */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details) {

  /**
   * Factory method ensuring details list is never {@code null}.
   */
  public ApiErrorResponse {
    details = details == null ? List.of() : List.copyOf(details);
  }
}

