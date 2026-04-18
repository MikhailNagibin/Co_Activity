package com.coactivity.auth.domain;

import java.time.Instant;

/**
 * Password reset verification challenge stored in temporary storage.
 *
 * <p>This is an immutable value object used during password reset flows. It stores a hash of the
 * code (never the plaintext code) and metadata required to enforce expiration and attempt limits.</p>
 */
public record PasswordResetChallenge(
        String codeHash,
        int attempts,
        int maxAttempts,
        Instant expiresAt) implements AttemptLimitedChallenge<PasswordResetChallenge> {

    /**
     * Creates a new instance with an updated attempt counter, keeping all other fields intact.
     */
    public PasswordResetChallenge withAttempts(int nextAttempts) {
        return new PasswordResetChallenge(codeHash, nextAttempts, maxAttempts, expiresAt);
    }
}
