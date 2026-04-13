package com.coactivity.service.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends DomainException {

  public StorageException(String message) {
    super(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", message);
  }

  public StorageException(String message, Throwable cause) {
    super(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", message, cause);
  }
}
