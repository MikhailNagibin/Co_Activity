package com.coactivity.service.exception;

import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/**
 * Base class for service-layer domain exceptions that should be surfaced to controllers.
 */
public abstract class DomainException extends RuntimeException {

  /**
   * Stable machine-readable error code.
   */
  private final String code;

  /**
   * HTTP status that should be returned to the client.
   */
  private final HttpStatus httpStatus;

  /**
   * Optional structured errors (mainly for validation).
   */
  private final List<ApiFieldError> errors;

  protected DomainException(HttpStatus httpStatus, String code, String message) {
    this(httpStatus, code, message, null, null);
  }

  protected DomainException(HttpStatus httpStatus, String code, String message, Throwable cause) {
    this(httpStatus, code, message, cause, null);
  }

  protected DomainException(HttpStatus httpStatus, String code, String message, Throwable cause,
      List<ApiFieldError> errors) {
    super(message, cause);
    this.httpStatus = Objects.requireNonNull(httpStatus, "httpStatus");
    this.code = Objects.requireNonNull(code, "code");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    this.errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public List<ApiFieldError> getErrors() {
    return errors;
  }
}
