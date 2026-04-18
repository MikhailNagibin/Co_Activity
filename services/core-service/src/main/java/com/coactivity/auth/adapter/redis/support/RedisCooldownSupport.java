package com.coactivity.auth.adapter.redis.support;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis helper for implementing simple cooldown / rate-limit flags.
 *
 * <p>The model is intentionally minimal:</p>
 * <ul>
 *   <li>Key exists =&gt; cooldown is active.</li>
 *   <li>Key TTL defines the cooldown duration.</li>
 * </ul>
 *
 * <p>The stored value is a constant marker (currently {@code "1"}) and has no business meaning.
 * Only the existence of the key and its TTL are used.</p>
 */
@Component
public class RedisCooldownSupport {

    private static final String COOLDOWN_MARKER_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    public RedisCooldownSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Forces the cooldown to be active by setting the key with a TTL (overwrites the current TTL).
     */
    public void mark(String key, Duration cooldown) {
        redisTemplate.opsForValue().set(key, COOLDOWN_MARKER_VALUE, cooldown);
    }

    /**
     * Atomically activates a cooldown window using SET NX with a TTL.
     *
     * @return {@code true} if the key was created now; {@code false} if it already existed.
     */
    public boolean tryActivate(String key, Duration cooldown) {
        Boolean activated = redisTemplate.opsForValue()
                .setIfAbsent(key, COOLDOWN_MARKER_VALUE, cooldown);
        return Boolean.TRUE.equals(activated);
    }

    /**
     * Clears the cooldown by deleting the key.
     */
    public void clear(String key) {
        redisTemplate.delete(key);
    }
}
