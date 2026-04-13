package com.coactivity.controller.advice;

import com.coactivity.service.exception.ApiFieldError;
import com.coactivity.service.exception.DomainException;
import com.coactivity.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
    return buildProblemDetail(
        request,
        ex.getHttpStatus(),
        ex.getCode(),
        ex.getMessage(),
        ex.getErrors());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage(), error.getCode()))
        .toList();
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", errors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {
    List<ApiFieldError> errors = ex.getConstraintViolations().stream()
        .map(violation -> new ApiFieldError(
            violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString(),
            violation.getMessage(),
            violation.getConstraintDescriptor() == null
                ? null
                : violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()))
        .toList();
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", errors);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {
    String detail = "invalid value";
    if (ex.getValue() != null) {
      detail = detail + " '" + ex.getValue() + "'";
    }
    ApiFieldError error = new ApiFieldError(ex.getName(), detail, "TYPE_MISMATCH");
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", List.of(error));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpServletRequest request) {
    ApiFieldError error = new ApiFieldError(null,
        "Request body contains invalid or malformed value", "MALFORMED_JSON");
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", List.of(error));
  }

  @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
  public ResponseEntity<ProblemDetail> handleMultipartErrors(Exception ex,
      HttpServletRequest request) {
    ApiFieldError error = new ApiFieldError(null,
        "Uploaded file exceeds the allowed size or is malformed", "MULTIPART_INVALID");
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", List.of(error));
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ProblemDetail> handleMissingMultipartPart(
      MissingServletRequestPartException ex,
      HttpServletRequest request) {
    ApiFieldError error = new ApiFieldError(ex.getRequestPartName(),
        "required multipart part is missing", "MISSING_PART");
    return buildProblemDetail(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
        "Validation failed", List.of(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex,
      HttpServletRequest request) {
    log.error("Unexpected error while processing {} {}", request.getMethod(),
        request.getRequestURI(), ex);
    return buildProblemDetail(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
        "Internal server error", List.of());
  }

  private ResponseEntity<ProblemDetail> buildProblemDetail(HttpServletRequest request,
      HttpStatus status,
      String code,
      String detail,
      List<ApiFieldError> errors) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setType(URI.create("urn:coactivity:error:" + code));
    problemDetail.setStatus(status.value());
    problemDetail.setTitle(resolveTitle(code, status));
    problemDetail.setDetail(detail);
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    String traceId = resolveTraceId(request);
    problemDetail.setProperty("timestamp", Instant.now());
    problemDetail.setProperty("code", code);
    problemDetail.setProperty("traceId", traceId);
    if (errors != null && !errors.isEmpty()) {
      problemDetail.setProperty("errors", List.copyOf(errors));
    }

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .header(RequestIdFilter.REQUEST_ID_HEADER, traceId)
        .body(problemDetail);
  }

  private String resolveTitle(String code, HttpStatus status) {
    if ("VALIDATION_FAILED".equals(code)) {
      return "Validation failed";
    }
    return status.getReasonPhrase();
  }

  private String resolveTraceId(HttpServletRequest request) {
    Object traceId = request.getAttribute(RequestIdFilter.TRACE_ID_ATTRIBUTE);
    if (traceId instanceof String value && !value.isBlank()) {
      return value;
    }
    String header = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    if (header != null && !header.isBlank()) {
      return header.trim();
    }
    return UUID.randomUUID().toString();
  }
}
