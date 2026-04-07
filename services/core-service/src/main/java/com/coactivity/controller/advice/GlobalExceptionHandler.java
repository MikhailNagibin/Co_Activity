package com.coactivity.controller.advice;

import com.coactivity.controller.dto.response.ApiErrorResponse;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.ResourceNotFoundException;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * Translates domain and validation exceptions into consistent HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), null, HttpStatus.NOT_FOUND, request, null);
  }

  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthorization(AuthorizationException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), null, HttpStatus.FORBIDDEN, request, null);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), null, HttpStatus.BAD_REQUEST, request, null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), ex.getCode(), HttpStatus.CONFLICT, request, null);
  }

  @ExceptionHandler(NotificationDeliveryException.class)
  public ResponseEntity<ApiErrorResponse> handleNotificationDelivery(
      NotificationDeliveryException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE, request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.toList());
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList());
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request, details);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {
    String detail = ex.getName() + ": invalid value";
    if (ex.getValue() != null) {
      detail = detail + " '" + ex.getValue() + "'";
    }
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request,
        List.of(detail));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpServletRequest request) {
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request,
        List.of("Request body contains invalid or malformed value"));
  }

  @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
  public ResponseEntity<ApiErrorResponse> handleMultipartErrors(Exception ex,
      HttpServletRequest request) {
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request,
        List.of("Uploaded file exceeds the allowed size or is malformed"));
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingMultipartPart(
      MissingServletRequestPartException ex,
      HttpServletRequest request) {
    return buildResponse("Validation failed", null, HttpStatus.BAD_REQUEST, request,
        List.of(ex.getRequestPartName() + ": required multipart part is missing"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
      HttpServletRequest request) {
    log.error("Unexpected error while processing {} {}", request.getMethod(),
        request.getRequestURI(), ex);
    return buildResponse("Internal server error", null, HttpStatus.INTERNAL_SERVER_ERROR, request,
        null);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(String message, String code,
      HttpStatus status,
      HttpServletRequest request, List<String> details) {
    ApiErrorResponse body = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        code,
        request.getRequestURI(),
        details);
    return ResponseEntity.status(status).body(body);
  }
}
