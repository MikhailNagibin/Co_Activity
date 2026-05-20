package com.coactivity.auth.adapter.redis;

import com.coactivity.auth.adapter.redis.contract.InvalidAttemptOutcome;
import com.coactivity.auth.adapter.redis.contract.LoadResult;
import com.coactivity.auth.adapter.redis.contract.LoadStatus;
import com.coactivity.auth.adapter.redis.support.RedisAttemptLimitedChallengeSupport;
import com.coactivity.auth.adapter.redis.support.RedisCooldownSupport;
import com.coactivity.auth.domain.RegistrationChallenge;
import com.coactivity.auth.port.RegistrationChallengeStore;
import com.coactivity.auth.port.VerificationResult;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Redis adapter implementing {@link RegistrationChallengeStore}.
 *
 * <p>This adapter stores registration challenges as JSON in Redis and uses:</p>
 * <ul>
 *   <li>{@link AuthRedisKeys} to build stable, non-PII Redis keys,</li>
 *   <li>{@link RedisAttemptLimitedChallengeSupport} to manage TTL/attempt limits,</li>
 *   <li>{@link RedisCooldownSupport} to implement resend cooldown windows,</li>
 *   <li>{@link PasswordEncoder} to store only a hash of the code.</li>
 * </ul>
 */
@Component
public class RedisChallengeStore implements RegistrationChallengeStore {

    private final PasswordEncoder passwordEncoder;
    private final RedisAttemptLimitedChallengeSupport challengeSupport;
    private final RedisCooldownSupport cooldownSupport;
    private final AuthRedisKeys authRedisKeys;
    private final Duration ttl;
    private final int maxAttempts;
    private final Duration resendCooldown;

    public RedisChallengeStore(PasswordEncoder passwordEncoder,
            RedisAttemptLimitedChallengeSupport challengeSupport,
            RedisCooldownSupport cooldownSupport,
            AuthRedisKeys authRedisKeys,
            @Value("${app.auth.challenge.ttl:10m}") Duration ttl,
            @Value("${app.auth.challenge.max-attempts:5}") int maxAttempts,
            @Value("${app.auth.challenge.resend-cooldown:60s}") Duration resendCooldown) {
        this.passwordEncoder = passwordEncoder;
        this.challengeSupport = challengeSupport;
        this.cooldownSupport = cooldownSupport;
        this.authRedisKeys = authRedisKeys;
        this.ttl = ttl;
        this.maxAttempts = maxAttempts;
        this.resendCooldown = resendCooldown;
    }

    public void create(String emailNormalized, Integer userId, String rawCode) {
        RegistrationChallenge challenge = new RegistrationChallenge(
                passwordEncoder.encode(rawCode),
                0,
                maxAttempts,
                Instant.now().plus(ttl));
        challengeSupport.setJson(authRedisKeys.registrationChallengeKey(emailNormalized), challenge,
                ttl);
    }

    public VerificationResult verify(String emailNormalized, String code) {
        String challengeKey = authRedisKeys.registrationChallengeKey(emailNormalized);
        LoadResult<RegistrationChallenge> loaded = challengeSupport.loadActive(
                challengeKey, RegistrationChallenge.class);

        if (loaded.status() == LoadStatus.EXPIRED_OR_MISSING) {
            return VerificationResult.EXPIRED_OR_MISSING;
        }
        if (loaded.status() == LoadStatus.TOO_MANY_ATTEMPTS) {
            return VerificationResult.TOO_MANY_ATTEMPTS;
        }

        RegistrationChallenge challenge = loaded.challenge();
        if (!passwordEncoder.matches(code, challenge.codeHash())) {
            InvalidAttemptOutcome outcome = challengeSupport.recordInvalidAttempt(challengeKey,
                    challenge);
            return VerificationResultMapper.fromInvalidAttemptOutcome(outcome);
        }

        challengeSupport.delete(challengeKey);
        return VerificationResult.SUCCESS;
    }

    public void delete(String emailNormalized) {
        challengeSupport.delete(authRedisKeys.registrationChallengeKey(emailNormalized));
    }

    public void markResendCooldown(String emailNormalized) {
        cooldownSupport.mark(authRedisKeys.registrationResendCooldownKey(emailNormalized),
                resendCooldown);
    }

    public boolean tryActivateResendCooldown(String emailNormalized) {
        return cooldownSupport.tryActivate(
                authRedisKeys.registrationResendCooldownKey(emailNormalized),
                resendCooldown);
    }

    public void clearResendCooldown(String emailNormalized) {
        cooldownSupport.clear(authRedisKeys.registrationResendCooldownKey(emailNormalized));
    }
}
