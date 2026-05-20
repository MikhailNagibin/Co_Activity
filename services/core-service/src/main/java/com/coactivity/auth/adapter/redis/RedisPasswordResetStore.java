package com.coactivity.auth.adapter.redis;

import com.coactivity.auth.domain.PasswordResetChallenge;
import com.coactivity.auth.port.PasswordResetStore;
import com.coactivity.auth.port.VerificationResult;
import com.coactivity.auth.adapter.redis.support.RedisAttemptLimitedChallengeSupport;
import com.coactivity.auth.adapter.redis.support.RedisCooldownSupport;
import com.coactivity.auth.adapter.redis.contract.InvalidAttemptOutcome;
import com.coactivity.auth.adapter.redis.contract.LoadResult;
import com.coactivity.auth.adapter.redis.contract.LoadStatus;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Redis adapter implementing {@link PasswordResetStore}.
 *
 * <p>This adapter persists password reset challenges as JSON and supports a request cooldown to
 * reduce abuse (e.g. repeated email sends). It stores only a hash of the code and uses Redis TTL to
 * enforce expiration.</p>
 */
@Component
public class RedisPasswordResetStore implements PasswordResetStore {

  private final PasswordEncoder passwordEncoder;
  private final RedisAttemptLimitedChallengeSupport challengeSupport;
  private final RedisCooldownSupport cooldownSupport;
  private final AuthRedisKeys authRedisKeys;
  private final Duration ttl;
  private final int maxAttempts;
  private final Duration requestCooldown;

  public RedisPasswordResetStore(PasswordEncoder passwordEncoder,
      RedisAttemptLimitedChallengeSupport challengeSupport, RedisCooldownSupport cooldownSupport,
      AuthRedisKeys authRedisKeys,
      @Value("${app.auth.password-reset.ttl:10m}") Duration ttl,
      @Value("${app.auth.password-reset.max-attempts:5}") int maxAttempts,
      @Value("${app.auth.password-reset.request-cooldown:60s}") Duration requestCooldown) {
    this.passwordEncoder = passwordEncoder;
    this.challengeSupport = challengeSupport;
    this.cooldownSupport = cooldownSupport;
    this.authRedisKeys = authRedisKeys;
    this.ttl = ttl;
    this.maxAttempts = maxAttempts;
    this.requestCooldown = requestCooldown;
  }

  public void create(String emailNormalized, Integer userId, String rawCode) {
    PasswordResetChallenge challenge = new PasswordResetChallenge(
        passwordEncoder.encode(rawCode),
        0,
        maxAttempts,
        Instant.now().plus(ttl));
    challengeSupport.setJson(authRedisKeys.passwordResetChallengeKey(emailNormalized), challenge, ttl);
  }

  public VerificationResult inspect(String emailNormalized, String code) {
    return validate(emailNormalized, code, false);
  }

  public VerificationResult consume(String emailNormalized, String code) {
    return validate(emailNormalized, code, true);
  }

  public boolean tryActivateRequestCooldown(String emailNormalized) {
    return cooldownSupport.tryActivate(authRedisKeys.passwordResetCooldownKey(emailNormalized),
        requestCooldown);
  }

  public void clearRequestCooldown(String emailNormalized) {
    cooldownSupport.clear(authRedisKeys.passwordResetCooldownKey(emailNormalized));
  }

  public void delete(String emailNormalized) {
    challengeSupport.delete(authRedisKeys.passwordResetChallengeKey(emailNormalized));
  }

  private VerificationResult validate(String emailNormalized, String code, boolean deleteOnSuccess) {
    String challengeKey = authRedisKeys.passwordResetChallengeKey(emailNormalized);
    LoadResult<PasswordResetChallenge> loaded = challengeSupport.loadActive(
        challengeKey, PasswordResetChallenge.class);

    if (loaded.status() == LoadStatus.EXPIRED_OR_MISSING) {
      return VerificationResult.EXPIRED_OR_MISSING;
    }
    if (loaded.status() == LoadStatus.TOO_MANY_ATTEMPTS) {
      return VerificationResult.TOO_MANY_ATTEMPTS;
    }

    PasswordResetChallenge challenge = loaded.challenge();
    if (!passwordEncoder.matches(code, challenge.codeHash())) {
      InvalidAttemptOutcome outcome = challengeSupport.recordInvalidAttempt(challengeKey, challenge);
      return VerificationResultMapper.fromInvalidAttemptOutcome(outcome);
    }
    if (deleteOnSuccess) {
      challengeSupport.delete(challengeKey);
    }
    return VerificationResult.SUCCESS;
  }
}
