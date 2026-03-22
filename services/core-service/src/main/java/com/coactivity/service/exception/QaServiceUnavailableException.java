package com.coactivity.service.exception;

/**
 * Indicates that the external QA service is temporarily unavailable.
 */
public class QaServiceUnavailableException extends DomainException {

  public QaServiceUnavailableException(String message) {
    super(message);
  }

  public QaServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
