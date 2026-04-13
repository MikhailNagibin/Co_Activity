package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates an unexpected internal condition that cannot be mapped to a specific client error.
 */
public class InternalException extends DomainException {

  public InternalException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
  }

  public InternalException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, cause);
  }
}
