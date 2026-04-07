package com.coactivity.service.exception;

/**
 * Indicates that a client exceeded an operation-specific request limit.
 */
public class TooManyRequestsException extends DomainException {

  private final String code;

  public TooManyRequestsException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
