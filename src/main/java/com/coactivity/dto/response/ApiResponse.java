package com.coactivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized response wrapper for all API endpoints.
 * <p>
 * Provides a consistent structure for both successful operations and error conditions. This wrapper
 * separates metadata (success status, messages) from the actual data payload, making client-side
 * handling more predictable and maintainable.
 * </p>
 *
 * @param <T> the type of data payload contained in this response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  /**
   * Indicates whether the API operation completed successfully.
   * <p>
   * {@code true} if the operation succeeded and the data payload is valid, {@code false} if the
   * operation failed due to business logic errors or validation issues. This flag should be checked
   * before attempting to process the data payload.
   * </p>
   */
  private Boolean success;

  /**
   * Human-readable message describing the operation result.
   * <p>
   * For successful operations, this typically provides confirmation or additional context. For
   * failed operations, this describes the error condition in a user-friendly manner. Clients should
   * display this message to end-users when appropriate.
   * </p>
   */
  private String message;

  /**
   * The actual data payload returned by the operation.
   * <p>
   * Contains the business data when {@code success} is {@code true}. Will be {@code null} when
   * {@code success} is {@code false} or when the operation doesn't return any data (e.g., delete
   * operations).
   * </p>
   */
  private T data;

  /**
   * Creates a successful response with data payload.
   *
   * @param <T>  the type of data payload
   * @param data the business data to return to the client
   * @return a successful {@code ApiResponse} instance with the provided data
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, "Operation completed successfully", data);
  }

  /**
   * Creates a successful response with custom message and data.
   *
   * @param <T>     the type of data payload
   * @param message custom success message describing the operation result
   * @param data    the business data to return to the client
   * @return a successful {@code ApiResponse} instance with custom message and data
   */
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data);
  }

  /**
   * Creates an error response with the specified message.
   *
   * @param <T>     the type of data payload (typically {@code Void} for errors)
   * @param message error description explaining what went wrong
   * @return an error {@code ApiResponse} instance with the provided message
   */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, message, null);
  }
}