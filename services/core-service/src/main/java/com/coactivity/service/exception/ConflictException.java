package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the requested operation conflicts with the current persisted state.
 */
public class ConflictException extends DomainException {

  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }

  public ConflictException(String code, String message, Throwable cause) {
    super(HttpStatus.CONFLICT, code, message, cause);
  }
}
