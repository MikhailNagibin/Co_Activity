package com.coactivity.service.exception;

/**
 * Indicates that a provided authentication token is malformed, expired, or unregistered.
 */
public class TokenValidationException extends DomainException {

  public TokenValidationException(String message) {
    super(message);
  }

  public TokenValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

