package com.coactivity.service.exception;

/**
 * Indicates that the system could not dispatch a required notification for the current operation.
 */
public class NotificationDeliveryException extends DomainException {

  public NotificationDeliveryException(String message) {
    super(message);
  }

  public NotificationDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
