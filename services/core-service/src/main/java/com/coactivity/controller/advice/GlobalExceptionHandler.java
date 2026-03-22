package com.coactivity.controller.advice;

import com.coactivity.controller.dto.response.ApiErrorResponse;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.QaServiceUnavailableException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.TokenValidationException;
import com.coactivity.service.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and validation exceptions into consistent HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
  }

  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthorization(AuthorizationException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN, request, null);
  }

  @ExceptionHandler(TokenValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleToken(TokenValidationException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED, request, null);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request, null);
  }

  @ExceptionHandler(NotificationDeliveryException.class)
  public ResponseEntity<ApiErrorResponse> handleNotificationDelivery(
      NotificationDeliveryException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request, null);
  }

  @ExceptionHandler(QaServiceUnavailableException.class)
  public ResponseEntity<ApiErrorResponse> handleQaServiceUnavailable(
      QaServiceUnavailableException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.toList());
    return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, request, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList());
    return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, request, details);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
      HttpServletRequest request) {
    log.error("Unexpected error while processing {} {}", request.getMethod(),
        request.getRequestURI(), ex);
    return buildResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR, request,
        null);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(String message, HttpStatus status,
      HttpServletRequest request, List<String> details) {
    ApiErrorResponse body = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI(),
        details);
    return ResponseEntity.status(status).body(body);
  }
}
