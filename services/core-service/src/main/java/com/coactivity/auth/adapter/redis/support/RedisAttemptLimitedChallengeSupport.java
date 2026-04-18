package com.coactivity.auth.adapter.redis.support;

import com.coactivity.auth.domain.AttemptLimitedChallenge;
import com.coactivity.auth.adapter.redis.contract.InvalidAttemptOutcome;
import com.coactivity.auth.adapter.redis.contract.LoadResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis helper for storing and managing "attempt-limited" challenges as JSON values.
 *
 * <p>Responsibilities of this component:</p>
 * <ul>
 *   <li>Serialize/deserialize challenge objects to/from JSON.</li>
 *   <li>Enforce expiration via {@link AttemptLimitedChallenge#expiresAt()} and Redis TTL.</li>
 *   <li>Enforce maximum attempts and remove exhausted challenges.</li>
 *   <li>Record an invalid attempt without extending the original lifetime (uses remaining TTL).</li>
 * </ul>
 *
 * <p>Non-responsibilities:</p>
 * <ul>
 *   <li>It does <strong>not</strong> decide whether a user-provided code is correct.</li>
 *   <li>It does <strong>not</strong> implement business rules beyond attempts/expiration.</li>
 * </ul>
 *
 * <p>Corrupted/unreadable JSON is treated as missing data and the key is deleted as a safe
 * cleanup strategy.</p>
 */
@Component
public class RedisAttemptLimitedChallengeSupport {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RedisAttemptLimitedChallengeSupport(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T extends AttemptLimitedChallenge<T>> void setJson(String key, T value, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize challenge (" + value.getClass().getSimpleName() + ")", ex);
        }
    }

    public <T extends AttemptLimitedChallenge<T>> LoadResult<T> loadActive(String key,
            Class<T> type) {
        String rawValue = redisTemplate.opsForValue().get(key);
        if (rawValue == null || rawValue.isBlank()) {
            return LoadResult.expiredOrMissing();
        }

        final T challenge;
        try {
            challenge = objectMapper.readValue(rawValue, type);
        } catch (JsonProcessingException ex) {
            delete(key);
            return LoadResult.expiredOrMissing();
        }

        Instant now = clock.instant();
        if (challenge.expiresAt().isBefore(now)) {
            delete(key);
            return LoadResult.expiredOrMissing();
        }
        if (challenge.attempts() >= challenge.maxAttempts()) {
            delete(key);
            return LoadResult.tooManyAttempts();
        }
        return LoadResult.ok(challenge);
    }

    public <T extends AttemptLimitedChallenge<T>> InvalidAttemptOutcome recordInvalidAttempt(
            String key,
            T challenge
    ) {
        int nextAttempts = challenge.attempts() + 1;
        if (nextAttempts >= challenge.maxAttempts()) {
            delete(key);
            return InvalidAttemptOutcome.TOO_MANY_ATTEMPTS;
        }

        Duration remainingTtl = Duration.between(clock.instant(), challenge.expiresAt());
        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            delete(key);
            return InvalidAttemptOutcome.EXPIRED_OR_MISSING;
        }

        setJson(key, challenge.withAttempts(nextAttempts), remainingTtl);
        return InvalidAttemptOutcome.INVALID_CODE;
    }
}
