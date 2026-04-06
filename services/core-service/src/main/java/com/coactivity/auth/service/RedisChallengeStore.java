package com.coactivity.auth.service;

import com.coactivity.auth.model.RegistrationChallenge;
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
public class RedisChallengeStore {

  public enum VerificationResult {
    SUCCESS,
    INVALID_CODE,
    EXPIRED_OR_MISSING,
    TOO_MANY_ATTEMPTS
  }

  private static final String KEY_PREFIX = "auth:register:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;
  private final Duration ttl;
  private final int maxAttempts;

  public RedisChallengeStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
      PasswordEncoder passwordEncoder,
      @Value("${app.auth.challenge.ttl:10m}") Duration ttl,
      @Value("${app.auth.challenge.max-attempts:5}") int maxAttempts) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.passwordEncoder = passwordEncoder;
    this.ttl = ttl;
    this.maxAttempts = maxAttempts;
  }

  public void create(String emailNormalized, Integer userId, String rawCode) {
    RegistrationChallenge challenge = new RegistrationChallenge(
        userId,
        emailNormalized,
        passwordEncoder.encode(rawCode),
        0,
        maxAttempts,
        Instant.now().plus(ttl));
    write(challenge, ttl);
  }

  public VerificationResult verify(String emailNormalized, String code) {
    Optional<RegistrationChallenge> optionalChallenge = read(emailNormalized);
    if (optionalChallenge.isEmpty()) {
      return VerificationResult.EXPIRED_OR_MISSING;
    }

    RegistrationChallenge challenge = optionalChallenge.get();
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

    delete(emailNormalized);
    return VerificationResult.SUCCESS;
  }

  private Optional<RegistrationChallenge> read(String emailNormalized) {
    String rawValue = redisTemplate.opsForValue().get(key(emailNormalized));
    if (rawValue == null || rawValue.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(rawValue, RegistrationChallenge.class));
    } catch (JsonProcessingException e) {
      delete(emailNormalized);
      return Optional.empty();
    }
  }

  private void write(RegistrationChallenge challenge, Duration requestedTtl) {
    Duration effectiveTtl = requestedTtl == null || requestedTtl.isNegative() || requestedTtl.isZero()
        ? ttl
        : requestedTtl;
    try {
      redisTemplate.opsForValue().set(
          key(challenge.emailNormalized()),
          objectMapper.writeValueAsString(challenge),
          effectiveTtl);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to serialize registration challenge", e);
    }
  }

  public void delete(String emailNormalized) {
    redisTemplate.delete(key(emailNormalized));
  }

  private String key(String emailNormalized) {
    return KEY_PREFIX + emailNormalized;
  }
}
