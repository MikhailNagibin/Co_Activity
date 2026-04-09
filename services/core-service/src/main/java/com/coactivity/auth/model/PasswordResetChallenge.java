package com.coactivity.auth.model;

import java.time.Instant;

public record PasswordResetChallenge(
    Integer userId,
    String emailNormalized,
    String codeHash,
    int attempts,
    int maxAttempts,
    Instant expiresAt) {

  public PasswordResetChallenge withAttempts(int nextAttempts) {
    return new PasswordResetChallenge(userId, emailNormalized, codeHash, nextAttempts,
        maxAttempts, expiresAt);
  }
}
