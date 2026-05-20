package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the current user lacks permissions to perform an action.
 */
public class AuthorizationException extends DomainException {

  public AuthorizationException(String message) {
    super(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message);
  }

  public AuthorizationException(String code, String message) {
    super(HttpStatus.FORBIDDEN, code, message);
  }

  public AuthorizationException(String code, String message, Throwable cause) {
    super(HttpStatus.FORBIDDEN, code, message, cause);
  }
}
