package com.coactivity.auth.adapter.redis.contract;

/**
 * Status of loading an active challenge from Redis before verifying a user-provided code.
 */
public enum LoadStatus {
    /**
     * The challenge is present, not expired, and has remaining attempts.
     */
    OK,
    /**
     * The challenge is missing or expired (or was corrupted and therefore removed).
     */
    EXPIRED_OR_MISSING,
    /**
     * The maximum number of attempts has been exceeded.
     */
    TOO_MANY_ATTEMPTS
}
