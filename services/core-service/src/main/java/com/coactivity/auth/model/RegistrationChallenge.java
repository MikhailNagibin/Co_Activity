package com.coactivity.auth.model;

import java.time.Instant;

public record RegistrationChallenge(
    Integer userId,
    String emailNormalized,
    String codeHash,
    int attempts,
    int maxAttempts,
    Instant expiresAt) {

  public RegistrationChallenge withAttempts(int nextAttempts) {
    return new RegistrationChallenge(userId, emailNormalized, codeHash, nextAttempts, maxAttempts,
        expiresAt);
  }
}
