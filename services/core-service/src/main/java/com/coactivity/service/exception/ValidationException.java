package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the client supplied data that violates business validation rules.
 */
public class ValidationException extends DomainException {

  public ValidationException(String message) {
    super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
  }

  public ValidationException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, cause);
  }
}
