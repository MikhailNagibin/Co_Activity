package com.coactivity.service.exception;

/**
 * Thrown when the current user lacks permissions to perform an action.
 */
public class AuthorizationException extends DomainException {

  public AuthorizationException(String message) {
    super(message);
  }
}
