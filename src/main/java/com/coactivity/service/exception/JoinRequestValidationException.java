package com.coactivity.service.exception;

/**
 * Signals invalid operations on join requests.
 */
public class JoinRequestValidationException extends ValidationException {

  public JoinRequestValidationException(String message) {
    super(message);
  }
}

