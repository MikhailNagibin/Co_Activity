package com.coactivity.service.exception;

/**
 * Base exception for missing domain entities (room, user, question, etc.).
 */
public class ResourceNotFoundException extends DomainException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}

