package com.coactivity.auth.adapter.redis.contract;

/**
 * Technical outcome of recording an invalid verification attempt in Redis.
 *
 * <p>This is not a business-level "verification result". It describes what happened to the Redis
 * state (attempt counters, TTL) after an invalid code.</p>
 */
public enum InvalidAttemptOutcome {
    /**
     * The invalid attempt was recorded and the challenge remains active.
     */
    INVALID_CODE,
    /**
     * The challenge is missing or expired (or the remaining TTL became non-positive).
     */
    EXPIRED_OR_MISSING,
    /**
     * The attempt limit has been exceeded and the challenge is removed.
     */
    TOO_MANY_ATTEMPTS
}
