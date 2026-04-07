package com.coactivity.service.exception;

/**
 * Thrown when the current user lacks permissions to perform an action.
 */
public class AuthorizationException extends DomainException {

  private final String code;

  public AuthorizationException(String message) {
    super(message);
    this.code = null;
  }

  public AuthorizationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public AuthorizationException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
