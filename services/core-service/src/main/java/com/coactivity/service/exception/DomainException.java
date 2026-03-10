package com.coactivity.service.exception;

/**
 * Base class for service-layer domain exceptions that should be surfaced to controllers.
 */
public abstract class DomainException extends RuntimeException {

  /**
   * Creates a new domain exception with the provided message.
   *
   * @param message human-readable error description
   */
  protected DomainException(String message) {
    super(message);
  }

  /**
   * Creates a new domain exception with the provided message and cause.
   *
   * @param message human-readable error description
   * @param cause   underlying cause of the exception
   */
  protected DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
