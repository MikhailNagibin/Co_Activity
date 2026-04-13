package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the system could not dispatch a required notification for the current operation.
 */
public class NotificationDeliveryException extends DomainException {

  public NotificationDeliveryException(String message) {
    super(HttpStatus.SERVICE_UNAVAILABLE, "NOTIFICATION_DELIVERY_FAILED", message);
  }

  public NotificationDeliveryException(String message, Throwable cause) {
    super(HttpStatus.SERVICE_UNAVAILABLE, "NOTIFICATION_DELIVERY_FAILED", message, cause);
  }
}
