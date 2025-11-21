package com.coactivity.service.dto;

import java.time.Instant;

/**
 * Lightweight record representing a pending login verification state.
 */
public record PendingVerification(Integer userId, String code, Instant expiresAt) {
}

