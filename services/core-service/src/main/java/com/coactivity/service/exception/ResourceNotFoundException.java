package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for missing domain entities (room, user, question, etc.).
 */
public class ResourceNotFoundException extends DomainException {

  public ResourceNotFoundException(String code, String message) {
    super(HttpStatus.NOT_FOUND, code, message);
  }

  public ResourceNotFoundException(String code, String message, Throwable cause) {
    super(HttpStatus.NOT_FOUND, code, message, cause);
  }
}
