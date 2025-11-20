package com.coactivity.controller.dto.response;

import com.coactivity.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned after a login attempt is verified with the email code.
 * <p>
 * Provides the authentication token that represents the authenticated session together with minimal
 * user metadata needed by the client. Tokens are base64-encoded strings containing the user's id
 * and expiration timestamp. Tokens expire 30 minutes after creation and must be echoed back in
 * every protected request for authentication.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  /**
   * Encoded token issued after successful verification.
   * <p>
   * The token encapsulates the user's id and expiration timestamp (30 minutes from creation). It
   * should be supplied by the client when calling any controller method that requires
   * authentication. The token automatically expires after 30 minutes, requiring the user to log in
   * again.
   * </p>
   */
  private String token;

  /**
   * Unique identifier of the authenticated user.
   * <p>
   * Mirrors the {@code id} field in the {@link User} domain model so the client can keep track of
   * which user is logged in.
   * </p>
   */
  private Integer userId;

  /**
   * Public display name of the authenticated user.
   * <p>
   * This is the username chosen during registration and can be displayed throughout the application
   * to identify the current user.
   * </p>
   */
  private String username;
}

