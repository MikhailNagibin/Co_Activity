package com.coactivity.notifications.service;

public class InvalidEmailCommandException extends RuntimeException {

  public InvalidEmailCommandException(String message) {
    super(message);
  }

  public InvalidEmailCommandException(String message, Throwable cause) {
    super(message, cause);
  }
}
