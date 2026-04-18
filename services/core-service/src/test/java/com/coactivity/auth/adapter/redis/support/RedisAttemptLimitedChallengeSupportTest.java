package com.coactivity.auth.adapter.redis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.auth.domain.PasswordResetChallenge;
import com.coactivity.auth.adapter.redis.contract.InvalidAttemptOutcome;
import com.coactivity.auth.adapter.redis.contract.LoadResult;
import com.coactivity.auth.adapter.redis.contract.LoadStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisAttemptLimitedChallengeSupportTest {

  @Test
  void loadActive_expiredChallenge_deletesAndReturnsExpired() throws Exception {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> ops = (ValueOperations<String, String>) mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(ops);

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    PasswordResetChallenge expired = new PasswordResetChallenge(
        "hash",
        0,
        5,
        now.minusSeconds(1));
    when(ops.get("k")).thenReturn(objectMapper.writeValueAsString(expired));

    RedisAttemptLimitedChallengeSupport support = new RedisAttemptLimitedChallengeSupport(
        redisTemplate, objectMapper, clock);

    LoadResult<PasswordResetChallenge> result = support.loadActive("k", PasswordResetChallenge.class);

    assertEquals(LoadStatus.EXPIRED_OR_MISSING, result.status());
    verify(redisTemplate).delete("k");
  }

  @Test
  void recordInvalidAttempt_writesWithRemainingTtlAndIncrementsAttempts() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> ops = (ValueOperations<String, String>) mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(ops);
    doNothing().when(ops).set(eq("k"), anyString(), any(Duration.class));

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    RedisAttemptLimitedChallengeSupport support = new RedisAttemptLimitedChallengeSupport(
        redisTemplate, objectMapper, clock);

    PasswordResetChallenge challenge = new PasswordResetChallenge(
        "hash",
        0,
        5,
        now.plus(Duration.ofMinutes(10)));

    InvalidAttemptOutcome outcome = support.recordInvalidAttempt("k", challenge);
    assertEquals(InvalidAttemptOutcome.INVALID_CODE, outcome);

    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(ops).set(eq("k"), jsonCaptor.capture(), ttlCaptor.capture());
    assertEquals(Duration.ofMinutes(10), ttlCaptor.getValue());
    assertTrue(jsonCaptor.getValue().contains("\"attempts\":1"));

    verify(redisTemplate, never()).delete(anyString());
  }
}
