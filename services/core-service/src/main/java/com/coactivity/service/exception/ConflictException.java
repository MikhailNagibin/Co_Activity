package com.coactivity.service.exception;

/**
 * Indicates that the requested operation conflicts with the current persisted state.
 */
public class ConflictException extends DomainException {

  private final String code;

  public ConflictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public ConflictException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
