package com.coactivity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for initiating user authentication.
 * <p>
 * Contains credentials for verifying user identity and initiating the two-factor authentication
 * process with email verification codes.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

  /**
   * User's registered email address used as login identifier.
   * <p>
   * Must match a valid, active user account in the system. Case-insensitive matching is applied
   * during verification.
   * </p>
   */
  private String login;

  /**
   * User's account password for initial credential verification.
   * <p>
   * Must match the stored password hash for the specified email address. Passwords are
   * case-sensitive and verified using secure hashing algorithms.
   * </p>
   */
  private String password;
}