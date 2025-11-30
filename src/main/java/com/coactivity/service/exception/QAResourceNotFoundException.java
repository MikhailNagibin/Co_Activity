package com.coactivity.service.exception;

/**
 * Raised when a Q&A resource (question/answer) is not found.
 */
public class QAResourceNotFoundException extends ResourceNotFoundException {

  public QAResourceNotFoundException(String message) {
    super(message);
  }
}

