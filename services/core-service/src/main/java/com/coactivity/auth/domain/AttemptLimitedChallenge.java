package com.coactivity.auth.domain;

import java.time.Instant;

/**
 * Contract for short-lived "challenge" objects that are stored with an expiration time and a
 * limited number of attempts.
 *
 * <p>This interface exists to separate responsibilities:</p>
 * <ul>
 *   <li>Infrastructure code can manage persistence concerns (TTL, attempt counters, cleanup).</li>
 *   <li>Domain/application code can decide what "valid code" means.</li>
 * </ul>
 *
 * <p>The self-referential generic ({@code TSelf}) allows infrastructure to create an updated copy
 * via {@link #withAttempts(int)} without losing the concrete type.</p>
 */
public interface AttemptLimitedChallenge<TSelf extends AttemptLimitedChallenge<TSelf>> {

    int attempts();

    int maxAttempts();

    Instant expiresAt();

    TSelf withAttempts(int nextAttempts);
}
