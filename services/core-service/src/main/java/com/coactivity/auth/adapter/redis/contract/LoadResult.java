package com.coactivity.auth.adapter.redis.contract;

import java.util.Objects;

/**
 * Contract result of loading a challenge from Redis.
 *
 * @param status status of the load operation
 * @param challenge loaded challenge (present only when {@link LoadStatus#OK})
 */
public record LoadResult<T>(LoadStatus status, T challenge) {

    public static <T> LoadResult<T> ok(T challenge) {
        return new LoadResult<>(LoadStatus.OK, Objects.requireNonNull(challenge));
    }

    public static <T> LoadResult<T> expiredOrMissing() {
        return new LoadResult<>(LoadStatus.EXPIRED_OR_MISSING, null);
    }

    public static <T> LoadResult<T> tooManyAttempts() {
        return new LoadResult<>(LoadStatus.TOO_MANY_ATTEMPTS, null);
    }
}
