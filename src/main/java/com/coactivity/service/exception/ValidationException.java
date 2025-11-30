package com.coactivity.service.exception;

/**
 * Indicates that the client supplied data that violates business validation rules.
 */
public class ValidationException extends DomainException {

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
