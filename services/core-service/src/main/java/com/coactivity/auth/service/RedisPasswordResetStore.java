package com.coactivity.auth.service;

import com.coactivity.auth.model.PasswordResetChallenge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RedisPasswordResetStore {

  public enum VerificationResult {
    SUCCESS,
    INVALID_CODE,
    EXPIRED_OR_MISSING,
    TOO_MANY_ATTEMPTS
  }

  private static final String KEY_PREFIX = "auth:password-reset:";
  private static final String COOLDOWN_KEY_PREFIX = KEY_PREFIX + "cooldown:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;
  private final Duration ttl;
  private final int maxAttempts;
  private final Duration requestCooldown;

  public RedisPasswordResetStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
      PasswordEncoder passwordEncoder,
      @Value("${app.auth.password-reset.ttl:10m}") Duration ttl,
      @Value("${app.auth.password-reset.max-attempts:5}") int maxAttempts,
      @Value("${app.auth.password-reset.request-cooldown:60s}") Duration requestCooldown) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.passwordEncoder = passwordEncoder;
    this.ttl = ttl;
    this.maxAttempts = maxAttempts;
    this.requestCooldown = requestCooldown;
  }

  public void create(String emailNormalized, Integer userId, String rawCode) {
    PasswordResetChallenge challenge = new PasswordResetChallenge(
        userId,
        emailNormalized,
        passwordEncoder.encode(rawCode),
        0,
        maxAttempts,
        Instant.now().plus(ttl));
    write(challenge, ttl);
  }

  public VerificationResult inspect(String emailNormalized, String code) {
    return validate(emailNormalized, code, false);
  }

  public VerificationResult consume(String emailNormalized, String code) {
    return validate(emailNormalized, code, true);
  }

  public boolean tryActivateRequestCooldown(String emailNormalized) {
    Boolean activated = redisTemplate.opsForValue()
        .setIfAbsent(cooldownKey(emailNormalized), "1", requestCooldown);
    return Boolean.TRUE.equals(activated);
  }

  public void clearRequestCooldown(String emailNormalized) {
    redisTemplate.delete(cooldownKey(emailNormalized));
  }

  public void delete(String emailNormalized) {
    redisTemplate.delete(key(emailNormalized));
  }

  private VerificationResult validate(String emailNormalized, String code, boolean deleteOnSuccess) {
    Optional<PasswordResetChallenge> optionalChallenge = read(emailNormalized);
    if (optionalChallenge.isEmpty()) {
      return VerificationResult.EXPIRED_OR_MISSING;
    }

    PasswordResetChallenge challenge = optionalChallenge.get();
    if (challenge.expiresAt().isBefore(Instant.now())) {
      delete(emailNormalized);
      return VerificationResult.EXPIRED_OR_MISSING;
    }
    if (challenge.attempts() >= challenge.maxAttempts()) {
      delete(emailNormalized);
      return VerificationResult.TOO_MANY_ATTEMPTS;
    }
    if (!passwordEncoder.matches(code, challenge.codeHash())) {
      int nextAttempts = challenge.attempts() + 1;
      if (nextAttempts >= challenge.maxAttempts()) {
        delete(emailNormalized);
        return VerificationResult.TOO_MANY_ATTEMPTS;
      }
      Duration remainingTtl = Duration.between(Instant.now(), challenge.expiresAt());
      write(challenge.withAttempts(nextAttempts), remainingTtl);
      return VerificationResult.INVALID_CODE;
    }
    if (deleteOnSuccess) {
      delete(emailNormalized);
    }
    return VerificationResult.SUCCESS;
  }

  private Optional<PasswordResetChallenge> read(String emailNormalized) {
    String rawValue = redisTemplate.opsForValue().get(key(emailNormalized));
    if (rawValue == null || rawValue.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(rawValue, PasswordResetChallenge.class));
    } catch (JsonProcessingException ex) {
      delete(emailNormalized);
      return Optional.empty();
    }
  }

  private void write(PasswordResetChallenge challenge, Duration requestedTtl) {
    Duration effectiveTtl = requestedTtl == null || requestedTtl.isNegative() || requestedTtl.isZero()
        ? ttl
        : requestedTtl;
    try {
      redisTemplate.opsForValue().set(
          key(challenge.emailNormalized()),
          objectMapper.writeValueAsString(challenge),
          effectiveTtl);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to serialize password reset challenge", ex);
    }
  }

  private String key(String emailNormalized) {
    return KEY_PREFIX + emailNormalized;
  }

  private String cooldownKey(String emailNormalized) {
    return COOLDOWN_KEY_PREFIX + emailNormalized;
  }
}
