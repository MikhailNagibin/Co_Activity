package com.coactivity.auth.port;

/**
 * Common outcome of verifying a one-time code (registration verification, password reset, etc.).
 *
 * <p>This type belongs to the {@code port} layer: application code consumes this result without
 * knowing which infrastructure (Redis, database, in-memory) is used underneath.</p>
 */
public enum VerificationResult {
    /**
     * The provided code is valid for the current challenge.
     *
     * <p>What happens after a successful verification (e.g. whether the challenge is removed
     * immediately or kept until a later "confirm" step) is a decision of the specific use case.</p>
     */
    SUCCESS,

    /**
     * The provided code does not match the stored code hash.
     */
    INVALID_CODE,

    /**
     * The challenge does not exist anymore or is already expired.
     */
    EXPIRED_OR_MISSING,

    /**
     * Too many verification attempts were made for the challenge.
     */
    TOO_MANY_ATTEMPTS
}
