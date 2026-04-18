package com.coactivity.auth.domain;

import java.time.Instant;

/**
 * Registration email verification challenge stored in temporary storage.
 *
 * <p>This is an immutable value object. It intentionally stores only a hash of the code and
 * bookkeeping fields (attempts, expiration) so that plaintext codes and personal data do not need
 * to be persisted in Redis.</p>
 */
public record RegistrationChallenge(
        String codeHash,
        int attempts,
        int maxAttempts,
        Instant expiresAt) implements AttemptLimitedChallenge<RegistrationChallenge> {

    /**
     * Creates a new instance with an updated attempt counter, keeping all other fields intact.
     */
    public RegistrationChallenge withAttempts(int nextAttempts) {
        return new RegistrationChallenge(codeHash, nextAttempts, maxAttempts, expiresAt);
    }
}
