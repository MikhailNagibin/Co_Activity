package com.coactivity.auth.port;

/**
 * Port for storing and verifying registration email challenges.
 *
 * <p>This interface defines what the application layer needs from a "challenge store" without
 * coupling to a specific storage technology (Redis, database, in-memory implementation, etc.).</p>
 */
public interface RegistrationChallengeStore {

  /**
   * Creates or overwrites the current registration challenge for the given user/email.
   *
   * <p>The implementation is expected to store a non-plaintext representation of the code (e.g. a
   * password hash) and enforce an expiration policy.</p>
   */
  void create(String emailNormalized, Integer userId, String rawCode);

  /**
   * Verifies a registration code against the active challenge.
   */
  VerificationResult verify(String emailNormalized, String code);

  /**
   * Deletes the active registration challenge, if any.
   */
  void delete(String emailNormalized);

  /**
   * Forces a resend cooldown window to start (regardless of any existing cooldown).
   */
  void markResendCooldown(String emailNormalized);

  /**
   * Atomically starts the resend cooldown window.
   *
   * @return {@code true} if the cooldown was started now; {@code false} if a cooldown is already
   * active.
   */
  boolean tryActivateResendCooldown(String emailNormalized);

  /**
   * Clears the resend cooldown flag.
   */
  void clearResendCooldown(String emailNormalized);
}
