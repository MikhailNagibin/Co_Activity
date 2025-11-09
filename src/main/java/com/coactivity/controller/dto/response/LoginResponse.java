package com.coactivity.controller.dto.response;

import com.coactivity.domain.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Response payload returned after successful user authentication.
 * <p>
 * Contains the JWT token and session information needed for subsequent API calls.
 * The token is valid for 30 minutes as specified in the functional requirements.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  /**
   * JSON Web Token for authenticating subsequent API requests.
   * <p>
   * This token must be included in the {@code Authorization} header of all
   * authenticated requests. The token contains encrypted user identity information
   * and expires after 30 minutes of inactivity.
   * </p>
   */
  private String token;

  /**
   * Unique identifier of the authenticated user.
   * <p>
   * This ID should be used by the client when displaying user-specific content
   * or making user-related API calls. It corresponds to the {@code id} field
   * in the {@link User} domain model.
   * </p>
   */
  private Integer userId;

  /**
   * Public display name of the authenticated user.
   * <p>
   * This is the username chosen during registration and can be displayed
   * throughout the application to identify the current user.
   * </p>
   */
  private String username;

  /**
   * Timestamp indicating when the JWT token becomes invalid.
   * <p>
   * Clients should use this information to implement proactive token refresh
   * or to warn users about impending session expiration.
   * </p>
   */
  private Instant expiresAt;
}

