package com.coactivity.qa.advice;

import com.coactivity.qa.dto.response.ApiErrorResponse;
import com.coactivity.qa.exception.ResourceNotFoundException;
import com.coactivity.qa.exception.TokenValidationException;
import com.coactivity.qa.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
      HttpServletRequest request) {
    return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
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
