package com.coactivity.auth.domain;

/**
 * User account lifecycle status used by authentication and security components.
 */
public enum UserStatus {
  /**
   * The user has registered but has not yet verified their email address.
   */
  PENDING_VERIFICATION,
  /**
   * The user account is active and can authenticate.
   */
  ACTIVE,
  /**
   * The user account is disabled and cannot authenticate.
   */
  DISABLED
}
