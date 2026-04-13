package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a client exceeded an operation-specific request limit.
 */
public class TooManyRequestsException extends DomainException {

  public TooManyRequestsException(String code, String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, code, message);
  }
}
