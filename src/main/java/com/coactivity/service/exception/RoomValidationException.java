package com.coactivity.service.exception;

/**
 * Raised when room-related input fails validation.
 */
public class RoomValidationException extends ValidationException {

  public RoomValidationException(String message) {
    super(message);
  }
}

